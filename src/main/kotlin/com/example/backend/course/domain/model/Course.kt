package com.example.backend.course.domain.model

/**
 * Course 도메인 스켈레톤 — 상세 유스케이스/어댑터는 후속(SCRUM). member 도메인을 템플릿으로 확장.
 */
@ConsistentCopyVisibility // copy() 도 private 으로 — 팩토리 우회 차단
data class Course private constructor(
    val id: Int?,
    val title: String,
) {
    companion object {
        /** 신규 생성 — 도메인 불변식을 검증한다. */
        fun create(title: String): Course {
            require(title.isNotBlank()) { "제목은 비어 있을 수 없습니다." }
            return Course(id = null, title = title)
        }
    }
}
