package com.example.backend.place.domain.model

/**
 * Place 도메인 스켈레톤 — 상세 유스케이스/어댑터는 후속(SCRUM). user 도메인을 템플릿으로 확장.
 */
@ConsistentCopyVisibility // copy() 도 private 으로 — 팩토리 우회 차단
data class Place private constructor(
    val id: Int?,
    val name: String,
) {
    companion object {
        /** 신규 생성 — 도메인 불변식을 검증한다. */
        fun create(name: String): Place {
            require(name.isNotBlank()) { "이름은 비어 있을 수 없습니다." }
            return Place(id = null, name = name)
        }
    }
}
