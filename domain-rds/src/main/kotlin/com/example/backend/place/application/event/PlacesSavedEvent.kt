package com.example.backend.place.application.event

import com.example.backend.place.domain.model.Place

/** 도메인 이벤트 — 검색 인덱스 동기화 등 커밋 후 후처리에 쓴다. */
data class PlacesSavedEvent(
    val places: List<Place>,
)
