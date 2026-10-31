package com.example.backend.place.application.port.outbound

import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceBusinessHour

/** 장소 영업시간 영속 아웃바운드 포트 — 동기화 대상 조회·잠금과 요일별 행 저장을 담당한다. */
interface PlaceBusinessHourPersistencePort {
    /** 주어진 id 중 아직 영업시간을 동기화하지 않은 활성 장소. */
    fun findUnsynced(placeIds: List<Long>): List<Place>

    /**
     * 미동기화 활성 장소 한 건을 행 잠금으로 잡아 반환한다. 이미 동기화됐거나 다른 트랜잭션이 잠근 중이면 기다리지 않고 null.
     * 잠금은 호출 트랜잭션이 끝날 때까지 유지된다.
     */
    fun lockUnsynced(placeId: Long): Place?

    /** 장소의 요일별 영업시간을 저장하고 동기화 시각을 기록한다(빈 목록이면 행 없이 시각만). */
    fun saveBusinessHours(
        placeId: Long,
        hours: List<PlaceBusinessHour>,
    )
}
