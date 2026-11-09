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
import org.junit.jupiter.api.Assertions.assertFalse
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
        fun `갱신한 지 오래됐으면 같은 자리여도 한 행의 좌표와 갱신 시각을 바꾼다`() {
            transaction {
                insertUsers()
                userLastLocationPersistencePort.save(ME, SEOUL)
                UserLastLocationTable.update({ UserLastLocationTable.userId eq ME }) { it[updatedAt] = OLD }

                userLastLocationPersistencePort.save(ME, SEOUL_NEAR)

                val rows = rows(ME)
                assertEquals(1, rows.size)
                assertEquals(SEOUL_NEAR, rows.single()[UserLastLocationTable.location])
                assertTrue(rows.single()[UserLastLocationTable.updatedAt] > OLD)
                rollback()
            }
        }

        @Test
        fun `최근에 갱신했고 500m 안이면 덮어쓰지 않는다`() {
            transaction {
                insertUsers()
                userLastLocationPersistencePort.save(ME, SEOUL)

                userLastLocationPersistencePort.save(ME, SEOUL_NEAR)

                assertEquals(SEOUL, rows(ME).single()[UserLastLocationTable.location])
                rollback()
            }
        }

        @Test
        fun `최근에 갱신했어도 500m 넘게 움직였으면 덮어쓴다`() {
            transaction {
                insertUsers()
                userLastLocationPersistencePort.save(ME, SEOUL)

                userLastLocationPersistencePort.save(ME, BUSAN)

                assertEquals(BUSAN, rows(ME).single()[UserLastLocationTable.location])
                rollback()
            }
        }

        @Test
        fun `isRecentNear는 10분 안에 500m 안쪽으로 저장된 위치만 true`() {
            transaction {
                insertUsers()
                userLastLocationPersistencePort.save(ME, SEOUL)

                assertTrue(userLastLocationPersistencePort.isRecentNear(ME, SEOUL_NEAR))
                assertFalse(userLastLocationPersistencePort.isRecentNear(ME, BUSAN))
                assertFalse(userLastLocationPersistencePort.isRecentNear(OTHER, SEOUL))
                UserLastLocationTable.update({ UserLastLocationTable.userId eq ME }) { it[updatedAt] = OLD }
                assertFalse(userLastLocationPersistencePort.isRecentNear(ME, SEOUL))
                rollback()
            }
        }

        @Test
        fun `deleteByUser는 그 사용자의 위치만 지운다`() {
            transaction {
                insertUsers()
                userLastLocationPersistencePort.save(ME, SEOUL)
                userLastLocationPersistencePort.save(OTHER, BUSAN)

                userLastLocationPersistencePort.deleteByUser(ME)

                assertTrue(rows(ME).isEmpty())
                assertEquals(BUSAN, rows(OTHER).single()[UserLastLocationTable.location])
                rollback()
            }
        }

        private fun insertUsers() {
            TransactionManager.current().exec(
                "INSERT INTO users (id, nickname) VALUES ($ME, '위치주인'), ($OTHER, '다른사용자')",
            )
        }

        private fun rows(userId: Long) =
            UserLastLocationTable.selectAll().where { UserLastLocationTable.userId eq userId }.toList()

        private companion object {
            const val ME = 780101L
            const val OTHER = 780102L
            val SEOUL = Coordinate(37.544, 127.056)

            // SEOUL 에서 약 110m.
            val SEOUL_NEAR = Coordinate(37.545, 127.056)
            val BUSAN = Coordinate(35.18, 129.076)
            val OLD = Instant.parse("2000-01-01T00:00:00Z")
        }
    }
