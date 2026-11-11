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
    /**
     * 활성 사용자일 때만 최근 위치를 저장한다.
     * - 10분 안에 500m 안쪽 재진입이면 잠금 없이 읽기만 하고 끝낸다. 홈 조회 대부분은 여기서 끝나 잠금과 쓰기가 없다.
     *   쓰기가 없는 트랜잭션은 잠금과 WAL 비용이 없고, readOnly 로 나눠도 읽기 전용 라우팅이 없어 이득이 없다.
     * - users 행 FOR SHARE: 탈퇴가 그 행을 FOR UPDATE 로 잡고 있으면 커밋까지 기다렸다가 deleted_at 을 다시 평가한다.
     *   탈퇴가 위치 행을 지운 뒤에 다시 써서 탈퇴자 위치가 남는 일을 막는다. 잠금과 저장이 이 트랜잭션 하나에 묶인다.
     */
    override fun record(
        userId: Long,
        location: Coordinate,
    ) {
        // 추천 용도라 약 100m(소수 3자리)면 충분하다. 필요 이상 정밀한 위치는 보관하지 않는다.
        val rounded = Coordinate(location.latitude.round3(), location.longitude.round3())
        if (userLastLocationPersistencePort.isRecentNear(userId, rounded)) return
        if (userId !in userPersistencePort.lockActiveShared(listOf(userId))) return
        userLastLocationPersistencePort.save(userId, rounded)
    }

    private fun Double.round3(): Double = Math.round(this * 1000) / 1000.0
}
