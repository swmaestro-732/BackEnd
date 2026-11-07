package com.example.backend.admin.adapter.outbound.persistence

import com.example.backend.admin.adapter.outbound.persistence.exposed.AdminTable
import com.example.backend.admin.application.port.outbound.AdminPersistencePort
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Component

@Component
class AdminPersistenceAdapter : AdminPersistencePort {
    override fun exists(email: String): Boolean =
        AdminTable
            .selectAll()
            .where { AdminTable.email eq email }
            .empty()
            .not()

    override fun bindGoogleSub(
        email: String,
        googleSub: String,
    ): Boolean =
        AdminTable.update({
            (AdminTable.email eq email) and (AdminTable.googleSub.isNull() or (AdminTable.googleSub eq googleSub))
        }) {
            it[AdminTable.googleSub] = googleSub
        } > 0
}
