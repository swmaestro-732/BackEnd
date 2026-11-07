package com.example.backend.place.application.port.outbound

import com.example.backend.place.domain.model.Place

/**
 * 아웃바운드 포트 — 장소를 검색 인덱스(OpenSearch)에 색인한다.
 * 검색은 부가 기능이라 색인 실패·미연결은 쓰기 흐름을 막지 않는다(구현체가 fail-soft·no-op 처리).
 */
interface PlaceSearchIndexPort {
    /** 장소들을 검색 인덱스에 저장한다(docId=place.id, upsert). id 가 없는(미영속) 장소는 건너뛴다. */
    fun save(places: List<Place>)
}
