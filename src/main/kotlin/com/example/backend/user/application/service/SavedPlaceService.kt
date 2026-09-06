package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.user.application.port.inbound.SavedPlaceUseCase
import com.example.backend.user.application.port.inbound.dto.SavedPlacesCommand
import com.example.backend.user.application.port.inbound.dto.SavedPlacesResult
import com.example.backend.user.application.port.outbound.PlaceAccessPort
import com.example.backend.user.application.port.outbound.SavedPlacePersistencePort
import com.example.backend.user.domain.model.SavedPlace
import com.example.backend.user.domain.model.SavedPlaceCategory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 장소 저장 유스케이스 — 저장·저장 취소와 저장 장소 조회를 함께 담당한다.
 *
 * 저장(save): 저장할 장소가 실제로 존재하는지 검증하고(그 외 404),
 *   이미 저장한 장소면 중복 저장으로 막는다(409) — 장소당 저장 레코드는 1개다.
 *   place_id 는 cross-domain(FK 없음)이라 존재 검증은 아웃바운드 [PlaceAccessPort] 로 하고(코어는 place 를 직접 알지 않는다),
 *   이때 받은 장소 카테고리를 [SavedPlaceCategory] 스냅샷으로 복사해 저장한다(MSA 대비 비정규화).
 * 취소(unsave): (userId, placeId) 저장 레코드를 소프트 삭제한다 — 없어도 성공하는 멱등 연산(코스 저장 취소 선례).
 * 조회(getSavedPlaces): 저장 레코드를 최신 저장순(id 내림차순)으로 커서 페이지네이션해 반환한다.
 *
 * 기본은 읽기 전용 트랜잭션이고, 쓰기(save·unsave)만 메서드 레벨에서 재정의한다.
 */
@Service
@Transactional(readOnly = true)
class SavedPlaceService(
    private val savedPlacePersistencePort: SavedPlacePersistencePort,
    private val placeAccessPort: PlaceAccessPort,
) : SavedPlaceUseCase {
    @Transactional
    override fun save(
        userId: Long,
        placeId: Long,
    ): SavedPlace {
        val place =
            placeAccessPort.findPlace(placeId)
                ?: throw BusinessException(PlaceErrorCode.PLACE_NOT_FOUND, "저장할 장소를 찾을 수 없습니다: placeId=$placeId")

        if (savedPlacePersistencePort.existsSavedPlace(userId, placeId)) {
            throw BusinessException(PlaceErrorCode.PLACE_ALREADY_SAVED, "이미 저장한 장소입니다: placeId=$placeId")
        }

        // 장소 카테고리 스냅샷 — SavedPlaceCategory 는 PlaceCategory 의 사본 enum 이라 이름으로 매핑한다(모르는 값은 미분류 null).
        val category = SavedPlaceCategory.entries.find { it.name == place.category }
        // 예전에 저장했다 취소한 장소면 그때 소프트 삭제된 행을 지우고 새로 발행한다(행을 쌓지 않는다).
        return savedPlacePersistencePort.insert(userId, placeId, category)
    }

    @Transactional
    override fun unsave(
        userId: Long,
        placeId: Long,
    ) {
        savedPlacePersistencePort.deleteByUserAndPlace(userId, placeId)
    }

    override fun getSavedPlaces(command: SavedPlacesCommand): SavedPlacesResult {
        val cursorId = command.cursor?.let(::decodeCursor)
        val category = command.category?.let(::decodeCategory)

        // hasNext 판정을 위해 한 개 더 조회한 뒤, 페이지 크기만큼 잘라낸다.
        val rows =
            savedPlacePersistencePort.findPage(
                command.userId,
                command.visited,
                category,
                cursorId,
                command.size + 1,
            )
        val hasNext = rows.size > command.size
        val page = rows.take(command.size)

        // 탭·칩 배지는 조회 필터와 무관한 전체 기준 카운트다 — visited 를 false/true 로 고정해 각각 센다.
        val unvisitedCount = savedPlacePersistencePort.countByVisited(command.userId, visited = false)
        val visitedCount = savedPlacePersistencePort.countByVisited(command.userId, visited = true)

        return SavedPlacesResult(
            totalCount = unvisitedCount + visitedCount,
            unvisitedCount = unvisitedCount,
            visitedCount = visitedCount,
            categoryCounts =
                savedPlacePersistencePort
                    .countByCategory(command.userId)
                    .sortedByDescending { it.count }
                    .map { SavedPlacesResult.CategoryCount(category = it.category.name, count = it.count) },
            nextCursor = if (hasNext) page.lastOrNull()?.id?.toString() else null,
            hasNext = hasNext,
            savedPlaces =
                page.map {
                    SavedPlacesResult.SavedPlaceItem(
                        id = it.id,
                        placeId = it.placeId,
                        category = it.category?.name,
                        visited = it.visited,
                        savedAt = it.savedAt,
                    )
                },
        )
    }

    /** 커서(=저장 레코드 id)를 파싱한다. 형식이 잘못되면 400. */
    private fun decodeCursor(cursor: String): Long =
        cursor.toLongOrNull()
            ?: throw BusinessException(CommonErrorCode.INVALID_INPUT, "잘못된 커서입니다: $cursor")

    /**
     * 카테고리 필터 이름을 도메인 enum 으로 파싱한다. 아는 이름이 아니면 400 —
     * 포트 계약이 문자열이라(BFF·타 도메인이 enum 을 참조할 수 없다) 검증을 여기서 한다.
     */
    private fun decodeCategory(category: String): SavedPlaceCategory =
        SavedPlaceCategory.entries.find { it.name == category }
            ?: throw BusinessException(CommonErrorCode.INVALID_INPUT, "알 수 없는 저장 카테고리입니다: $category")
}
