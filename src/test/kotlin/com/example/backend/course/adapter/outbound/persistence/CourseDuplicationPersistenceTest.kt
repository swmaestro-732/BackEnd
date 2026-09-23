package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseEntity
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTable
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CoursePlace
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.support.IntegrationTestBase
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import javax.sql.DataSource

class CourseDuplicationPersistenceTest
    @Autowired
    constructor(
        private val port: CoursePersistencePort,
        private val dataSource: DataSource,
    ) : IntegrationTestBase() {
        @Test
        fun `복제 원본과 개수는 저장 재조회 및 장소 편집 후에도 유지된다`() {
            transaction {
                val originId =
                    CourseTable
                        .insertAndGetId {
                            it[userId] = 1L
                            it[title] = "원본 코스"
                            it[status] = CourseStatus.ACTIVE
                            it[visibility] = CourseVisibility.PUBLIC
                            it[isPublished] = true
                        }.value
                val saved =
                    port.save(
                        Course.create(
                            userId = 1L,
                            title = "복제 초안",
                            description = null,
                            coverImageUrl = null,
                            visibility = CourseVisibility.PRIVATE,
                            isPublished = false,
                            duplicatedFromId = originId,
                            originalPlaceCount = 10,
                            sharedPlaceCount = 2,
                            tags = emptyList(),
                            places = coursePlaces(1L, 2L),
                            placeCategoryByPlaceId = emptyMap(),
                            areaCode = null,
                            area = null,
                        ),
                    )
                val savedId = requireNotNull(saved.id)
                assertSnapshot(saved, originId)
                val reloaded = CourseEntity[savedId].apply { refresh(flush = true) }.toDomain(emptyList(), saved.places)
                assertSnapshot(reloaded, originId)

                val edited =
                    port.update(
                        Course.edit(
                            id = savedId,
                            userId = 1L,
                            title = "장소를 바꾼 복제 초안",
                            description = null,
                            coverImageUrl = null,
                            visibility = CourseVisibility.PRIVATE,
                            isPublished = false,
                            tags = emptyList(),
                            places = coursePlaces(3L, 4L),
                            wasPublished = false,
                            existingCategory = null,
                            placeCategoryByPlaceId = emptyMap(),
                            areaCode = null,
                            area = null,
                        ),
                    )

                assertSnapshot(edited, originId)
                val afterEdit =
                    CourseEntity[savedId]
                        .apply {
                            refresh(
                                flush = true,
                            )
                        }.toDomain(emptyList(), edited.places)
                assertSnapshot(afterEdit, originId)
                assertEquals("장소를 바꾼 복제 초안", afterEdit.title)
                assertEquals(listOf(3L, 4L), port.findPlaces(savedId).map { it.placeId })
                rollback()
            }
        }

        @Test
        fun `V9는 기존 원본 참조와 FK를 보존하고 과거 개수는 null로 남긴다`() {
            // V8의 변경 대상 컬럼과 FK를 세션 전용 임시 테이블로 재현한다.
            // pg_temp가 우선하므로 실제 courses 및 Flyway 이력은 변경하지 않는다.
            val migration =
                requireNotNull(javaClass.getResource("/db/migration/V9__course_duplication_snapshot.sql"))
                    .readText()
            dataSource.connection.use { connection ->
                connection.autoCommit = false
                try {
                    connection.createStatement().use { statement ->
                        statement.execute(
                            "CREATE TEMP TABLE courses (id bigint PRIMARY KEY, forked_from_id bigint) ON COMMIT DROP",
                        )
                        statement.execute(
                            "ALTER TABLE pg_temp.courses ADD CONSTRAINT courses_forked_from_id_fkey " +
                                "FOREIGN KEY (forked_from_id) REFERENCES pg_temp.courses(id)",
                        )
                        statement.execute("INSERT INTO pg_temp.courses VALUES (1, NULL), (2, 1)")
                        statement.execute(migration)
                        statement
                            .executeQuery(
                                "SELECT duplicated_from_id, original_place_count, shared_place_count FROM pg_temp.courses WHERE id = 2",
                            ).use { rows ->
                                assertTrue(rows.next())
                                assertEquals(1L, rows.getLong("duplicated_from_id"))
                                assertNull(rows.getObject("original_place_count"))
                                assertNull(rows.getObject("shared_place_count"))
                            }
                        statement
                            .executeQuery(
                                "SELECT conname FROM pg_constraint WHERE conrelid = 'pg_temp.courses'::regclass AND contype = 'f'",
                            ).use { rows ->
                                assertTrue(rows.next())
                                assertEquals("courses_duplicated_from_id_fkey", rows.getString("conname"))
                            }
                    }
                } finally {
                    connection.rollback()
                }
            }
        }

        private fun assertSnapshot(
            course: Course,
            originId: Long,
        ) {
            assertEquals(originId, course.duplicatedFromId)
            assertEquals(10, course.originalPlaceCount)
            assertEquals(2, course.sharedPlaceCount)
        }

        private fun coursePlaces(
            first: Long,
            second: Long,
        ) = listOf(CoursePlace(first, 0, null, emptyList()), CoursePlace(second, 1, null, emptyList()))
    }
