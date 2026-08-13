package com.example.backend.mobile.place.application.port.outbound.dto

/**
 * BFF 아웃바운드 출력 — 장소 상세 화면 후보. place 도메인 응답([com.example.backend.place.application.port.inbound.dto.PlaceSummary])을
 * BFF 안으로 복사한 격리 DTO다(크로스 도메인·BFF 격리).
 */
data class ScreenPlace(
    val id: Long,
    val name: String,
    val category: String,
    val imageUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    // 법정동코드 10자리(미확인 시 null) — 화면 표시용 지역 이름 해석 입력(ScreenAreaPort)
    val areaCode: String?,
)
