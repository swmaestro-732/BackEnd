package com.example.backend.admin.adapter.outbound.persistence

import com.example.backend.admin.adapter.outbound.persistence.exposed.AdminTable
import com.example.backend.admin.application.port.outbound.AdminPersistencePort
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Component

@Component
class AdminPersistenceAdapter : AdminPersistencePort {
    override fun isRegistered(
        email: String,
        googleSub: String,
    ): Boolean =
        AdminTable
            .selectAll()
            .where { (AdminTable.email eq email) and (AdminTable.googleSub eq googleSub) }
            .empty()
            .not()
}
