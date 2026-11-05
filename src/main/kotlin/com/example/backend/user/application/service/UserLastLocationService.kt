package com.example.backend.user.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.application.port.inbound.UserLastLocationUseCase
import com.example.backend.user.application.port.outbound.UserLastLocationPersistencePort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserLastLocationService(
    private val userLastLocationPersistencePort: UserLastLocationPersistencePort,
    private val userPersistencePort: UserPersistencePort,
) : UserLastLocationUseCase {
    override fun record(
        userId: Long,
        location: Coordinate,
    ) {
        // 탈퇴와 같은 사용자 행 잠금으로 직렬화해, 탈퇴 후 남은 access token 으로 위치가 다시 생기지 않게 한다.
        if (userPersistencePort.lockActive(listOf(userId)).isEmpty()) return
        // 추천 용도라 약 100m(소수 3자리)면 충분하다. 필요 이상 정밀한 위치는 보관하지 않는다.
        userLastLocationPersistencePort.save(
            userId,
            Coordinate(location.latitude.round3(), location.longitude.round3()),
        )
    }

    private fun Double.round3(): Double = Math.round(this * 1000) / 1000.0
}
