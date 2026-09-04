package com.example.backend.mobile.user.application.port.inbound

import com.example.backend.mobile.user.application.port.inbound.dto.SavedCourseScreenResult

/**
 * 인바운드 포트 — 저장함 코스 탭 화면 조합 (BFF).
 */
interface SavedCourseScreenUseCase {
    fun 저장함코스화면조회(command: SavedCourseScreenCommand): SavedCourseScreenResult
}

data class SavedCourseScreenCommand(
    val userId: Long,
    val folderId: Long?,
    val completed: Boolean?,
    val cursor: String?,
    val size: Int,
)
