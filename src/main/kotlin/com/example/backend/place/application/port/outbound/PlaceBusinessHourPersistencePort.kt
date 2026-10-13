package com.example.backend.place.application.port.outbound

import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceBusinessHour

/** 장소 영업시간 영속 아웃바운드 포트 — 동기화 대상 조회와 요일별 행 교체를 담당한다. */
interface PlaceBusinessHourPersistencePort {
    /** 주어진 id 중 아직 영업시간을 동기화하지 않은 활성 장소. */
    fun findUnsynced(placeIds: List<Long>): List<Place>

    /** 장소의 요일별 영업시간 행을 전부 교체하고 동기화 시각을 기록한다(빈 목록이면 행 없이 시각만). */
    fun replaceBusinessHours(
        placeId: Long,
        hours: List<PlaceBusinessHour>,
    )
}
