package com.example.backend.mobile.user.application.port.inbound

import com.example.backend.mobile.user.application.port.inbound.dto.SavedCourseScreenResult

/**
 * 인바운드 포트 — 저장함 코스 탭 화면 조합 (BFF).
 * 저장 레코드(user)·코스 요약(course)·작성자 프로필(user)·장소 좌표(place)·완주 여부(trace)를
 * 조합해 한 화면 계약을 만든다. 저장·완주·폴더는 조회 주체(로그인 사용자) 소유 데이터라 userId 가 필수다.
 */
interface SavedCourseScreenUseCase {
    fun getScreen(command: SavedCourseScreenCommand): SavedCourseScreenResult
}

/**
 * 저장함 코스 탭 화면 조회 명령.
 * - userId: 조회 주체(JWT subject).
 * - folderId: 폴더 칩 필터(null 이면 전체).
 * - completed: 완주 상태 필터(null=전체, true=완주, false=안 가봄).
 * - cursor: 직전 응답의 nextCursor(첫 페이지는 null).
 * - size: 페이지 크기(1~50, 웹 어댑터에서 검증).
 */
data class SavedCourseScreenCommand(
    val userId: Long,
    val folderId: Long?,
    val completed: Boolean?,
    val cursor: String?,
    val size: Int,
)
