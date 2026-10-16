package com.example.backend.place.application.port.inbound

/**
 * 인바운드 포트 — 전체 장소를 검색 인덱스에 재색인한다(backfill·드리프트 복구).
 * 부팅 러너(bootstrap)가 인덱스가 비었거나 강제 재색인 설정일 때 호출한다.
 */
interface PlaceReindexUseCase {
    /** 전체 장소를 재색인하고 색인한 문서 수를 반환한다. */
    fun reindexAll(): Int
}
