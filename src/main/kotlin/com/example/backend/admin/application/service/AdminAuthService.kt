package com.example.backend.admin.application.service

import com.example.backend.admin.application.port.inbound.AdminAccessUseCase
import com.example.backend.admin.application.port.inbound.AdminAuthUseCase
import com.example.backend.admin.application.port.inbound.dto.AdminLogin
import com.example.backend.admin.application.port.outbound.AdminIdentityPort
import com.example.backend.admin.application.port.outbound.AdminPersistencePort
import com.example.backend.admin.application.port.outbound.AdminTokenPort
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.AdminErrorCode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 어드민 로그인과 요청별 어드민 확인 구현. admins 에 등록된 구글 계정만 어드민 토큰을 받고 쓸 수 있다. */
@Service
@Transactional(readOnly = true)
class AdminAuthService(
    private val adminIdentityPort: AdminIdentityPort,
    private val adminPersistencePort: AdminPersistencePort,
    private val adminTokenPort: AdminTokenPort,
) : AdminAuthUseCase,
    AdminAccessUseCase {
    private val log = KotlinLogging.logger {}

    /**
     * 이메일만으로 믿지 않고 구글 계정(sub)까지 맞춘다. 같은 이메일이 다른 구글 계정에 붙는 경우(회사 메일 재할당,
     * 도메인 만료 후 재등록)에 이전 주인이 어드민 토큰을 받지 못하게 하려는 것이다. 첫 로그인 때 그 계정에 묶는다.
     */
    @Transactional
    override fun login(idToken: String): AdminLogin {
        val account = adminIdentityPort.verifyGoogle(idToken)
        val email = account.email.lowercase()
        if (!adminPersistencePort.bindGoogleSub(email, account.sub)) {
            // 토큰은 남기지 않는다. 누가 시도했는지 추적할 수 있게 이메일만 남긴다.
            log.warn { "어드민이 아니거나 다른 구글 계정의 어드민 로그인 시도: email=$email" }
            throw BusinessException(AdminErrorCode.ADMIN_FORBIDDEN)
        }
        val issued = adminTokenPort.issue(email, account.sub)
        return AdminLogin(accessToken = issued.token, expiresAt = issued.expiresAt, email = email)
    }

    override fun isAdmin(
        email: String,
        googleSub: String,
    ): Boolean = adminPersistencePort.isBound(email.lowercase(), googleSub)
}
