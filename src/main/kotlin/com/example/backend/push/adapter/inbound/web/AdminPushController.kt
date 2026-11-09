package com.example.backend.push.adapter.inbound.web

import com.example.backend.common.response.ApiResponse
import com.example.backend.push.adapter.inbound.web.request.SendMarketingPushRequest
import com.example.backend.push.adapter.inbound.web.response.MarketingPushResponse
import com.example.backend.push.application.port.inbound.MarketingPushUseCase
import com.example.backend.push.application.port.inbound.dto.SendMarketingPushCommand
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터: 어드민 마케팅 푸시 발송.
 *
 * /admin/v1 경로라 purpose=admin 토큰이면서 지금도 admins 에 있는 계정만 통과한다(SecurityConfig).
 */
@RestController
@RequestMapping("/admin/v1/pushes")
class AdminPushController(
    private val marketingPushUseCase: MarketingPushUseCase,
) {
    /** 광고성 수신 동의 기기 전체에 발송한다. 야간(KST 21시~8시)에는 야간 수신 동의 기기에만 보낸다. */
    @PostMapping
    fun send(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: SendMarketingPushRequest,
    ): ApiResponse<MarketingPushResponse> {
        val command = SendMarketingPushCommand(checkNotNull(jwt.subject), request.title, request.body, request.link)
        return ApiResponse.success(MarketingPushResponse.from(marketingPushUseCase.send(command)))
    }
}
