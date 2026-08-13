package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.adapter.outbound.persistence.exposed.SavedPlaceTable
import com.example.backend.user.adapter.outbound.persistence.exposed.UserTable
import com.example.backend.user.application.port.outbound.SavedPlacePersistencePort
import com.example.backend.user.domain.model.SavedPlaceCategory
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.temporal.ChronoUnit
import kotlin.time.toJavaInstant

/**
 * saved_places 영속성 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 *
 * DSL insert 가 생성 id·저장 시각을 재조회 없이 돌려주는지, 소프트 삭제와 partial 유니크 인덱스
 * (V16, deleted_at IS NULL 한정)가 맞물려 동작하는지, 커서 페이징·배지 카운트 집계가 맞는지 검증한다.
 * 각 테스트는 transaction { ... rollback() } 으로 격리한다(픽스처 오염 없음).
 */
class SavedPlacePersistenceTest
    @Autowired
    constructor(
        private val port: SavedPlacePersistencePort,
    ) : IntegrationTestBase() {
        // --- insert ---

        @Test
        fun `insert 는 생성된 id 와 저장 시각을 담은 도메인을 반환하고 DB 행과 일치한다`() {
            transaction {
                val userId = insertUser("저장러1")

                val saved = port.insert(userId, placeId = 777L, category = SavedPlaceCategory.CAFE)

                assertTrue(saved.id > 0) // 시퀀스가 발급한 id 를 되받았다
                assertEquals(userId, saved.userId)
                assertEquals(777L, saved.placeId)
                assertEquals(SavedPlaceCategory.CAFE, saved.category)
                assertFalse(saved.visited) // 저장 직후는 미방문
                assertNull(saved.deletedAt)

                val row = SavedPlaceTable.selectAll().where { SavedPlaceTable.id eq saved.id }.single()
                assertEquals(userId, row[SavedPlaceTable.userId])
                assertEquals(777L, row[SavedPlaceTable.placeId])
                assertEquals(SavedPlaceCategory.CAFE, row[SavedPlaceTable.category])
                assertFalse(row[SavedPlaceTable.visited])
                assertNull(row[SavedPlaceTable.deletedAt])

                // 반환한 savedAt 이 실제 저장된 created_at 과 같은 시각인지.
                // ms 로 절삭해 비교한다 — Postgres timestamptz 는 µs 까지만 담고(그 이하는 반올림),
                // JVM 시계가 더 미세한 자리를 줄 수 있어 정확한 동일성은 플랫폼에 의존한다.
                assertEquals(
                    row[SavedPlaceTable.createdAt].toJavaInstant().truncatedTo(ChronoUnit.MILLIS),
                    saved.savedAt.truncatedTo(ChronoUnit.MILLIS),
                )
                rollback()
            }
        }

        @Test
        fun `category 가 null 이면 미분류로 저장된다`() {
            transaction {
                val userId = insertUser("저장러2")

                val saved = port.insert(userId, placeId = 778L, category = null)

                assertNull(saved.category)
                val row = SavedPlaceTable.selectAll().where { SavedPlaceTable.id eq saved.id }.single()
                assertNull(row[SavedPlaceTable.category])
                rollback()
            }
        }

        @Test
        fun `살아있는 중복 저장은 유니크 인덱스가 막는다`() {
            transaction {
                val userId = insertUser("저장러3")
                port.insert(userId, placeId = 779L, category = null)

                // uq_saved_places_user_place (V16) 위반 → 23505. 서비스의 사전검사와 별개인 최종 방어선.
                val ex =
                    org.junit.jupiter.api.assertThrows<ExposedSQLException> {
                        port.insert(userId, placeId = 779L, category = null)
                    }
                assertEquals("23505", ex.sqlState)
                rollback()
            }
        }

        // --- exists / 소프트 삭제 ---

        @Test
        fun `existsSavedPlace 는 살아있는 행만 true 이고 소프트 삭제된 행은 없는 것으로 본다`() {
            transaction {
                val userId = insertUser("저장러4")
                port.insert(userId, placeId = 780L, category = null)

                assertTrue(port.existsSavedPlace(userId, 780L))
                assertFalse(port.existsSavedPlace(userId, 781L)) // 저장하지 않은 장소

                port.deleteByUserAndPlace(userId, 780L)
                assertFalse(port.existsSavedPlace(userId, 780L)) // 소프트 삭제 후
                rollback()
            }
        }

        @Test
        fun `deleteByUserAndPlace 는 삭제 스탬프만 찍고 두 번째 호출은 false 다 (멱등)`() {
            transaction {
                val userId = insertUser("저장러5")
                val saved = port.insert(userId, placeId = 782L, category = null)

                assertTrue(port.deleteByUserAndPlace(userId, 782L))

                // 하드 삭제가 아니라 deleted_at 스탬프 — 행은 남아 있다.
                val row = SavedPlaceTable.selectAll().where { SavedPlaceTable.id eq saved.id }.single()
                assertNotNull(row[SavedPlaceTable.deletedAt])

                assertFalse(port.deleteByUserAndPlace(userId, 782L)) // 이미 삭제됨 = 0행
                rollback()
            }
        }

        @Test
        fun `소프트 삭제 후 같은 장소를 다시 저장할 수 있다 (partial 유니크 인덱스)`() {
            transaction {
                val userId = insertUser("저장러6")
                val first = port.insert(userId, placeId = 783L, category = null)
                port.deleteByUserAndPlace(userId, 783L)

                val second = port.insert(userId, placeId = 783L, category = SavedPlaceCategory.BAR)

                assertTrue(second.id != first.id) // 되살리는 게 아니라 새 행
                assertTrue(port.existsSavedPlace(userId, 783L))
                rollback()
            }
        }

        // --- findPage ---

        @Test
        fun `findPage 는 최신 저장순(id 내림차순)으로 limit 개까지 반환한다`() {
            transaction {
                val userId = insertUser("저장러7")
                val a = port.insert(userId, placeId = 1L, category = null)
                val b = port.insert(userId, placeId = 2L, category = null)
                val c = port.insert(userId, placeId = 3L, category = null)

                val page = port.findPage(userId, visited = false, category = null, cursorId = null, limit = 2)

                assertEquals(listOf(c.id, b.id), page.map { it.id })
                assertTrue(a.id < b.id) // 삽입 순서대로 id 증가 → 내림차순이 곧 최신순
                rollback()
            }
        }

        @Test
        fun `findPage 는 cursorId 보다 작은 id 만 반환한다 (다음 페이지)`() {
            transaction {
                val userId = insertUser("저장러8")
                val a = port.insert(userId, placeId = 1L, category = null)
                val b = port.insert(userId, placeId = 2L, category = null)
                val c = port.insert(userId, placeId = 3L, category = null)

                val page = port.findPage(userId, visited = false, category = null, cursorId = c.id, limit = 10)

                assertEquals(listOf(b.id, a.id), page.map { it.id }) // 커서 행(c)은 제외
                rollback()
            }
        }

        @Test
        fun `findPage 는 visited 와 category 로 필터링하고 소프트 삭제된 행을 제외한다`() {
            transaction {
                val userId = insertUser("저장러9")
                val cafe = port.insert(userId, placeId = 1L, category = SavedPlaceCategory.CAFE)
                val bar = port.insert(userId, placeId = 2L, category = SavedPlaceCategory.BAR)
                val visitedCafe = port.insert(userId, placeId = 3L, category = SavedPlaceCategory.CAFE)
                markVisited(visitedCafe.id)
                val deleted = port.insert(userId, placeId = 4L, category = SavedPlaceCategory.CAFE)
                port.deleteByUserAndPlace(userId, 4L)

                val unvisited = port.findPage(userId, visited = false, category = null, cursorId = null, limit = 10)
                assertEquals(listOf(bar.id, cafe.id), unvisited.map { it.id }) // 방문·삭제 행 제외
                assertFalse(deleted.id in unvisited.map { it.id })

                val visited = port.findPage(userId, visited = true, category = null, cursorId = null, limit = 10)
                assertEquals(listOf(visitedCafe.id), visited.map { it.id })

                val unvisitedCafe =
                    port.findPage(
                        userId,
                        visited = false,
                        category = SavedPlaceCategory.CAFE,
                        cursorId = null,
                        limit = 10,
                    )
                assertEquals(listOf(cafe.id), unvisitedCafe.map { it.id })
                rollback()
            }
        }

        @Test
        fun `findPage 는 저장 레코드를 읽기 모델로 매핑한다`() {
            transaction {
                val userId = insertUser("저장러10")
                val saved = port.insert(userId, placeId = 900L, category = SavedPlaceCategory.NATURE)

                val row = port.findPage(userId, visited = false, category = null, cursorId = null, limit = 10).single()

                assertEquals(saved.id, row.id)
                assertEquals(900L, row.placeId)
                assertEquals(SavedPlaceCategory.NATURE, row.category)
                assertFalse(row.visited)
                assertEquals(
                    saved.savedAt.truncatedTo(ChronoUnit.MILLIS),
                    row.savedAt.truncatedTo(ChronoUnit.MILLIS),
                )
                rollback()
            }
        }

        // --- 카운트 ---

        @Test
        fun `countByVisited 는 방문 여부별로 살아있는 행만 센다`() {
            transaction {
                val userId = insertUser("저장러11")
                port.insert(userId, placeId = 1L, category = null)
                port.insert(userId, placeId = 2L, category = null)
                val visited = port.insert(userId, placeId = 3L, category = null)
                markVisited(visited.id)
                port.insert(userId, placeId = 4L, category = null)
                port.deleteByUserAndPlace(userId, 4L) // 소프트 삭제 → 어느 쪽에도 안 센다

                assertEquals(2L, port.countByVisited(userId, visited = false))
                assertEquals(1L, port.countByVisited(userId, visited = true))
                rollback()
            }
        }

        @Test
        fun `countByCategory 는 미분류(null)를 제외하고 카테고리별로 센다`() {
            transaction {
                val userId = insertUser("저장러12")
                port.insert(userId, placeId = 1L, category = SavedPlaceCategory.CAFE)
                port.insert(userId, placeId = 2L, category = SavedPlaceCategory.CAFE)
                port.insert(userId, placeId = 3L, category = SavedPlaceCategory.BAR)
                port.insert(userId, placeId = 4L, category = null) // 미분류는 집계 대상 아님

                val counts = port.countByCategory(userId).associate { it.category to it.count }

                assertEquals(mapOf(SavedPlaceCategory.CAFE to 2L, SavedPlaceCategory.BAR to 1L), counts)
                rollback()
            }
        }

        @Test
        fun `카운트와 조회는 다른 사용자의 저장을 섞지 않는다`() {
            transaction {
                val me = insertUser("저장러13")
                val other = insertUser("저장러14")
                port.insert(me, placeId = 1L, category = SavedPlaceCategory.CAFE)
                port.insert(other, placeId = 1L, category = SavedPlaceCategory.CAFE)
                port.insert(other, placeId = 2L, category = SavedPlaceCategory.BAR)

                assertEquals(1L, port.countByVisited(me, visited = false))
                assertEquals(1, port.findPage(me, false, null, null, 10).size)
                assertEquals(1, port.countByCategory(me).size)
                rollback()
            }
        }

        /** 저장 레코드의 소유자로 쓸 활성 사용자 한 명. saved_places.user_id 는 users 를 FK 로 참조한다. */
        private fun insertUser(nickname: String): Long =
            UserTable
                .insert {
                    it[UserTable.nickname] = nickname
                    it[handle] = "h_$nickname"
                    it[status] = 0 // ACTIVE
                    it[followersCnt] = 0
                    it[followingsCnt] = 0
                    it[publicCoursesCnt] = 0
                    it[followerCoursesCnt] = 0
                    it[privateCoursesCnt] = 0
                }[UserTable.id]
                .value

        /** 방문 처리(PATCH) 를 흉내낸다 — 방문 탭 필터·카운트 검증용. */
        private fun markVisited(savedPlaceId: Long) {
            SavedPlaceTable.update({ (SavedPlaceTable.id eq savedPlaceId) and SavedPlaceTable.deletedAt.isNull() }) {
                it[visited] = true
            }
        }
    }
