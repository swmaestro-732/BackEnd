package com.example.backend.user.application.service

import com.example.backend.user.application.port.inbound.SavedPlaceReindexUseCase
import com.example.backend.user.application.port.outbound.PlaceAccessPort
import com.example.backend.user.application.port.outbound.SavedPlaceIndexDocument
import com.example.backend.user.application.port.outbound.SavedPlacePersistencePort
import com.example.backend.user.application.port.outbound.SavedPlaceSearchIndexPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SavedPlaceReindexService(
    private val savedPlacePersistencePort: SavedPlacePersistencePort,
    private val placeAccessPort: PlaceAccessPort,
    private val savedPlaceSearchIndexPort: SavedPlaceSearchIndexPort,
) : SavedPlaceReindexUseCase {
    override fun reindexAll(): Int {
        var afterId: Long? = null
        var total = 0
        while (true) {
            val batch = savedPlacePersistencePort.findForIndex(afterId, PAGE)
            if (batch.isEmpty()) break
            val places = placeAccessPort.findPlaces(batch.map { it.placeId }.distinct()).associateBy { it.id }
            val documents =
                batch.mapNotNull { saved ->
                    places[saved.placeId]?.let { place ->
                        SavedPlaceIndexDocument(
                            saved.userId,
                            saved.placeId,
                            place.location,
                            saved.visited,
                            saved.savedAt,
                        )
                    }
                }
            if (documents.isNotEmpty()) savedPlaceSearchIndexPort.index(documents)
            total += documents.size
            // 삭제된 장소만 담긴 배치여도 DB 커서는 반드시 전진한다.
            afterId = batch.last().id
            if (batch.size < PAGE) break
        }
        return total
    }

    private companion object {
        const val PAGE = 100
    }
}
