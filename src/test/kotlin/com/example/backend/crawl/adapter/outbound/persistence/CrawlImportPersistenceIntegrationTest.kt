package com.example.backend.crawl.adapter.outbound.persistence

import com.example.backend.bootstrap.config.CrawlerProperties
import com.example.backend.common.geo.Coordinate
import com.example.backend.course.application.port.inbound.CourseUseCase
import com.example.backend.course.application.port.inbound.dto.CreateCourseCommand
import com.example.backend.course.application.port.outbound.CourseSearchIndexPort
import com.example.backend.crawl.adapter.outbound.course.CrawlCourseAdapter
import com.example.backend.crawl.adapter.outbound.place.CrawlPlaceAdapter
import com.example.backend.crawl.application.port.inbound.dto.CrawlResultCommand
import com.example.backend.crawl.application.port.inbound.dto.CrawledPlaceCommand
import com.example.backend.crawl.application.port.outbound.CrawlImportPersistencePort
import com.example.backend.crawl.application.port.outbound.CrawlPlacePort
import com.example.backend.crawl.application.port.outbound.CrawlWalkingPort
import com.example.backend.crawl.application.port.outbound.ResolvedCrawlPlace
import com.example.backend.crawl.application.service.CrawlImportService
import com.example.backend.crawl.domain.model.CrawlImport
import com.example.backend.crawl.domain.model.CrawlImportStatus
import com.example.backend.place.application.port.inbound.PlaceBusinessHoursSyncUseCase
import com.example.backend.place.application.port.inbound.PlaceRegistrationUseCase
import com.example.backend.place.application.port.outbound.PlaceSearchIndexPort
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@TestPropertySource(properties = ["aws.sqs.crawl-results-queue-url=", "aws.sqs.course-count-queue-url="])
class CrawlImportPersistenceIntegrationTest
    @Autowired
    constructor(
        private val persistence: CrawlImportPersistencePort,
        private val courseUseCase: CourseUseCase,
        private val jdbc: JdbcTemplate,
        private val registration: PlaceRegistrationUseCase,
        private val transactionManager: PlatformTransactionManager,
    ) : IntegrationTestBase() {
        private val tx = TransactionTemplate(transactionManager)

        @MockitoBean
        private lateinit var index: CourseSearchIndexPort

        @MockitoBean
        private lateinit var placeIndex: PlaceSearchIndexPort

        @MockitoBean
        private lateinit var businessHours: PlaceBusinessHoursSyncUseCase

        @BeforeEach
        fun seed() {
            tx.executeWithoutResult {
                jdbc.update("INSERT INTO users (id, nickname) VALUES (?, '크롤러 검증')", AUTHOR_ID)
                listOf(-69501L, -69502L).forEach { id ->
                    jdbc.update(
                        "INSERT INTO places (id, name, category, location, address, kakao_place_id, image_url) " +
                            "VALUES (?, '크롤링 장소', 'CAFE', ST_GeogFromText('SRID=4326;POINT(127 37.5)'), '서울', ?, ?)",
                        id,
                        "scrum-695-k${-69500L - id}",
                        "https://cdn.example.com/existing${-69500L - id}.jpg",
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
                persistence.claim(command.shortcode)
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
        fun `같은 게시물의 두 번째 claim은 첫 트랜잭션의 최종 결과를 기다린다`() {
            val shortcode = "scrum-695-concurrent"
            val completed = CrawlImport(shortcode, "first-run", CrawlImportStatus.REJECTED, reason = "not course")
            val claimed = CountDownLatch(1)
            val release = CountDownLatch(1)
            val contenderStarted = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(2)
            try {
                val first =
                    executor.submit {
                        tx.executeWithoutResult {
                            assertNull(persistence.claim(shortcode))
                            claimed.countDown()
                            assertTrue(release.await(5, TimeUnit.SECONDS))
                            persistence.save(completed)
                        }
                    }
                assertTrue(claimed.await(5, TimeUnit.SECONDS))
                assertNull(tx.execute { persistence.findByShortcode(shortcode) })
                val second =
                    executor.submit<CrawlImport?> {
                        tx.execute {
                            jdbc.execute("SET LOCAL lock_timeout = '5s'")
                            contenderStarted.countDown()
                            persistence.claim(shortcode)
                        }
                    }
                assertTrue(contenderStarted.await(5, TimeUnit.SECONDS))
                assertThrows(TimeoutException::class.java) { second.get(200, TimeUnit.MILLISECONDS) }

                release.countDown()
                first.get(5, TimeUnit.SECONDS)

                assertEquals(completed, second.get(5, TimeUnit.SECONDS))
                assertEquals(completed, tx.execute { persistence.findByShortcode(shortcode) })
            } finally {
                release.countDown()
                executor.shutdownNow()
                assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
            }
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

        @Test
        fun `코스 도메인의 INVALID_INPUT은 롤백 후 별도 트랜잭션에 거절 이력을 커밋한다`() {
            val invalidCourse =
                object : CourseUseCase by courseUseCase {
                    override fun create(command: CreateCourseCommand) =
                        courseUseCase.create(command).let {
                            // 저장·이벤트 발행 후 실제 REQUIRED 경계에서 도메인 예외를 일으킨다.
                            assertEquals(1, courseCount())
                            courseUseCase.create(command.copy(title = ""))
                        }
                }

            assertDoesNotThrow {
                tx.executeWithoutResult { status ->
                    service(course = invalidCourse).importResult(command())
                    status.setRollbackOnly()
                }
            }

            assertEquals(0, courseCount())
            assertEquals(
                CrawlImport(
                    command().shortcode,
                    command().runId,
                    CrawlImportStatus.REJECTED,
                    reason = "invalid: 코스를 발행하려면 제목이 필요합니다.",
                ),
                tx.execute { persistence.findByShortcode(command().shortcode) },
            )
            verifyNoInteractions(index)
            assertEquals(
                0,
                jdbc.queryForObject("SELECT public_courses_cnt FROM users WHERE id = ?", Int::class.java, AUTHOR_ID),
            )

            service().importResult(command().copy(runId = "redelivery"))

            assertEquals(0, courseCount())
            assertEquals(command().runId, tx.execute { persistence.findByShortcode(command().shortcode) }?.runId)
        }

        @Test
        fun `등록할 수 없는 장소 카테고리는 실제 등록 경로에서 거절 이력으로 남긴다`() {
            val invalid =
                command().copy(
                    places = command().places.map { it.copy(category = "UNKNOWN") },
                )

            assertDoesNotThrow { service(placePort = CrawlPlaceAdapter(registration)).importResult(invalid) }

            assertEquals(
                CrawlImport(
                    invalid.shortcode,
                    invalid.runId,
                    CrawlImportStatus.REJECTED,
                    reason = "invalid: unknown category: UNKNOWN",
                ),
                tx.execute { persistence.findByShortcode(invalid.shortcode) },
            )
            assertEquals(0, courseCount())
            verifyNoInteractions(index, placeIndex, businessHours)
        }

        @Test
        fun `크롤러가 생략한 이미지는 실제 기존 장소 사진으로 보완해 발행한다`() {
            val noImages = command().copy(places = command().places.map { it.copy(imageUrls = emptyList()) })

            service(placePort = CrawlPlaceAdapter(registration)).importResult(noImages)

            assertEquals(
                CrawlImportStatus.IMPORTED,
                tx.execute { persistence.findByShortcode(noImages.shortcode) }?.status,
            )
            assertEquals(1, courseCount())
            assertEquals(
                "https://cdn.example.com/existing1.jpg",
                jdbc.queryForObject(
                    "SELECT cover_image_url FROM courses WHERE user_id = ?",
                    String::class.java,
                    AUTHOR_ID,
                ),
            )
            assertEquals(
                listOf("https://cdn.example.com/existing1.jpg", "https://cdn.example.com/existing2.jpg"),
                jdbc.queryForList(
                    "SELECT i.image_url FROM course_place_images i JOIN course_places p ON p.id = i.course_place_id " +
                        "JOIN courses c ON c.id = p.course_id WHERE c.user_id = ? ORDER BY p.order_no",
                    String::class.java,
                    AUTHOR_ID,
                ),
            )
        }

        private fun service(
            port: CrawlImportPersistencePort = persistence,
            course: CourseUseCase = courseUseCase,
            placePort: CrawlPlacePort =
                object : CrawlPlacePort {
                    override fun resolve(places: List<CrawledPlaceCommand>) =
                        places.map {
                            ResolvedCrawlPlace(it.kakaoPlaceId, -69500L - it.order, Coordinate(it.lat, it.lng))
                        }
                },
        ) = CrawlImportService(
            persistence = port,
            places = placePort,
            walking =
                object : CrawlWalkingPort {
                    override fun walkingSegments(points: List<Coordinate>) = listOf(8)
                },
            courses = CrawlCourseAdapter(course, CrawlerProperties(curatorUserId = AUTHOR_ID)),
            transactionManager = transactionManager,
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
                            "scrum-695-k$it",
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
