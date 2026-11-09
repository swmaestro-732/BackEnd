package com.example.backend.notification.application.service

import com.example.backend.notification.application.port.inbound.NotificationCleanupUseCase
import com.example.backend.notification.application.port.outbound.NotificationNicknamePort
import com.example.backend.notification.application.port.outbound.NotificationPersistencePort
import com.example.backend.notification.application.port.outbound.NotificationSettingsPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class NotificationCleanupService(
    private val notificationPersistencePort: NotificationPersistencePort,
    private val notificationSettingsPersistencePort: NotificationSettingsPersistencePort,
    private val notificationNicknamePort: NotificationNicknamePort,
    private val clock: Clock,
) : NotificationCleanupUseCase {
    @Transactional
    override fun purgeByUser(userId: Long) {
        val displayActorByBundle = notificationPersistencePort.deleteAllInvolving(userId)
        // 탈퇴자가 대표 행동자였던 묶음은 남은 사람과 그 닉네임으로 바꿔 다른 사람 활동 알림을 지키고 탈퇴자 닉네임은 지운다.
        val nicknames = notificationNicknamePort.findNicknames(displayActorByBundle.values.toSet())
        displayActorByBundle.forEach { (id, actorId) ->
            notificationPersistencePort.updateActorNickname(id, nicknames[actorId])
        }
        notificationSettingsPersistencePort.delete(userId)
    }

    @Transactional
    override fun purgeExpired(): Int =
        notificationPersistencePort.deleteCreatedBefore(clock.instant().minus(NotificationCleanupUseCase.RETENTION))
}
