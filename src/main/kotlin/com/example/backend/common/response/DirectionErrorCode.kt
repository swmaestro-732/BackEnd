package com.example.backend.common.response

enum class DirectionErrorCode(
    override val status: Int,
    override val code: Int,
    override val message: String,
) : ErrorCode {
    DIRECTION_UNAVAILABLE(503, 5030, "도보 경로 서비스를 일시적으로 이용할 수 없습니다."),

    // 5031(장소 검색)·5032(소셜 로그인)이 선점 — 5033 으로 채번했다.
    // 따라가기 경로 전용: 5030(재시도) 과 달리 클라이언트가 외부 지도(네이버) 안내로 넘기는 신호다.
    WALKING_ROUTE_UNAVAILABLE(503, 5033, "도보 경로를 불러오지 못했습니다. 외부 지도로 안내합니다."),
}
