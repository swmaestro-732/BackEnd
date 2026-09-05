package com.example.backend.mobile.user.application.service

import com.example.backend.mobile.place.application.port.outbound.ScreenPlacePort
import com.example.backend.mobile.place.application.port.outbound.dto.ScreenPlace
import com.example.backend.mobile.user.application.port.inbound.SavedPlaceScreenCommand
import com.example.backend.mobile.user.application.port.inbound.SavedPlaceScreenUseCase
import com.example.backend.mobile.user.application.port.inbound.dto.SavedPlaceScreenResult
import com.example.backend.mobile.user.application.port.outbound.SavedPlaceRecordPort
import com.example.backend.mobile.user.application.port.outbound.ScreenAreaPort
import com.example.backend.mobile.user.application.port.outbound.dto.SavedPlaceRecord
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 저장함 장소 탭 화면 조합 서비스 (BFF).
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
            items = assembleItems(page.records),
        )
    }

    private fun assembleItems(records: List<SavedPlaceRecord>): List<SavedPlaceScreenResult.Item> {
        val placeById = screenPlacePort.findByIds(records.map { it.placeId }).associateBy { it.id }
        val areaNameByCode = resolveAreaNames(placeById.values)

        return records.mapNotNull { record ->
            val place = placeById[record.placeId] ?: return@mapNotNull null
            SavedPlaceScreenResult.Item(
                id = record.id,
                placeId = record.placeId,
                category = record.category,
                visited = record.visited,
                savedAt = record.savedAt,
                place = place.toResultPlace(areaNameByCode),
            )
        }
    }

    private fun resolveAreaNames(places: Collection<ScreenPlace>): Map<String, String> =
        places
            .mapNotNull { it.areaCode }
            .distinct()
            .mapNotNull { code -> screenAreaPort.findAreaName(code)?.let { code to it } }
            .toMap()

    private fun ScreenPlace.toResultPlace(areaNameByCode: Map<String, String>) =
        SavedPlaceScreenResult.Place(
            name = name,
            category = category,
            area = areaCode?.let { areaNameByCode[it] },
            imageUrl = imageUrl,
            latitude = latitude,
            longitude = longitude,
        )
}
