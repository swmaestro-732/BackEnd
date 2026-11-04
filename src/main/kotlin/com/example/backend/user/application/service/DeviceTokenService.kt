package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.UserErrorCode
import com.example.backend.user.application.port.inbound.DeviceTokenUseCase
import com.example.backend.user.application.port.inbound.MarketingDeviceUseCase
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import com.example.backend.user.application.port.outbound.DevicePersistencePort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DeviceTokenService(
    private val devicePersistencePort: DevicePersistencePort,
    private val userPersistencePort: UserPersistencePort,
) : DeviceTokenUseCase,
    MarketingDeviceUseCase {
    override fun register(
        userId: Long,
        command: RegisterDeviceTokenCommand,
    ) {
        // 탈퇴와 같은 사용자 행 잠금으로 직렬화해, 탈퇴 후 남은 access token 으로 토큰이 다시 생기지 않게 한다.
        if (userPersistencePort.lockActive(listOf(userId)).isEmpty()) {
            throw BusinessException(UserErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$userId")
        }
        devicePersistencePort.saveToken(command, userId)
    }

    override fun unregister(
        userId: Long,
        deviceId: String,
    ) = devicePersistencePort.unlink(userId, deviceId)

    override fun clearInvalidTokens(fcmTokens: Collection<String>) {
        if (fcmTokens.isEmpty()) return
        devicePersistencePort.clearTokens(fcmTokens)
    }

    @Transactional(readOnly = true)
    override fun findMarketingTokens(night: Boolean): List<String> = devicePersistencePort.findMarketingTokens(night)
}
