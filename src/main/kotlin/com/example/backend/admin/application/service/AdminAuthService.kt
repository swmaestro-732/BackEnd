package com.example.backend.admin.application.service

import com.example.backend.admin.application.port.inbound.AdminAccessUseCase
import com.example.backend.admin.application.port.inbound.AdminAuthUseCase
import com.example.backend.admin.application.port.inbound.AdminNotRegisteredException
import com.example.backend.admin.application.port.inbound.dto.AdminLogin
import com.example.backend.admin.application.port.outbound.AdminIdentityPort
import com.example.backend.admin.application.port.outbound.AdminPersistencePort
import com.example.backend.admin.application.port.outbound.AdminTokenPort
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
     * 이메일과 구글 계정(sub)이 모두 등록된 경우에만 토큰을 준다. 같은 이메일이 다른 구글 계정에 붙는 경우
     * (회사 메일 재할당, 도메인 만료 후 재등록)에도 등록한 계정만 통과한다.
     * 등록되지 않았으면 본인의 이메일과 sub 를 담아 거부해 운영자에게 전달할 수 있게 한다.
     */
    override fun login(idToken: String): AdminLogin {
        val account = adminIdentityPort.verifyGoogle(idToken)
        val email = account.email
        if (!adminPersistencePort.isRegistered(email, account.sub)) {
            // 토큰은 남기지 않는다. 등록 요청을 확인할 수 있게 이메일과 구글 계정 id 를 남긴다.
            log.warn { "등록되지 않은 계정의 어드민 로그인 시도: email=$email, googleSub=${account.sub}" }
            throw AdminNotRegisteredException(email, account.sub)
        }
        val issued = adminTokenPort.issue(email, account.sub)
        return AdminLogin(accessToken = issued.token, expiresAt = issued.expiresAt, email = email)
    }

    override fun isAdmin(
        email: String,
        googleSub: String,
    ): Boolean = adminPersistencePort.isRegistered(email.lowercase(), googleSub)
}
