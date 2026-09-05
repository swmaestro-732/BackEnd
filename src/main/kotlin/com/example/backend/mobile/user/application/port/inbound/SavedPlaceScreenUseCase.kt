package com.example.backend.mobile.user.application.port.inbound

import com.example.backend.mobile.user.application.port.inbound.dto.SavedPlaceScreenResult

/**
 * 인바운드 포트 — 저장함 장소 탭 화면 조합 (BFF).
 */
interface SavedPlaceScreenUseCase {
    fun getScreen(command: SavedPlaceScreenCommand): SavedPlaceScreenResult
}

data class SavedPlaceScreenCommand(
    val userId: Long,
    val visited: Boolean,
    val category: String?,
    val cursor: String?,
    val size: Int,
)
