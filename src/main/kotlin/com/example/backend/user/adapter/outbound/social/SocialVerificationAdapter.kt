package com.example.backend.user.adapter.outbound.social

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.user.application.port.outbound.SocialIdentity
import com.example.backend.user.application.port.outbound.SocialVerificationPort
import com.example.backend.user.domain.model.SocialProvider
import org.springframework.stereotype.Component

/** provider 로 provider별 검증기(SocialTokenVerifier)에 위임하는 디스패처. */
@Component
class SocialVerificationAdapter(
    verifiers: List<SocialTokenVerifier>,
) : SocialVerificationPort {
    private val byProvider: Map<SocialProvider, SocialTokenVerifier> =
        verifiers.associateBy { it.provider }

    override fun verify(
        provider: SocialProvider,
        idToken: String,
    ): SocialIdentity {
        val verifier =
            byProvider[provider]
                ?: throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)
        return verifier.verify(idToken)
    }
}
