package com.example.backend.member.adapter.inbound.web.response

import com.example.backend.member.application.dto.MemberResult

/** 웹 응답 DTO. 유스케이스 결과([MemberResult])를 직렬화 형태로 변환한다. */
data class MemberResponse(
    val id: Long,
    val nickname: String,
) {
    companion object {
        fun from(result: MemberResult): MemberResponse = MemberResponse(id = result.id, nickname = result.nickname)
    }
}
