package com.example.backend.member.domain.model

import com.example.backend.common.area.Area

/**
 * Member 애그리거트 루트.
 *
 * 프레임워크(Spring·Exposed)에 의존하지 않는 순수 도메인 모델이다.
 * 생성은 [create] 팩토리를 통해서만 하며, 이때 도메인 불변식을 검증한다.
 * [id] 가 null 이면 아직 영속화되지 않은 상태다.
 * [area] 는 common 의 공유 참조 타입([Area])이다.
 */
@ConsistentCopyVisibility // copy() 도 private 으로 — 팩토리 우회 차단
data class Member private constructor(
    val id: Int?,
    val name: String,
    val area: Area,
) {
    companion object {
        const val MAX_NAME_LENGTH = 100

        /** 신규 생성 — 도메인 불변식을 검증한다. */
        fun create(
            name: String,
            area: Area,
        ): Member {
            require(name.isNotBlank()) { "이름은 비어 있을 수 없습니다." }
            require(name.length <= MAX_NAME_LENGTH) { "이름은 최대 ${MAX_NAME_LENGTH}자입니다." }
            return Member(id = null, name = name, area = area)
        }

        /** 영속 저장소에서 복원(이미 검증된 데이터). */
        fun reconstitute(
            id: Int,
            name: String,
            area: Area,
        ): Member = Member(id = id, name = name, area = area)
    }
}
