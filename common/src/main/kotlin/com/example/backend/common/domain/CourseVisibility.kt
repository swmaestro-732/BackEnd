package com.example.backend.common.domain

/**
 * 코스 공개 범위. enum 이름이 DB 저장 계약 — 상수 이름 변경 금지(값 추가는 무방).
 *
 * 여러 도메인(course 가 소유·발행, user 가 공개범위별 개수 카운트)이 공유하는 값이라
 * shared kernel 인 common 에 둔다(멀티모듈 분리 시 core 로 승격).
 */
enum class CourseVisibility {
    PUBLIC,
    FOLLOWER,
    PRIVATE,
}
