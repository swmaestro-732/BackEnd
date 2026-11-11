package com.example.backend.course.domain.model

/** 따라가기 장소 상태 — 루트에 담겨 아직 방문 전(PLANNED)이거나 체크인함(VISITED). enum 이름이 DB 저장 계약 — 상수 이름 변경 금지. */
enum class CourseTracePlaceStatus {
    PLANNED,
    VISITED,
}
