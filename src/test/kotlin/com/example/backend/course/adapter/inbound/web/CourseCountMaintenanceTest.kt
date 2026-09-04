package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.course.application.event.CourseCountDeltaEvent
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 작성자의 공개범위별 코스 개수 캐시 유지 검증.
 *
 * 카운트 반영은 이제 동기 호출이 아니라 [CourseCountDeltaEvent] 발행 → SQS 컨슈머(멱등)가 처리하는
 * 결과적 일관성 구조다(SCRUM-523). 실제 카운터 반영·멱등은 CourseCountMessageHandlerTest 가 단위로 검증하고,
 * 여기서는 웹 → 서비스 경로에서 공개범위·발행 전이가 **올바른 델타 이벤트**로 나오는지(±1 산출 로직)를 확인한다.
 * 픽스처(course-crud-fixture)는 소유자(1)를 public=1(course 1 PUBLIC·발행 반영)로 심는다.
 */
@AutoConfigureMockMvc
@RecordApplicationEvents
@Sql(scripts = ["/sql/course-crud-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseCountMaintenanceTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @Autowired
        private lateinit var events: ApplicationEvents

        @Test
        fun `발행 PUBLIC 코스를 생성하면 public 델타 +1 이벤트가 발행된다`() {
            createCourse(visibility = "PUBLIC", published = true)

            assertSingleDelta(public = 1, follower = 0, private = 0)
        }

        @Test
        fun `임시저장 코스를 생성하면 카운트 델타 이벤트가 없다`() {
            createCourse(visibility = "PRIVATE", published = false)

            assertEquals(0, events.stream(CourseCountDeltaEvent::class.java).count(), "델타 이벤트 없음")
        }

        @Test
        fun `임시저장 초안을 FOLLOWER 로 발행 편집하면 follower 델타 +1 이벤트가 발행된다`() {
            // course 2 = PRIVATE 임시저장(카운트 미포함) → 발행하면 발행 상태로 들어와 follower +1(초안이라 removed 없음).
            val body =
                """
                {
                  "title": "발행으로 전환",
                  "thumbnailUrl": "https://img/publish-cover.jpg",
                  "visibility": "FOLLOWER",
                  "isPublished": true,
                  "places": [
                    {"placeId": 1, "orderNo": 0, "caption": "카페A", "imageUrls": ["https://img/a.jpg"]},
                    {"placeId": 2, "orderNo": 1, "caption": "카페B", "imageUrls": ["https://img/b.jpg"]}
                  ]
                }
                """.trimIndent()
            mockMvc
                .perform(
                    patch("/api/v1/courses/$PRIVATE_DRAFT_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isOk)

            assertSingleDelta(public = 0, follower = 1, private = 0)
        }

        @Test
        fun `발행 PUBLIC 코스를 삭제하면 public 델타 -1 이벤트가 발행된다`() {
            mockMvc
                .perform(
                    delete("/api/v1/courses/$PUBLIC_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isOk)

            assertSingleDelta(public = -1, follower = 0, private = 0)
        }

        private fun createCourse(
            visibility: String,
            published: Boolean,
        ) {
            // 장소 2곳 이상은 발행·임시저장 공통 규칙이라 임시저장도 빈 배열로는 생성할 수 없다.
            val places =
                """
                "places": [
                  {"placeId": 1, "orderNo": 0, "caption": "카페A", "imageUrls": ["https://img/a.jpg"]},
                  {"placeId": 2, "orderNo": 1, "caption": "카페B", "imageUrls": ["https://img/b.jpg"]}
                ]
                """.trimIndent()
            // 발행 코스는 커버(thumbnailUrl)가 필수라 항상 넣는다(임시저장도 있어도 무방).
            val body =
                """
                {"title": "카운트 테스트", "thumbnailUrl": "https://img/cover.jpg", "visibility": "$visibility", "isPublished": $published, $places}
                """.trimIndent()
            mockMvc
                .perform(
                    post("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isCreated)
        }

        /** 카운트 델타 이벤트가 정확히 하나 발행됐고 버킷별 델타가 기대와 같은지 검증한다. */
        private fun assertSingleDelta(
            public: Int,
            follower: Int,
            private: Int,
        ) {
            val event = events.stream(CourseCountDeltaEvent::class.java).toList().single()
            assertEquals(public, event.publicDelta, "publicDelta")
            assertEquals(follower, event.followerDelta, "followerDelta")
            assertEquals(private, event.privateDelta, "privateDelta")
        }

        private fun tokenFor(userId: Long) = jwtTokenProvider.issueAccessToken(userId)

        private companion object {
            const val OWNER_ID = 1L
            const val PUBLIC_COURSE_ID = 1L
            const val PRIVATE_DRAFT_ID = 2L
        }
    }
