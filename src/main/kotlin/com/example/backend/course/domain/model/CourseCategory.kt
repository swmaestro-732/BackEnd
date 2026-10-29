package com.example.backend.course.domain.model

/**
 * 코스 카테고리. 작성자가 코스마다 여러 개 고르며, DB `course_categories.category` 에 이름 문자열로 저장된다
 * (V3 enum 저장 컨벤션 — enumerationByName).
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
