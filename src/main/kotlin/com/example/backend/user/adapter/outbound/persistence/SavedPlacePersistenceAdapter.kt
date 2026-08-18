package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.SavedPlaceRepository
import com.example.backend.user.application.port.outbound.SavedPlaceCategoryCountRow
import com.example.backend.user.application.port.outbound.SavedPlacePersistencePort
import com.example.backend.user.application.port.outbound.SavedPlaceRow
import com.example.backend.user.domain.model.SavedPlace
import com.example.backend.user.domain.model.SavedPlaceCategory
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [SavedPlacePersistencePort] 를 구현한다.
 * 실제 테이블(saved_places) 접근은 [SavedPlaceRepository] 에 위임한다.
 */
@Component
class SavedPlacePersistenceAdapter(
    private val savedPlaceRepository: SavedPlaceRepository,
) : SavedPlacePersistencePort {
    override fun existsSavedPlace(
        userId: Long,
        placeId: Long,
    ): Boolean = savedPlaceRepository.existsSavedPlace(userId, placeId)

    override fun insert(
        userId: Long,
        placeId: Long,
        category: SavedPlaceCategory?,
    ): SavedPlace = savedPlaceRepository.insert(userId, placeId, category)

    override fun deleteByUserAndPlace(
        userId: Long,
        placeId: Long,
    ): Boolean = savedPlaceRepository.deleteByUserAndPlace(userId, placeId)

    override fun countByVisited(
        userId: Long,
        visited: Boolean,
    ): Long = savedPlaceRepository.countByVisited(userId, visited)

    override fun countByCategory(userId: Long): List<SavedPlaceCategoryCountRow> =
        savedPlaceRepository.countByCategory(userId)

    override fun findPage(
        userId: Long,
        visited: Boolean,
        category: SavedPlaceCategory?,
        cursorId: Long?,
        limit: Int,
    ): List<SavedPlaceRow> = savedPlaceRepository.findPage(userId, visited, category, cursorId, limit)
}
