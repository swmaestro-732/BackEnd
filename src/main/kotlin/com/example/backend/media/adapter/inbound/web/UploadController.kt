package com.example.backend.media.adapter.inbound.web

import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.media.adapter.inbound.web.request.PresignRequest
import com.example.backend.media.adapter.inbound.web.response.PresignResponse
import com.example.backend.media.application.port.inbound.PresignUploadUseCase
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 인바운드 어댑터 — 이미지 업로드용 S3 프리사인 URL 발급. */
@RestController
@RequestMapping("/api/v1/uploads")
class UploadController(
    private val presignUploadUseCase: PresignUploadUseCase,
) {
    /** 클라이언트는 발급받은 uploadUrl 로 동일 Content-Type 헤더를 실어 PUT 한 뒤, imageUrl 을 프로필 수정 등에 사용한다. */
    @PostMapping("/presign")
    fun presign(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: PresignRequest,
    ): ApiResponse<PresignResponse> =
        ApiResponse.success(PresignResponse.from(presignUploadUseCase.presign(request.toCommand(userId))))
}
