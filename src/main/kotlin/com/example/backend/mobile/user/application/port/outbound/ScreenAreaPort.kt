package com.example.backend.mobile.user.application.port.outbound

/**
 * BFF 아웃바운드 포트 — 법정동코드를 화면 표시용 지역 이름으로 해석한다.
 * 지금은 area 도메인 인바운드 포트에 위임하는 어댑터가 구현하지만,
 * MSA 분리 후엔 같은 계약을 HTTP 클라이언트 어댑터로 바꿔 끼우면 BFF 조합 코드는 그대로다.
 *
 * 지금은 저장함 장소 탭만 쓰므로 이 슬라이스(mobile.user)가 소유한다 — 다른 화면이 필요해지면 옮긴다.
 */
interface ScreenAreaPort {
    /** 법정동코드(10자리)의 짧은 표시 이름(읍면동 등). 미존재·비활성이면 null. */
    fun findAreaName(code: String): String?
}
