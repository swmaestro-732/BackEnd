package com.example.backend.mobile.home.application.port.outbound.dto

/** BFF 격리 DTO — 사용자의 관심 테마(코스 카테고리 이름)와 관심 지역 코드. 비로그인은 [NONE]. */
data class HomeInterest(
    val likeThemes: List<String>,
    val areaCodes: List<String>,
) {
    companion object {
        val NONE = HomeInterest(likeThemes = emptyList(), areaCodes = emptyList())
    }
}
