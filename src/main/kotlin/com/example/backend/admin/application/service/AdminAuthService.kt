package com.example.backend.admin.application.service

import com.example.backend.admin.application.port.inbound.AdminAuthUseCase
import com.example.backend.admin.application.port.inbound.dto.AdminLogin
import com.example.backend.admin.application.port.outbound.AdminIdentityPort
import com.example.backend.admin.application.port.outbound.AdminPersistencePort
import com.example.backend.admin.application.port.outbound.AdminTokenPort
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 어드민 로그인 유스케이스 구현. admins 에 등록된 구글 계정만 어드민 토큰을 받는다. */
@Service
@Transactional(readOnly = true)
class AdminAuthService(
    private val adminIdentityPort: AdminIdentityPort,
    private val adminPersistencePort: AdminPersistencePort,
    private val adminTokenPort: AdminTokenPort,
) : AdminAuthUseCase {
    private val log = KotlinLogging.logger {}

    override fun login(idToken: String): AdminLogin {
        val email = adminIdentityPort.verifyGoogle(idToken).lowercase()
        if (!adminPersistencePort.exists(email)) {
            // 토큰은 남기지 않는다. 누가 시도했는지 추적할 수 있게 이메일만 남긴다.
            log.warn { "어드민이 아닌 계정의 어드민 로그인 시도: email=$email" }
            throw BusinessException(CommonErrorCode.ADMIN_FORBIDDEN)
        }
        val issued = adminTokenPort.issue(email)
        return AdminLogin(accessToken = issued.token, expiresAt = issued.expiresAt, email = email)
    }
}
