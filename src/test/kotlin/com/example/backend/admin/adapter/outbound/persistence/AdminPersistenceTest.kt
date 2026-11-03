package com.example.backend.admin.adapter.outbound.persistence

import com.example.backend.admin.adapter.outbound.persistence.exposed.AdminTable
import com.example.backend.admin.application.port.outbound.AdminPersistencePort
import com.example.backend.support.IntegrationTestBase
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AdminPersistenceTest
    @Autowired
    constructor(
        private val adminPersistencePort: AdminPersistencePort,
    ) : IntegrationTestBase() {
        @Test
        fun `admins 에 있는 이메일만 존재한다고 답한다`() {
            transaction {
                AdminTable.insert { it[email] = "persistence-admin@example.com" }

                assertTrue(adminPersistencePort.exists("persistence-admin@example.com"))
                assertFalse(adminPersistencePort.exists("persistence-other@example.com"))
                rollback()
            }
        }
    }
