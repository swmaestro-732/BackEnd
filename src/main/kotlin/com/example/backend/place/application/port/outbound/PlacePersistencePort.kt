package com.example.backend.place.application.port.outbound

import com.example.backend.place.domain.model.Place

/** 장소 영속 아웃바운드 포트 — 외부 검색 결과를 kakao place id 로 dedup 저장/조회한다. */
interface PlacePersistencePort {
    fun findByKakaoIds(kakaoIds: List<String>): List<Place>

    fun saveAll(places: List<Place>): List<Place>
}
