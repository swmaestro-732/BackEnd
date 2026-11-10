package com.example.backend.admin.application.service

import com.example.backend.admin.application.port.inbound.AdminActionUseCase
import com.example.backend.admin.application.port.outbound.AdminActionPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** [AdminActionUseCase] 구현. 기본 전파(REQUIRED)라 조치를 실행한 트랜잭션에 함께 커밋된다. */
@Service
@Transactional
class AdminActionService(
    private val adminActionPersistencePort: AdminActionPersistencePort,
) : AdminActionUseCase {
    override fun record(
        adminEmail: String,
        targetType: String,
        targetId: Long,
        action: String,
        memo: String?,
    ) = adminActionPersistencePort.record(adminEmail, targetType, targetId, action, memo)
}
