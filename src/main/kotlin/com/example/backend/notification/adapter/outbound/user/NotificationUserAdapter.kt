package com.example.backend.notification.adapter.outbound.user

import com.example.backend.notification.application.port.outbound.NotificationUserPort
import com.example.backend.user.application.port.inbound.UserUseCase
import org.springframework.stereotype.Component

@Component
class NotificationUserAdapter(
    private val users: UserUseCase,
) : NotificationUserPort {
    override fun lockActive(userId: Long) = users.lockActive(userId)
}
