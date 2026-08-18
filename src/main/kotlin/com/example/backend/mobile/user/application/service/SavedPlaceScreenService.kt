package com.example.backend.mobile.user.application.service

import com.example.backend.mobile.place.application.port.outbound.ScreenPlacePort
import com.example.backend.mobile.user.application.port.inbound.SavedPlaceScreenCommand
import com.example.backend.mobile.user.application.port.inbound.SavedPlaceScreenUseCase
import com.example.backend.mobile.user.application.port.inbound.dto.SavedPlaceScreenResult
import com.example.backend.mobile.user.application.port.outbound.SavedPlaceRecordPort
import com.example.backend.mobile.user.application.port.outbound.ScreenAreaPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 저장함 장소 탭 화면 조합 서비스 (BFF). 도메인을 직접 알지 않고 **아웃바운드 포트만** 호출해 한 화면 응답 재료를
 * 만든다 — 저장 레코드·배지 카운트([SavedPlaceRecordPort]) → 장소 요약([ScreenPlacePort]) → 지역 이름([ScreenAreaPort]).
 * 실제 도메인 호출은 각 ACL 어댑터가 맡으므로, MSA 분리 시 어댑터만 HTTP 클라이언트로 교체하면 이 조합 코드는 그대로다.
 *
 * 장소는 [ScreenPlacePort.findByIds] 로 페이지 전체를 배치 조회한다(항목별 N+1 회피) — 삭제된 장소는 결과에서
 * 빠지므로 해당 항목을 목록에서 제외한다(저장함 코스 탭이 삭제·비공개 코스를 빼는 것과 같은 취급).
 * 지역 이름은 페이지에 등장하는 area_code 만 중복 없이 조회한다.
 * 조합 한 번을 하나의 읽기 트랜잭션으로 묶어 일관된 스냅샷을 본다.
 *
 * 커서·카운트는 저장 레코드 조회 결과를 그대로 잇는다 — 항목이 장소 해석 실패로 빠져도 페이지 메타는
 * 저장 레코드 기준이다(다음 페이지 커서가 어긋나지 않게).
 */
@Service
@Transactional(readOnly = true)
class SavedPlaceScreenService(
    private val savedPlaceRecordPort: SavedPlaceRecordPort,
    private val screenPlacePort: ScreenPlacePort,
    private val screenAreaPort: ScreenAreaPort,
) : SavedPlaceScreenUseCase {
    override fun getScreen(command: SavedPlaceScreenCommand): SavedPlaceScreenResult {
        val page =
            savedPlaceRecordPort.findPage(
                userId = command.userId,
                visited = command.visited,
                category = command.category,
                cursor = command.cursor,
                size = command.size,
            )

        val placeById =
            screenPlacePort
                .findByIds(page.records.map { it.placeId })
                .associateBy { it.id }

        // 지역 이름은 코드 종류만큼만 조회한다(같은 동에 여러 장소가 있어도 1회).
        val areaNameByCode =
            placeById.values
                .mapNotNull { it.areaCode }
                .distinct()
                .mapNotNull { code -> screenAreaPort.findAreaName(code)?.let { code to it } }
                .toMap()

        val items =
            page.records.mapNotNull { record ->
                // 장소를 해석하지 못한(삭제 등) 저장 항목은 화면 목록에서 제외한다.
                val place = placeById[record.placeId] ?: return@mapNotNull null
                SavedPlaceScreenResult.Item(
                    id = record.id,
                    placeId = record.placeId,
                    category = record.category,
                    visited = record.visited,
                    savedAt = record.savedAt,
                    place =
                        SavedPlaceScreenResult.Place(
                            name = place.name,
                            category = place.category,
                            area = place.areaCode?.let { areaNameByCode[it] },
                            imageUrl = place.imageUrl,
                            latitude = place.latitude,
                            longitude = place.longitude,
                        ),
                )
            }

        return SavedPlaceScreenResult(
            totalCount = page.totalCount,
            unvisitedCount = page.unvisitedCount,
            visitedCount = page.visitedCount,
            categoryCounts =
                page.categoryCounts.map {
                    SavedPlaceScreenResult.CategoryCount(category = it.category, count = it.count)
                },
            nextCursor = page.nextCursor,
            hasNext = page.hasNext,
            items = items,
        )
    }
}
