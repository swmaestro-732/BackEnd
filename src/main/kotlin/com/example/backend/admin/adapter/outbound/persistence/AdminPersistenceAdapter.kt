package com.example.backend.admin.adapter.outbound.persistence

import com.example.backend.admin.adapter.outbound.persistence.exposed.AdminActionTable
import com.example.backend.admin.adapter.outbound.persistence.exposed.AdminTable
import com.example.backend.admin.application.port.outbound.AdminActionPersistencePort
import com.example.backend.admin.application.port.outbound.AdminPersistencePort
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Component

@Component
class AdminPersistenceAdapter :
    AdminPersistencePort,
    AdminActionPersistencePort {
    override fun isRegistered(
        email: String,
        googleSub: String,
    ): Boolean =
        AdminTable
            .selectAll()
            .where { (AdminTable.email eq email) and (AdminTable.googleSub eq googleSub) }
            .empty()
            .not()

    override fun record(
        adminEmail: String,
        targetType: String,
        targetId: Long,
        action: String,
        memo: String?,
    ) {
        AdminActionTable.insert {
            it[AdminActionTable.adminEmail] = adminEmail
            it[AdminActionTable.targetType] = targetType
            it[AdminActionTable.targetId] = targetId
            it[AdminActionTable.action] = action
            it[AdminActionTable.memo] = memo
        }
    }
}
