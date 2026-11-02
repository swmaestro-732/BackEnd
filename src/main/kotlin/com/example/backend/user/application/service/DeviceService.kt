package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.user.application.port.inbound.DeviceUseCase
import com.example.backend.user.application.port.inbound.dto.DeviceConsent
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import com.example.backend.user.application.port.inbound.dto.UpdateDeviceCommand
import com.example.backend.user.application.port.outbound.DevicePersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional
class DeviceService(
    private val devicePersistencePort: DevicePersistencePort,
    private val clock: Clock,
) : DeviceUseCase {
    override fun update(command: UpdateDeviceCommand): DeviceConsent {
        command.fcmToken?.let {
            // 비로그인 등록은 계정 연결을 바꾸지 않는다(userId = null 이면 기존 연결 유지).
            devicePersistencePort.saveToken(
                RegisterDeviceTokenCommand(command.deviceId, it, command.platform, command.appBuild),
                null,
            )
        }
        if (command.marketing == null && command.night == null) {
            return devicePersistencePort.findConsent(command.deviceId) ?: DeviceConsent.NONE
        }
        val current = devicePersistencePort.lockConsent(command.deviceId)
        val now = clock.instant()
        // 이미 동의한 상태에서 다시 동의하면 처음 동의 시각을 유지한다(2년 재확인 기준).
        val marketingAgreedAt =
            when (command.marketing) {
                true -> current.marketingAgreedAt ?: now
                false -> null
                null -> current.marketingAgreedAt
            }
        if (command.night == true && marketingAgreedAt == null) {
            throw BusinessException(CommonErrorCode.INVALID_INPUT, "야간 수신 동의는 광고성 정보 수신 동의가 필요합니다.")
        }
        // 광고 동의를 철회하면 야간 동의도 함께 철회한다.
        val nightAgreedAt =
            when {
                marketingAgreedAt == null -> null
                command.night == true -> current.nightAgreedAt ?: now
                command.night == false -> null
                else -> current.nightAgreedAt
            }
        devicePersistencePort.updateConsent(command.deviceId, marketingAgreedAt, nightAgreedAt)
        return DeviceConsent(marketingAgreedAt, nightAgreedAt)
    }

    @Transactional(readOnly = true)
    override fun getConsent(deviceId: String): DeviceConsent =
        devicePersistencePort.findConsent(deviceId) ?: DeviceConsent.NONE
}
