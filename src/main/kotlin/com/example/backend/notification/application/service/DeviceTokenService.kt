package com.example.backend.notification.application.service

import com.example.backend.notification.application.port.inbound.DeviceTokenUseCase
import com.example.backend.notification.application.port.inbound.dto.RegisterDeviceTokenCommand
import com.example.backend.notification.application.port.outbound.DeviceTokenPersistencePort
import com.example.backend.notification.application.port.outbound.NotificationUserPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DeviceTokenService(
    private val deviceTokenPersistencePort: DeviceTokenPersistencePort,
    private val users: NotificationUserPort,
) : DeviceTokenUseCase {
    override fun register(
        userId: Long,
        command: RegisterDeviceTokenCommand,
    ) {
        // 탈퇴 정리와 같은 사용자 행 잠금으로 직렬화해, 탈퇴 후 남은 access token 으로 토큰이 다시 생기지 않게 한다.
        users.lockActive(userId)
        deviceTokenPersistencePort.upsert(userId, command)
    }

    override fun unregister(
        userId: Long,
        token: String,
    ) = deviceTokenPersistencePort.deleteByUserAndToken(userId, token)
}
