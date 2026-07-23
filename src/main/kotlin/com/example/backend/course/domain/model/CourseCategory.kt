package com.example.backend.course.domain.model

/**
 * 코스 카테고리. DB `courses.category` 에 이름 문자열로 저장된다(V3 enum 저장 컨벤션 — enumerationByName).
 * 카테고리 미선택 임시저장(draft) 코스가 있어 nullable.
 */
enum class CourseCategory {
    DATE,
    HEALING,
    FOOD,
    CAFETOUR,
    CULTURE,
    NATURE,
    NIGHTVIEW,
    SHOPPING,
    TRADITION,
    ACTIVITY,
    FAMILY,
    SOLO,
}
