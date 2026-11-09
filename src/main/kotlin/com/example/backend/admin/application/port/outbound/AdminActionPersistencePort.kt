package com.example.backend.admin.application.port.outbound

interface AdminActionPersistencePort {
    /** 어드민 조치 한 건을 admin_actions 에 남긴다. */
    fun record(
        adminEmail: String,
        targetType: String,
        targetId: Long,
        action: String,
        memo: String?,
    )
}
