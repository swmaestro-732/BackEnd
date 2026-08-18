package com.example.backend.mobile.user.application.port.inbound

import com.example.backend.mobile.user.application.port.inbound.dto.SavedPlaceScreenResult

/**
 * 인바운드 포트 — 저장함 장소 탭 화면 조합 (BFF).
 * 저장 레코드·배지 카운트(user)·장소 요약(place)·지역 이름(area)을 조합해 한 화면 계약을 만든다.
 * 저장은 조회 주체(로그인 사용자) 소유 데이터라 userId 가 필수다.
 */
interface SavedPlaceScreenUseCase {
    fun getScreen(command: SavedPlaceScreenCommand): SavedPlaceScreenResult
}

/**
 * 저장함 장소 탭 화면 조회 명령.
 * - userId: 조회 주체(JWT subject).
 * - visited: 방문 여부 필터(미방문/방문 탭). 생략 시 미방문(false) 기준 — 도메인 조회와 동일 계약.
 * - category: 저장 카테고리 칩 필터 이름(예: CAFE, null 이면 전체). BFF 는 user 도메인 enum 을 참조할 수 없어
 *   문자열로 받아 그대로 도메인 포트에 넘기고, 아는 이름이 아니면 도메인이 400 을 던진다.
 * - cursor: 직전 응답의 nextCursor(첫 페이지는 null).
 * - size: 페이지 크기(1~50, 웹 어댑터에서 검증).
 *
 * 사용자 위치(userLat·userLng)는 거리 표시·정렬용이라 이 명령에 없다 — 1차 구현은 거리를 다루지 않고
 * 최신 저장순만 지원한다(웹 어댑터가 계약상 파라미터만 받는다).
 */
data class SavedPlaceScreenCommand(
    val userId: Long,
    val visited: Boolean,
    val category: String?,
    val cursor: String?,
    val size: Int,
)
