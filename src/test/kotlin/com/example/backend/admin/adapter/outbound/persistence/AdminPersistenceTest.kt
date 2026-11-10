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
        fun `이메일과 구글 sub 가 함께 등록된 경우에만 등록됐다고 답한다`() {
            transaction {
                AdminTable.insert {
                    it[email] = "persistence-admin@example.com"
                    it[googleSub] = "sub-a"
                }

                assertTrue(adminPersistencePort.isRegistered("persistence-admin@example.com", "sub-a"))
                assertFalse(adminPersistencePort.isRegistered("persistence-admin@example.com", "sub-b"))
                assertFalse(adminPersistencePort.isRegistered("persistence-other@example.com", "sub-a"))
                rollback()
            }
        }
    }
