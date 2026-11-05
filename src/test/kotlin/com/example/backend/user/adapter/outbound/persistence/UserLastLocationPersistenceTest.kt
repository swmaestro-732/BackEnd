package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.common.geo.Coordinate
import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.adapter.outbound.persistence.exposed.UserLastLocationTable
import com.example.backend.user.application.port.outbound.UserLastLocationPersistencePort
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.time.Instant

class UserLastLocationPersistenceTest
    @Autowired
    constructor(
        private val userLastLocationPersistencePort: UserLastLocationPersistencePort,
    ) : IntegrationTestBase() {
        @Test
        fun `같은 사용자를 다시 저장하면 한 행의 좌표와 갱신 시각만 바뀐다`() {
            transaction {
                insertUser()
                userLastLocationPersistencePort.save(ME, FIRST)
                UserLastLocationTable.update({ UserLastLocationTable.userId eq ME }) { it[updatedAt] = OLD }

                userLastLocationPersistencePort.save(ME, SECOND)

                assertEquals(SECOND, userLastLocationPersistencePort.find(ME))
                val rows = UserLastLocationTable.selectAll().where { UserLastLocationTable.userId eq ME }.toList()
                assertEquals(1, rows.size)
                assertTrue(rows.single()[UserLastLocationTable.updatedAt] > OLD)
                rollback()
            }
        }

        @Test
        fun `deleteByUser는 그 사용자의 위치를 지운다`() {
            transaction {
                insertUser()
                userLastLocationPersistencePort.save(ME, FIRST)

                userLastLocationPersistencePort.deleteByUser(ME)

                assertNull(userLastLocationPersistencePort.find(ME))
                rollback()
            }
        }

        private fun insertUser() {
            TransactionManager.current().exec("INSERT INTO users (id, nickname) VALUES ($ME, '위치주인')")
        }

        private companion object {
            const val ME = 780101L
            val FIRST = Coordinate(37.544, 127.056)
            val SECOND = Coordinate(35.1796, 129.0756)
            val OLD = Instant.parse("2000-01-01T00:00:00Z")
        }
    }
