package com.example.backend.place.application.port.outbound

import com.example.backend.place.domain.model.Place

/** 장소 영속 아웃바운드 포트 — 외부 검색 결과를 kakao place id 로 dedup 저장/조회한다. */
interface PlacePersistencePort {
    fun findByKakaoIds(kakaoIds: List<String>): List<Place>

    /**
     * 신규 장소들을 삽입하되 kakao place id 유니크 충돌은 무시한다(동시 검색 경합 대비 — insert ignore).
     * 삽입 결과 id 는 반환하지 않는다. 호출부는 [findByKakaoIds] 로 (기존+방금 삽입+동시 삽입) 전부를 재조회해 확정한다.
     */
    fun insertIgnoringConflicts(places: List<Place>)
}
