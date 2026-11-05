package com.example.backend.user.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.application.port.inbound.UserLastLocationUseCase
import com.example.backend.user.application.port.outbound.UserLastLocationPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserLastLocationService(
    private val userLastLocationPersistencePort: UserLastLocationPersistencePort,
) : UserLastLocationUseCase {
    override fun record(
        userId: Long,
        location: Coordinate,
    ) {
        // 추천 용도라 약 100m(소수 3자리)면 충분하다. 필요 이상 정밀한 위치는 보관하지 않는다.
        // 활성 확인과 탈퇴 직렬화는 저장 문장 안에서 한다(users 행 FOR SHARE).
        userLastLocationPersistencePort.saveIfActive(
            userId,
            Coordinate(location.latitude.round3(), location.longitude.round3()),
        )
    }

    private fun Double.round3(): Double = Math.round(this * 1000) / 1000.0
}
