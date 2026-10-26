package com.example.backend.crawl.adapter.outbound.persistence

import com.example.backend.bootstrap.config.CrawlerProperties
import com.example.backend.common.geo.Coordinate
import com.example.backend.course.application.port.inbound.CourseUseCase
import com.example.backend.course.application.port.outbound.CourseSearchIndexPort
import com.example.backend.crawl.adapter.outbound.course.CrawlCourseAdapter
import com.example.backend.crawl.application.port.inbound.dto.CrawlResultCommand
import com.example.backend.crawl.application.port.inbound.dto.CrawledPlaceCommand
import com.example.backend.crawl.application.port.outbound.CrawlImportPersistencePort
import com.example.backend.crawl.application.port.outbound.CrawlPlacePort
import com.example.backend.crawl.application.port.outbound.CrawlWalkingPort
import com.example.backend.crawl.application.port.outbound.ResolvedCrawlPlace
import com.example.backend.crawl.application.service.CrawlImportService
import com.example.backend.crawl.domain.model.CrawlImport
import com.example.backend.crawl.domain.model.CrawlImportStatus
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@TestPropertySource(properties = ["aws.sqs.crawl-results-queue-url=", "aws.sqs.course-count-queue-url="])
class CrawlImportPersistenceIntegrationTest
    @Autowired
    constructor(
        private val persistence: CrawlImportPersistencePort,
        private val courseUseCase: CourseUseCase,
        private val jdbc: JdbcTemplate,
        transactionManager: PlatformTransactionManager,
    ) : IntegrationTestBase() {
        private val tx = TransactionTemplate(transactionManager)

        @MockitoBean
        private lateinit var index: CourseSearchIndexPort

        @BeforeEach
        fun seed() {
            tx.executeWithoutResult {
                jdbc.update("INSERT INTO users (id, nickname) VALUES (?, '크롤러 검증')", AUTHOR_ID)
                listOf(-69501L, -69502L).forEach { id ->
                    jdbc.update(
                        "INSERT INTO places (id, name, category, location, address) " +
                            "VALUES (?, '크롤링 장소', 'CAFE', ST_GeogFromText('SRID=4326;POINT(127 37.5)'), '서울')",
                        id,
                    )
                }
            }
        }

        @AfterEach
        fun clean() {
            tx.executeWithoutResult {
                jdbc.update("DELETE FROM crawl_imports WHERE shortcode LIKE 'scrum-695-%'")
                jdbc.update(
                    "DELETE FROM course_place_images WHERE course_place_id IN " +
                        "(SELECT id FROM course_places WHERE course_id IN (SELECT id FROM courses WHERE user_id = ?))",
                    AUTHOR_ID,
                )
                jdbc.update(
                    "DELETE FROM course_places WHERE course_id IN (SELECT id FROM courses WHERE user_id = ?)",
                    AUTHOR_ID,
                )
                jdbc.update(
                    "DELETE FROM course_tags WHERE course_id IN (SELECT id FROM courses WHERE user_id = ?)",
                    AUTHOR_ID,
                )
                jdbc.update("DELETE FROM courses WHERE user_id = ?", AUTHOR_ID)
                jdbc.update("DELETE FROM places WHERE id IN (-69501, -69502)")
                jdbc.update("DELETE FROM users WHERE id = ?", AUTHOR_ID)
            }
        }

        @Test
        fun `FAILED를 갱신한 REJECTED는 재전달에도 이력을 유지한다`() {
            val command = command().copy(isCourse = false)
            tx.executeWithoutResult {
                persistence.save(
                    CrawlImport(command.shortcode, "old-run", CrawlImportStatus.FAILED, reason = "timeout"),
                )
            }

            service().importResult(command)
            service().importResult(command.copy(runId = "next-run", isCourse = true))

            assertEquals(
                CrawlImport(command.shortcode, command.runId, CrawlImportStatus.REJECTED, reason = "not course"),
                tx.execute { persistence.findByShortcode(command.shortcode) },
            )
            assertEquals(0, courseCount())
        }

        @Test
        fun `코스 생성 후 이력 저장 실패는 함께 롤백하고 재전달은 한 번만 생성한다`() {
            val failingPersistence =
                object : CrawlImportPersistencePort by persistence {
                    override fun save(result: CrawlImport) {
                        // 실제 CourseService가 저장·이벤트 발행까지 끝낸 뒤 장애를 주입한다.
                        assertEquals(1, courseCount())
                        verifyNoInteractions(index)
                        throw IllegalStateException("import history unavailable")
                    }
                }
            assertThrows(IllegalStateException::class.java) { service(failingPersistence).importResult(command()) }

            assertEquals(0, courseCount())
            assertNull(tx.execute { persistence.findByShortcode(command().shortcode) })
            verifyNoInteractions(index)

            service().importResult(command())
            service().importResult(command().copy(runId = "redelivery"))

            val imported = tx.execute { persistence.findByShortcode(command().shortcode) }!!
            assertEquals(CrawlImportStatus.IMPORTED, imported.status)
            assertEquals(command().runId, imported.runId)
            assertEquals(1, courseCount())
            assertEquals(
                imported.courseId,
                jdbc.queryForObject("SELECT id FROM courses WHERE user_id = ?", Long::class.java, AUTHOR_ID),
            )
            assertEquals(
                listOf(8, null),
                jdbc.query("SELECT walking_minutes FROM course_places WHERE course_id = ? ORDER BY order_no", { rs, _ ->
                    rs.getObject("walking_minutes") as Int?
                }, imported.courseId),
            )
            assertEquals(1, mockingDetails(index).invocations.count { it.method.name == "save" })
            assertEquals(
                1,
                jdbc.queryForObject("SELECT public_courses_cnt FROM users WHERE id = ?", Int::class.java, AUTHOR_ID),
            )
        }

        private fun service(port: CrawlImportPersistencePort = persistence) =
            CrawlImportService(
                persistence = port,
                places =
                    object : CrawlPlacePort {
                        override fun resolve(places: List<CrawledPlaceCommand>) =
                            places.map {
                                ResolvedCrawlPlace(it.kakaoPlaceId, -69500L - it.order, Coordinate(it.lat, it.lng))
                            }
                    },
                walking =
                    object : CrawlWalkingPort {
                        override fun walkingSegments(points: List<Coordinate>) = listOf(8)
                    },
                courses = CrawlCourseAdapter(courseUseCase, CrawlerProperties(curatorUserId = AUTHOR_ID)),
                transactionOperations = tx,
            )

        private fun courseCount() =
            jdbc.queryForObject("SELECT count(*) FROM courses WHERE user_id = ?", Int::class.java, AUTHOR_ID)!!

        private fun command() =
            CrawlResultCommand(
                runId = "20261027-0400",
                shortcode = "scrum-695-atomic",
                sourceUrl = "https://www.instagram.com/p/scrum-695-atomic/",
                account = "eateat.mag",
                isCourse = true,
                title = "크롤링 코스",
                places =
                    (1..2).map {
                        CrawledPlaceCommand(
                            it,
                            "k$it",
                            "카페 $it",
                            "CAFE",
                            37.5,
                            127.0,
                            "서울",
                            imageUrls = listOf("https://cdn.example.com/$it.jpg"),
                        )
                    },
            )

        companion object {
            private const val AUTHOR_ID = -69500L
        }
    }
