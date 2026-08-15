package com.example.backend.place.application.service

import com.example.backend.place.application.port.inbound.PlaceReindexUseCase
import com.example.backend.place.application.port.outbound.PlacePersistencePort
import com.example.backend.place.application.port.outbound.PlaceSearchIndexPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PlaceReindexService(
    private val placePersistencePort: PlacePersistencePort,
    private val placeSearchIndexPort: PlaceSearchIndexPort,
) : PlaceReindexUseCase {
    /** 전체 장소를 페이지 단위로 재색인(멱등 upsert). 색인 자체는 어댑터가 fail-soft. */
    override fun reindexAll(): Int {
        var afterId: Long? = null
        var total = 0
        while (true) {
            val batch = placePersistencePort.findForIndex(afterId, PAGE)
            if (batch.isEmpty()) break
            placeSearchIndexPort.index(batch)
            total += batch.size
            afterId = batch.last().id
            if (batch.size < PAGE) break
        }
        return total
    }

    private companion object {
        const val PAGE = 500
    }
}
