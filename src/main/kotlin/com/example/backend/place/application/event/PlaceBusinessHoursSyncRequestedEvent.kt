package com.example.backend.place.application.event

/** 장소 영업시간 동기화 요청 — 상세 조회 등 요청 경로가 발행하고, 커밋 후 비동기 리스너가 처리한다. */
data class PlaceBusinessHoursSyncRequestedEvent(
    val placeIds: List<Long>,
)
