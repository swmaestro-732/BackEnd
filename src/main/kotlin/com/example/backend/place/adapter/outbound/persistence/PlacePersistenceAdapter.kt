package com.example.backend.place.adapter.outbound.persistence

import com.example.backend.place.adapter.outbound.persistence.exposed.repository.PlaceRepository
import com.example.backend.place.application.port.outbound.PlacePersistencePort
import com.example.backend.place.domain.model.Place
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [PlacePersistencePort] 를 구현한다.
 * 실제 테이블 접근은 [PlaceRepository] 에 위임하고, 이 어댑터는 유스케이스 계약만 맞춘다.
 */
@Component
class PlacePersistenceAdapter(
    private val placeRepository: PlaceRepository,
) : PlacePersistencePort {
    override fun findByKakaoIds(kakaoIds: List<String>): List<Place> = placeRepository.findByKakaoIds(kakaoIds)

    override fun insertIgnoringConflicts(places: List<Place>) = placeRepository.insertIgnoringConflicts(places)
}
