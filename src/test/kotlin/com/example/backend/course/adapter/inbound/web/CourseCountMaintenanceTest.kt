package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 작성자의 공개범위별 코스 개수 캐시(users.public/follower/private_courses_cnt) 유지 검증.
 * 코스 발행 생성/편집(공개범위·발행 전이)/삭제가 매 조회 GROUP BY 대신 이 저장 카운터를 ±1 로 정확히 움직이는지 확인한다.
 * 픽스처(course-crud-fixture)는 소유자(1)를 public=1(course 1 PUBLIC·발행 반영)로 심는다.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/course-crud-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseCountMaintenanceTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jdbcTemplate: JdbcTemplate,
    ) : IntegrationTestBase() {
        @Test
        fun `발행 PUBLIC 코스를 생성하면 작성자 public 카운트가 +1 된다`() {
            createCourse(visibility = "PUBLIC", published = true, withPlaces = true)

            assertCounts(public = 2, follower = 0, private = 0)
        }

        @Test
        fun `임시저장 코스를 생성하면 카운트는 그대로다`() {
            createCourse(visibility = "PRIVATE", published = false, withPlaces = false)

            assertCounts(public = 1, follower = 0, private = 0)
        }

        @Test
        fun `임시저장 초안을 FOLLOWER 로 발행 편집하면 follower 카운트가 +1 된다`() {
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

            assertCounts(public = 1, follower = 1, private = 0)
        }

        @Test
        fun `발행 PUBLIC 코스를 삭제하면 작성자 public 카운트가 -1 된다`() {
            mockMvc
                .perform(
                    delete("/api/v1/courses/$PUBLIC_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isOk)

            assertCounts(public = 0, follower = 0, private = 0)
        }

        private fun createCourse(
            visibility: String,
            published: Boolean,
            withPlaces: Boolean,
        ) {
            val places =
                if (withPlaces) {
                    """
                    ,
                    "places": [
                      {"placeId": 1, "orderNo": 0, "caption": "카페A", "imageUrls": ["https://img/a.jpg"]},
                      {"placeId": 2, "orderNo": 1, "caption": "카페B", "imageUrls": ["https://img/b.jpg"]}
                    ]
                    """.trimIndent()
                } else {
                    ""","places": []"""
                }
            // 발행 코스는 커버(thumbnailUrl)가 필수라 항상 넣는다(임시저장도 있어도 무방).
            val body =
                """
                {"title": "카운트 테스트", "thumbnailUrl": "https://img/cover.jpg", "visibility": "$visibility", "isPublished": $published$places}
                """.trimIndent()
            mockMvc
                .perform(
                    post("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isCreated)
        }

        private fun assertCounts(
            public: Int,
            follower: Int,
            private: Int,
        ) {
            val row =
                jdbcTemplate.queryForMap(
                    "SELECT public_courses_cnt, follower_courses_cnt, private_courses_cnt FROM users WHERE id = ?",
                    OWNER_ID,
                )
            assertEquals(public, (row["public_courses_cnt"] as Number).toInt(), "public_courses_cnt")
            assertEquals(follower, (row["follower_courses_cnt"] as Number).toInt(), "follower_courses_cnt")
            assertEquals(private, (row["private_courses_cnt"] as Number).toInt(), "private_courses_cnt")
        }

        private fun tokenFor(userId: Long) = jwtTokenProvider.issueAccessToken(userId)

        private companion object {
            const val OWNER_ID = 1L
            const val PUBLIC_COURSE_ID = 1L
            const val PRIVATE_DRAFT_ID = 2L
        }
    }
