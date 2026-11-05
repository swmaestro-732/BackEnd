package com.example.backend.common.response

enum class PushErrorCode(
    override val status: Int,
    override val code: Int,
    override val message: String,
) : ErrorCode {
    // 409 는 4099 까지 선점돼 채번 규칙(마지막 번호 + 1)대로 4100 을 쓴다. 410 상태 코드와는 무관하다.
    DUPLICATE_MARKETING_PUSH(409, 4100, "같은 내용의 광고 푸시를 10분 안에 이미 보냈습니다."),
}
