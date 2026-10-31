package com.example.backend.user.application.event

import com.example.backend.user.application.port.inbound.UserActivityKind
import com.example.backend.user.application.port.inbound.UserActivityOccurred
import java.time.Instant

data class UserActivityEvent(
    override val kind: UserActivityKind,
    override val actorId: Long,
    override val targetId: Long,
    override val occurredAt: Instant,
) : UserActivityOccurred
