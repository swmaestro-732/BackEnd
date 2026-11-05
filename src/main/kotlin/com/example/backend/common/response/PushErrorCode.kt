package com.example.backend.common.response

enum class PushErrorCode(
    override val status: Int,
    override val code: Int,
    override val message: String,
) : ErrorCode {
    // 새 코드부터 도메인별 100번대 대역을 쓴다. push 는 4300번대(기존 상태코드 기준 번호는 그대로 둔다).
    DUPLICATE_MARKETING_PUSH(409, 4300, "같은 내용의 광고 푸시를 10분 안에 이미 보냈습니다."),
}
