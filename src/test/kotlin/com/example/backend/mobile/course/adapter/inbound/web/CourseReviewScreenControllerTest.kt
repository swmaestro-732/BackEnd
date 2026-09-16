package com.example.backend.mobile.course.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

/**
 * 코스 후기 전체보기 BFF(`GET /service/v1/courses/{courseId}/reviews`) 통합 테스트.
 * 리뷰·사진·태그·작성자를 심어 두고 정렬(sort×order)·커서 페이지·집계·작성자 병합(탈퇴자 대체 닉네임)을 HTTP 로 확인한다.
 * 픽스처(course-review-fixture)는 코스 701 하나만 두므로 리뷰는 테스트가 직접 심는다 — 1인 1리뷰라 리뷰마다 작성자가 다르다.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/course-review-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseReviewScreenControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jdbcTemplate: JdbcTemplate,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jsonMapper: JsonMapper,
    ) : IntegrationTestBase() {
        @Test
        fun `기본은 최신순이고 집계·사진·태그·작성자가 함께 내려간다`() {
            seedReviews()

            mockMvc
                .perform(get("/service/v1/courses/$COURSE_ID/reviews"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.totalCount").value(3)) // 소프트 삭제(4)는 제외
                .andExpect(jsonPath("$.data.averageRating").value(4.3)) // 13/3 을 소수 첫째 자리로 반올림
                .andExpect(jsonPath("$.data.ratingDistribution[?(@.rating==5)].count").value(2))
                .andExpect(jsonPath("$.data.ratingDistribution[?(@.rating==3)].count").value(1))
                .andExpect(jsonPath("$.data.photoCount").value(2))
                .andExpect(jsonPath("$.data.hasCompletedCourse").value(false))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.reviews.length()").value(3))
                .andExpect(jsonPath("$.data.reviews[0].id").value(3))
                .andExpect(jsonPath("$.data.reviews[1].id").value(2))
                .andExpect(jsonPath("$.data.reviews[2].id").value(1))
                // 작성자 없는(탈퇴) 리뷰는 대체 닉네임, 있는 리뷰는 프로필 병합
                .andExpect(jsonPath("$.data.reviews[0].author.nickname").value("알 수 없음"))
                .andExpect(jsonPath("$.data.reviews[2].author.id").value(AUTHOR_A))
                .andExpect(jsonPath("$.data.reviews[2].author.nickname").value("후기작성자A"))
                .andExpect(jsonPath("$.data.reviews[1].content").isEmpty)
                .andExpect(jsonPath("$.data.reviews[2].photoUrls.length()").value(2))
                .andExpect(jsonPath("$.data.reviews[2].photoUrls[0]").value("https://cdn/p1.jpg"))
                .andExpect(jsonPath("$.data.reviews[2].photoUrls[1]").value("https://cdn/p2.jpg"))
                .andExpect(jsonPath("$.data.reviews[2].tags.length()").value(2))
                .andExpect(jsonPath("$.data.reviews[2].tags[0].code").value("packed"))
                .andExpect(jsonPath("$.data.reviews[2].tags[1].code").value("smooth"))
        }

        @Test
        fun `최신순 내림차순 커서로 다음 페이지를 이어 조회한다`() {
            seedReviews()

            val first = fetch("sort=LATEST&order=DESC&size=2")
            assertIds(first, listOf(3, 2))
            val cursor = first.nextCursor()

            val second = fetch("sort=LATEST&order=DESC&size=2&cursor=$cursor")
            assertIds(second, listOf(1))
            assertNoNext(second)
        }

        @Test
        fun `최신순 오름차순도 커서로 이어진다`() {
            seedReviews()

            val first = fetch("sort=LATEST&order=ASC&size=2")
            assertIds(first, listOf(1, 2))

            val second = fetch("sort=LATEST&order=ASC&size=2&cursor=${first.nextCursor()}")
            assertIds(second, listOf(3))
            assertNoNext(second)
        }

        @Test
        fun `평점순은 동점을 작성일로 가르고 커서로 이어진다`() {
            seedReviews()

            // 내림차순: 5점(3 최신 → 1) 다음 3점(2)
            val descFirst = fetch("sort=RATING&order=DESC&size=2")
            assertIds(descFirst, listOf(3, 1))
            assertIds(fetch("sort=RATING&order=DESC&size=2&cursor=${descFirst.nextCursor()}"), listOf(2))

            // 오름차순: 3점(2) 다음 5점(1 오래된 → 3)
            val ascFirst = fetch("sort=RATING&order=ASC&size=2")
            assertIds(ascFirst, listOf(2, 1))
            assertIds(fetch("sort=RATING&order=ASC&size=2&cursor=${ascFirst.nextCursor()}"), listOf(3))
        }

        @Test
        fun `로그인 조회자도 완주 여부는 스텁 false 다`() {
            seedReviews()

            mockMvc
                .perform(
                    get("/service/v1/courses/$COURSE_ID/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(AUTHOR_A)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.hasCompletedCourse").value(false))
        }

        @Test
        fun `리뷰가 없으면 빈 페이지와 0 집계를 내려준다`() {
            mockMvc
                .perform(get("/service/v1/courses/$COURSE_ID/reviews"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.photoCount").value(0))
                .andExpect(jsonPath("$.data.reviews.length()").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(false))
        }

        @Test
        fun `잘못된 커서는 400 이다`() {
            mockMvc
                .perform(get("/service/v1/courses/$COURSE_ID/reviews?cursor=not-a-cursor"))
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `mock=true 면 DB 와 무관하게 고정 목을 내려준다`() {
            mockMvc
                .perform(get("/service/v1/courses/999999/reviews?mock=true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.reviews.length()").value(org.hamcrest.Matchers.greaterThan(0)))
        }

        // ── 픽스처 ──

        /**
         * 코스 701 에 리뷰 3건(+소프트 삭제 1건). 작성일·평점을 서로 다르게 두어 정렬이 결정적이다.
         *  id1 작성자A 5점 2026-01-01 사진2·태그2 / id2 작성자B 3점 01-02 내용 없음 / id3 미존재 작성자 5점 01-03 / id4 삭제됨
         */
        private fun seedReviews() {
            jdbcTemplate.update(
                "INSERT INTO users (id, nickname) VALUES ($AUTHOR_A, '후기작성자A'), ($AUTHOR_B, '후기작성자B') ON CONFLICT DO NOTHING",
            )
            jdbcTemplate.update(
                """
                INSERT INTO course_reviews (id, course_id, user_id, rating, content, status, created_at, updated_at, deleted_at) VALUES
                  (1, $COURSE_ID, $AUTHOR_A, 5, '최고예요', 'PUBLISHED', '2026-01-01T10:00:00Z', '2026-01-01T10:00:00Z', NULL),
                  (2, $COURSE_ID, $AUTHOR_B, 3, NULL,      'PUBLISHED', '2026-01-02T10:00:00Z', '2026-01-02T10:00:00Z', NULL),
                  (3, $COURSE_ID, $MISSING_AUTHOR, 5, '뷰 좋음', 'PUBLISHED', '2026-01-03T10:00:00Z', '2026-01-03T10:00:00Z', NULL),
                  (4, $COURSE_ID, 9999, 1, '삭제됨', 'DELETED', '2026-01-04T10:00:00Z', '2026-01-04T10:00:00Z', '2026-01-05T10:00:00Z')
                """.trimIndent(),
            )
            jdbcTemplate.update(
                "INSERT INTO course_review_photos (course_review_id, image_url, order_no) VALUES (1, 'https://cdn/p1.jpg', 0), (1, 'https://cdn/p2.jpg', 1)",
            )
            jdbcTemplate.update(
                "INSERT INTO course_review_tag_links (course_review_id, tag) VALUES (1, 'PACKED'), (1, 'SMOOTH')",
            )
            // totalCount·averageRating 은 행 집계가 아니라 비정규화 카운터에서 읽는다 — 쓰기 경로가 유지하는 값을 그대로 맞춰 둔다.
            // 5+3+5 / 3건(삭제 제외)
            jdbcTemplate.update("UPDATE courses SET rating_sum = 13, rating_cnt = 3 WHERE id = $COURSE_ID")
        }

        private fun fetch(query: String): Map<String, Any?> {
            val body =
                mockMvc
                    .perform(get("/service/v1/courses/$COURSE_ID/reviews?$query"))
                    .andExpect(status().isOk)
                    .andReturn()
                    .response
                    .contentAsString
            @Suppress("UNCHECKED_CAST")
            return jsonMapper.readValue(body, Map::class.java)["data"] as Map<String, Any?>
        }

        private fun Map<String, Any?>.nextCursor(): String =
            requireNotNull(this["nextCursor"] as String?) {
                "nextCursor 가 있어야 한다"
            }

        private fun assertIds(
            page: Map<String, Any?>,
            expected: List<Int>,
        ) {
            @Suppress("UNCHECKED_CAST")
            val ids = (page["reviews"] as List<Map<String, Any?>>).map { (it["id"] as Number).toInt() }
            org.junit.jupiter.api.Assertions
                .assertEquals(expected, ids)
        }

        private fun assertNoNext(page: Map<String, Any?>) {
            org.junit.jupiter.api.Assertions
                .assertEquals(false, page["hasNext"])
            org.junit.jupiter.api.Assertions
                .assertNull(page["nextCursor"])
        }

        private companion object {
            const val COURSE_ID = 701L
            const val AUTHOR_A = 9001L
            const val AUTHOR_B = 9002L
            const val MISSING_AUTHOR = 9003L
        }
    }
