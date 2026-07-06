package com.example.backend.member.application.dto

import com.example.backend.member.domain.model.Member

/** 유스케이스 출력 — 도메인 모델을 밖으로 노출하지 않기 위한 애플리케이션 경계 타입. */
data class MemberResult(
    val id: Int,
    val name: String,
    val area: String,
) {
    companion object {
        fun from(member: Member): MemberResult =
            MemberResult(
                id = checkNotNull(member.id) { "영속화된 Member 는 id 를 가진다." },
                name = member.name,
                area = member.area.value,
            )
    }
}
