package com.example.backend.bootstrap.event

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.common.geo.Coordinate
import com.example.backend.course.application.event.CourseAuthorWithdrawnEvent
import com.example.backend.course.application.event.CourseDeletedEvent
import com.example.backend.course.application.event.CourseSavedEvent
import com.example.backend.place.application.event.PlaceBusinessHoursSyncRequestedEvent
import com.example.backend.place.application.event.PlacesSavedEvent
import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.application.event.SavedPlaceIndexedEvent
import com.example.backend.user.application.event.SavedPlaceRemovedEvent
import com.example.backend.user.application.port.inbound.CourseCountIntakeUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.modulith.events.IncompleteEventPublications
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

/**
 * Modulith 이벤트 저장소(아웃박스) 통합 테스트(실제 PostgreSQL, Flyway V23).
 * 발행과 같은 트랜잭션에 리스너별 기록이 남고, 리스너가 끝나면 지워지며(completion-mode: delete),
 * 코스 개수 반영이 실패하면 기록이 남아 같은 eventId 로 재발행되는지 확인한다.
 * 코스 개수 진입점([CourseCountIntakeUseCase])만 목으로 바꿔 성공과 실패를 만든다.
 */
class EventPublicationOutboxIntegrationTest
    @Autowired
    constructor(
        private val eventPublisher: ApplicationEventPublisher,
        private val transactionTemplate: TransactionTemplate,
        private val jdbcTemplate: JdbcTemplate,
        private val incompleteEventPublications: IncompleteEventPublications,
        private val jsonMapper: JsonMapper,
    ) : IntegrationTestBase() {
        @MockitoBean
        private lateinit var courseCountIntakeUseCase: CourseCountIntakeUseCase

        private val eventIds = mutableListOf<String>()

        @AfterEach
        fun cleanUp() {
            eventIds.forEach {
                jdbcTemplate.update(
                    "DELETE FROM event_publication WHERE serialized_event LIKE ?",
                    "%$it%",
                )
            }
        }

        @Test
        fun `발행과 같은 트랜잭션에 리스너별 기록이 남고 비동기 리스너가 끝나면 지워진다`() {
            val event = courseSavedEvent()

            transactionTemplate.executeWithoutResult {
                eventPublisher.publishEvent(event)
                // 검색 색인 리스너와 코스 개수 리스너 몫 두 행이 업무 트랜잭션 안에서 INSERT 된다.
                assertThat(publications(event.eventId)).isEqualTo(2)
            }

            awaitUntil { publications(event.eventId) == 0 }
            verify(courseCountIntakeUseCase).receive(event.eventId, 1L, null, CourseVisibility.PUBLIC)
        }

        @Test
        fun `코스 개수 반영이 실패하면 기록이 미완료로 남고 재발행하면 같은 eventId 로 다시 반영한다`() {
            val event = courseSavedEvent()
            doThrow(IllegalStateException("동기 반영과 SQS 폴백 모두 실패"))
                .`when`(courseCountIntakeUseCase)
                .receive(anyString(), anyLong(), isNull(), any(CourseVisibility::class.java))

            transactionTemplate.executeWithoutResult { eventPublisher.publishEvent(event) }

            awaitUntil { failedPublications(event.eventId) == 1 }
            assertThat(publications(event.eventId)).isEqualTo(1) // 검색 색인 몫은 완료돼 지워지고 실패한 개수 몫만 남는다

            reset(courseCountIntakeUseCase)
            incompleteEventPublications.resubmitIncompletePublications { it.event == event }

            awaitUntil { publications(event.eventId) == 0 }
            verify(courseCountIntakeUseCase, times(1)).receive(event.eventId, 1L, null, CourseVisibility.PUBLIC)
        }

        @Test
        fun `저장소에 남는 커밋 후 이벤트는 모두 JSON 으로 저장하고 그대로 복원된다`() {
            // Modulith 의 JacksonEventSerializer 는 앱의 JsonMapper 빈으로 writeValueAsString / readerFor(type) 를 한다.
            val events =
                listOf(
                    CourseSavedEvent(1L, 2L, CourseVisibility.FOLLOWER, CourseVisibility.PUBLIC),
                    CourseDeletedEvent(courseId = 1L, authorId = 2L, oldVisibility = CourseVisibility.PRIVATE),
                    CourseAuthorWithdrawnEvent(2L),
                    PlacesSavedEvent(listOf(1L, 2L)),
                    PlaceBusinessHoursSyncRequestedEvent(listOf(3L)),
                    SavedPlaceIndexedEvent(
                        userId = 7L,
                        placeId = 42L,
                        location = Coordinate(37.51, 127.02),
                        savedAt = Instant.parse("2026-10-11T08:00:00Z"),
                        visited = true,
                        name = "성수 카페",
                        category = "CAFE",
                        imageUrl = null,
                        rating = 4.5,
                        ratingCount = 100,
                        categoryDetail = "카페",
                    ),
                    SavedPlaceRemovedEvent(7L, 42L),
                )

            events.forEach { event ->
                val serialized = jsonMapper.writeValueAsString(event)
                assertThat(jsonMapper.readerFor(event::class.java).readValue<Any>(serialized)).isEqualTo(event)
            }
        }

        private fun courseSavedEvent() =
            CourseSavedEvent(
                // 없는 코스 id: 검색 색인 리스너는 다시 읽어 아무것도 못 찾고 그냥 끝난다
                courseId = Long.MAX_VALUE,
                authorId = 1L,
                oldVisibility = null,
                newVisibility = CourseVisibility.PUBLIC,
            ).also { eventIds += it.eventId }

        private fun publications(eventId: String): Int =
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM event_publication WHERE serialized_event LIKE ?",
                Int::class.java,
                "%$eventId%",
            )!!

        private fun failedPublications(eventId: String): Int =
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM event_publication " +
                    "WHERE serialized_event LIKE ? AND status = 'FAILED' AND completion_date IS NULL",
                Int::class.java,
                "%$eventId%",
            )!!

        private fun awaitUntil(condition: () -> Boolean) {
            val deadline = System.nanoTime() + 10_000_000_000L
            while (!condition()) {
                check(System.nanoTime() < deadline) { "10초 안에 조건을 만족하지 못했다" }
                Thread.sleep(100)
            }
        }
    }
