package com.example.backend.place.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.common.persistence.postgis.GeoPoint
import com.example.backend.place.application.event.PlacesSavedEvent
import com.example.backend.place.application.port.inbound.PlaceSearchExternalUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSearchResult
import com.example.backend.place.application.port.outbound.AreaCodeLookupPort
import com.example.backend.place.application.port.outbound.ExternalPlaceSearchPort
import com.example.backend.place.application.port.outbound.PlacePersistencePort
import com.example.backend.place.domain.model.Place
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionOperations

/**
 * 외부 장소 검색 서비스 — 카카오 로컬 키워드 검색 후 결과를 내부 places 테이블에 kakao place id 로 dedup 저장한다.
 *
 * 매 호출마다 카카오를 새로(fresh) 조회하되, 이미 저장된 장소는 재삽입 없이 기존 row(내부 id)를 재사용한다.
 * 카카오는 near 좌표를 radius 로 네이티브 지원하고 최대 45개까지 조회 가능해 단일 제공자로 충분하다.
 *
 * 외부 HTTP 호출(키워드 검색·좌표→법정동 변환)이 DB 커넥션을 점유하지 않도록 메서드 전체 대신
 * DB 구간 두 곳(dedup 조회 / 삽입+확정 재조회)만 [TransactionOperations] 로 짧게 감싼다.
 * dedup 은 원래 낙관적 설계(사전 조회 → insert ignore → 재조회)라 두 트랜잭션으로 나눠도 안전하다.
 */
@Service
class PlaceSearchService(
    private val externalPlaceSearchPort: ExternalPlaceSearchPort,
    private val placePersistencePort: PlacePersistencePort,
    private val areaCodeLookupPort: AreaCodeLookupPort,
    private val transactionOperations: TransactionOperations,
    private val eventPublisher: ApplicationEventPublisher,
) : PlaceSearchExternalUseCase {
    override fun search(
        query: String,
        near: Coordinate?,
    ): List<PlaceSearchResult> {
        val external = externalPlaceSearchPort.search(query, near)
        if (external.isEmpty()) return emptyList()

        val allKakaoIds = external.map { it.kakaoPlaceId }

        // 이미 저장된 장소는 kakao id 로 찾아 재사용한다(kakaoPlaceId 는 카카오 유래 row 에선 non-null).
        val existingKakaoIds =
            transactionOperations
                .execute { _ ->
                    placePersistencePort
                        .findByKakaoIds(allKakaoIds)
                        .mapNotNull { it.kakaoPlaceId }
                        .toSet()
                }.orEmpty()

        // 저장되지 않은 것만 신규 도메인으로. 카카오가 페이지 간 중복 id 를 줄 수 있어 distinctBy 로 한 번 걸러낸다.
        // 도메인 불변식(이름·주소 비어 있으면 예외)에 걸리지 않도록 빈 값은 미리 제외한다.
        val toInsert =
            external
                .asSequence()
                .filter { it.kakaoPlaceId !in existingKakaoIds }
                .distinctBy { it.kakaoPlaceId }
                .filter { it.name.isNotBlank() }
                .mapNotNull { ext ->
                    val address = ext.roadAddress ?: ext.address ?: return@mapNotNull null
                    if (address.isBlank()) return@mapNotNull null
                    Place.create(
                        name = ext.name,
                        description = null,
                        category = ext.category,
                        location = GeoPoint(ext.coordinate.latitude, ext.coordinate.longitude),
                        address = address,
                        // 신규 저장 장소에만 좌표→법정동 변환을 호출한다(dedup 이후라 호출 수가 신규분으로 제한됨).
                        // 트랜잭션 밖 외부 HTTP 호출 — DB 커넥션을 잡은 채 대기하지 않는다. 실패 시 null.
                        areaCode = areaCodeLookupPort.findLegalDongCode(ext.coordinate),
                        imageUrl = null,
                        kakaoPlaceId = ext.kakaoPlaceId,
                    )
                }.toList()

        // 삽입 후 재조회로 확정한다 — 기존 + 방금 삽입분을 집는다.
        // (kakao_place_id 유니크 인덱스를 걸면, 동시 경합으로 내 insert 가 무시돼도 상대가 넣은 row 를 이 재조회가 집어 요청이 실패하지 않는다.)
        // 삽입+확정 재조회와 같은 트랜잭션 안에서 이벤트를 발행해 커밋에 바인딩한다 — AFTER_COMMIT 리스너가
        // 커밋 후 비동기로 색인한다(롤백 시 색인 안 함 → 고스트 문서 방지, fail-soft 는 어댑터가 담당).
        val placeByKakao =
            transactionOperations
                .execute { _ ->
                    if (toInsert.isNotEmpty()) placePersistencePort.insertIgnoringConflicts(toInsert)
                    val map =
                        placePersistencePort
                            .findByKakaoIds(allKakaoIds)
                            .mapNotNull { place -> place.kakaoPlaceId?.let { it to place } }
                            .toMap()
                    eventPublisher.publishEvent(PlacesSavedEvent(map.values.toList())) // 커밋 후 색인(AFTER_COMMIT 리스너)
                    map
                }.orEmpty()

        return external.mapNotNull { ext ->
            val place = placeByKakao[ext.kakaoPlaceId] ?: return@mapNotNull null
            PlaceSearchResult(
                id = place.id!!,
                name = ext.name,
                category = place.category.name,
                roadAddress = ext.roadAddress,
                address = ext.address,
                latitude = ext.coordinate.latitude,
                longitude = ext.coordinate.longitude,
                telephone = ext.telephone,
            )
        }
    }
}
