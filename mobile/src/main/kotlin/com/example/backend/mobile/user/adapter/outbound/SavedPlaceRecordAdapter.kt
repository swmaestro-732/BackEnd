package com.example.backend.mobile.user.adapter.outbound

import com.example.backend.mobile.user.application.port.outbound.SavedPlaceRecordPort
import com.example.backend.mobile.user.application.port.outbound.dto.SavedPlaceCategoryCount
import com.example.backend.mobile.user.application.port.outbound.dto.SavedPlaceRecord
import com.example.backend.mobile.user.application.port.outbound.dto.SavedPlaceRecordPage
import com.example.backend.user.application.port.inbound.SavedPlaceUseCase
import com.example.backend.user.application.port.inbound.dto.SavedPlacesCommand
import org.springframework.stereotype.Component

/**
 * BFF 아웃바운드 어댑터 — 저장 레코드 조회를 user 도메인 인바운드 포트에 위임하고 BFF 격리 DTO 로 매핑한다.
 * (지금은 인프로세스 위임. MSA 분리 시 이 어댑터만 user 서비스 HTTP 클라이언트로 교체한다.)
 */
@Component
class SavedPlaceRecordAdapter(
    private val savedPlaceUseCase: SavedPlaceUseCase,
) : SavedPlaceRecordPort {
    override fun findPage(
        userId: Long,
        visited: Boolean,
        category: String?,
        cursor: String?,
        size: Int,
    ): SavedPlaceRecordPage {
        val result =
            savedPlaceUseCase.getSavedPlaces(
                SavedPlacesCommand(
                    userId = userId,
                    visited = visited,
                    category = category,
                    cursor = cursor,
                    size = size,
                ),
            )
        return SavedPlaceRecordPage(
            totalCount = result.totalCount,
            unvisitedCount = result.unvisitedCount,
            visitedCount = result.visitedCount,
            categoryCounts =
                result.categoryCounts.map { SavedPlaceCategoryCount(category = it.category, count = it.count) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
            records =
                result.savedPlaces.map {
                    SavedPlaceRecord(
                        id = it.id,
                        placeId = it.placeId,
                        category = it.category,
                        visited = it.visited,
                        savedAt = it.savedAt,
                    )
                },
        )
    }
}
