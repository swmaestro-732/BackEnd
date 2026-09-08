package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.domain.model.SocialProvider
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 코스 리뷰 작성·삭제(`/api/v1/courses/{courseId}/reviews`) 통합 테스트.
 * 픽스처(course-review-fixture.sql)는 코스 701 하나만 두고 리뷰 테이블을 비운다.
 *
 * 저장 결과(리뷰 본문·사진 순서·태그 코드)와 별점 카운터(courses.rating_sum/rating_cnt) 증감은 DB 를 직접 읽어 확인하고,
 * 입력 검증은 웹 DTO(4002 fieldErrors)와 도메인 불변식(4001)이 각각 어디서 걸리는지까지 본다.
 * 삭제는 소프트 삭제와 404 은닉(없음·타인·이미 삭제 동일 4046)을 본다.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/course-review-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseReviewControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jdbcTemplate: JdbcTemplate,
    ) : IntegrationTestBase() {
        @Test
        fun `리뷰를 쓰면 201과 생성된 reviewId 를 내려주고 본문·사진·태그가 저장된다`() {
            mockMvc
                .perform(
                    createReviewRequest(
                        COURSE_ID,
                        """
                        {
                          "rating": 5,
                          "content": "동선이 자연스러워요",
                          "photoUrls": ["https://cdn.example.com/1.jpg", "https://cdn.example.com/2.jpg"],
                          "tagCodes": ["packed", "smooth"]
                        }
                        """.trimIndent(),
                    ),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.reviewId").value(1))

            val review =
                jdbcTemplate.queryForMap(
                    "SELECT course_id, user_id, status, rating, content FROM course_reviews WHERE id = 1",
                )
            assertEquals(COURSE_ID, review["course_id"])
            assertEquals(USER_ID, review["user_id"])
            assertEquals("PUBLISHED", review["status"])
            assertEquals(5, review["rating"]) // smallint 은 JDBC 가 Integer 로 읽는다
            assertEquals("동선이 자연스러워요", review["content"])

            // 사진은 목록 순서가 곧 노출 순서(order_no)다.
            assertEquals(
                listOf("https://cdn.example.com/1.jpg", "https://cdn.example.com/2.jpg"),
                jdbcTemplate.queryForList(
                    "SELECT image_url FROM course_review_photos WHERE course_review_id = 1 ORDER BY order_no",
                    String::class.java,
                ),
            )
            // 태그는 마스터 테이블 없이 enum 이름으로 저장된다(V5).
            assertEquals(
                listOf("PACKED", "SMOOTH"),
                jdbcTemplate.queryForList(
                    "SELECT tag FROM course_review_tag_links WHERE course_review_id = 1 ORDER BY tag",
                    String::class.java,
                ),
            )
            // 별점 카운터는 리뷰 쓰기와 같은 트랜잭션에서 상대 갱신된다.
            assertEquals(5L to 1, ratingCounters())
        }

        @Test
        fun `별점만 남겨도 201이고 한마디·사진·태그는 비어 있다`() {
            mockMvc
                .perform(createReviewRequest(COURSE_ID, """{"rating":4}"""))
                .andExpect(status().isCreated)

            assertEquals(
                null,
                jdbcTemplate.queryForObject("SELECT content FROM course_reviews WHERE id = 1", String::class.java),
            )
            assertEquals(0, countRows("course_review_photos"))
            assertEquals(0, countRows("course_review_tag_links"))
        }

        @Test
        fun `같은 사용자가 같은 코스에 또 써도 막지 않고 카운터가 누적된다`() {
            // 다시 따라갈 때마다 남길 수 있어야 한다 — 스키마에도 유니크 제약이 없다.
            mockMvc.perform(createReviewRequest(COURSE_ID, """{"rating":5}""")).andExpect(status().isCreated)
            mockMvc.perform(createReviewRequest(COURSE_ID, """{"rating":3}""")).andExpect(status().isCreated)

            assertEquals(2, countRows("course_reviews"))
            assertEquals(8L to 2, ratingCounters())
        }

        @Test
        fun `없는 코스에 쓰면 4041을 내려준다`() {
            mockMvc
                .perform(createReviewRequest(999999L, """{"rating":4}"""))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))

            assertEquals(0, countRows("course_reviews"))
        }

        @Test
        fun `모르는 태그 코드는 4001이고 리뷰를 남기지 않는다`() {
            mockMvc
                .perform(createReviewRequest(COURSE_ID, """{"rating":4,"tagCodes":["nosuchtag"]}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))

            assertEquals(0, countRows("course_reviews"))
        }

        @Test
        fun `별점이 범위를 벗어나면 4002를 내려준다`() {
            mockMvc
                .perform(createReviewRequest(COURSE_ID, """{"rating":6}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("rating"))

            assertEquals(0, countRows("course_reviews"))
        }

        @Test
        fun `사진이 6장을 넘으면 4002를 내려준다`() {
            val photoUrls = (1..7).joinToString(",") { "\"https://cdn.example.com/$it.jpg\"" }

            mockMvc
                .perform(createReviewRequest(COURSE_ID, """{"rating":4,"photoUrls":[$photoUrls]}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("photoUrls"))
        }

        @Test
        fun `태그가 상한을 넘으면 4002를 내려준다`() {
            val tagCodes = CourseReviewTagCodes.take(6).joinToString(",") { "\"$it\"" }

            mockMvc
                .perform(createReviewRequest(COURSE_ID, """{"rating":4,"tagCodes":[$tagCodes]}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("tagCodes"))
        }

        @Test
        fun `한마디가 1000자를 넘으면 도메인 불변식이 4001로 막는다`() {
            // 요청 DTO 에는 길이 제한이 없어 도메인(CourseReview.create)에서 걸린다 — 필드 단위 4002 가 아니다.
            mockMvc
                .perform(createReviewRequest(COURSE_ID, """{"rating":4,"content":"${"가".repeat(1001)}"}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))

            assertEquals(0, countRows("course_reviews"))
        }

        @Test
        fun `토큰이 없으면 401을 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/courses/$COURSE_ID/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"rating":4}"""),
                ).andExpect(status().isUnauthorized)
        }

        /**
         * 회원가입 토큰에는 subject(userId)가 없어 `@CurrentUserId` 해석에서 먼저 막힌다 —
         * 인자 해석이 메서드 시큐리티(`@AccessTokenRequired`)보다 앞서므로 403 이 아니라 401 이다
         * (장소 리뷰 [com.example.backend.place.adapter.inbound.web.PlaceReviewControllerTest] 와 같은 규칙).
         */
        @Test
        fun `회원가입 토큰으로는 리뷰를 쓸 수 없다`() {
            val registrationToken = jwtTokenProvider.issueRegistrationToken(SocialProvider.KAKAO, "social-1")

            mockMvc
                .perform(
                    post("/api/v1/courses/$COURSE_ID/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $registrationToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"rating":4}"""),
                ).andExpect(status().isUnauthorized)

            assertEquals(0, countRows("course_reviews"))
        }

        @Test
        fun `mock=true 면 DB 저장 없이 목 reviewId 를 내려준다`() {
            mockMvc
                .perform(createReviewRequest(COURSE_ID, """{"rating":4}""").param("mock", "true"))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.reviewId").value(7))

            assertEquals(0, countRows("course_reviews"))
        }

        @Test
        fun `내 리뷰를 지우면 소프트 삭제되고 카운터가 줄어든다`() {
            mockMvc.perform(createReviewRequest(COURSE_ID, """{"rating":5}""")).andExpect(status().isCreated)

            mockMvc
                .perform(deleteReviewRequest(reviewId = 1, token = accessToken(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))

            val review = jdbcTemplate.queryForMap("SELECT status, deleted_at FROM course_reviews WHERE id = 1")
            assertEquals("DELETED", review["status"])
            assertEquals(true, review["deleted_at"] != null)
            assertEquals(0L to 0, ratingCounters()) // 작성 +5/+1 이 삭제로 되돌아간다
        }

        @Test
        fun `없는 리뷰를 지우면 4046을 내려준다`() {
            mockMvc
                .perform(deleteReviewRequest(reviewId = 999999, token = accessToken(USER_ID)))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4046))
        }

        @Test
        fun `타인 리뷰는 존재를 드러내지 않고 같은 4046 으로 은닉한다`() {
            mockMvc.perform(createReviewRequest(COURSE_ID, """{"rating":5}""")).andExpect(status().isCreated)

            mockMvc
                .perform(deleteReviewRequest(reviewId = 1, token = accessToken(OTHER_USER_ID)))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4046))

            // 리뷰는 살아 있고 카운터도 그대로다.
            assertEquals(
                "PUBLISHED",
                jdbcTemplate.queryForObject("SELECT status FROM course_reviews WHERE id = 1", String::class.java),
            )
            assertEquals(5L to 1, ratingCounters())
        }

        @Test
        fun `이미 지운 리뷰를 또 지우면 4046 이고 카운터가 두 번 줄지 않는다`() {
            mockMvc.perform(createReviewRequest(COURSE_ID, """{"rating":5}""")).andExpect(status().isCreated)
            mockMvc.perform(deleteReviewRequest(reviewId = 1, token = accessToken(USER_ID))).andExpect(status().isOk)

            mockMvc
                .perform(deleteReviewRequest(reviewId = 1, token = accessToken(USER_ID)))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4046))

            assertEquals(0L to 0, ratingCounters())
        }

        @Test
        fun `토큰 없이 지우면 401을 내려준다`() {
            mockMvc
                .perform(delete("/api/v1/courses/$COURSE_ID/reviews/1"))
                .andExpect(status().isUnauthorized)
        }

        @Test
        fun `mock=true 삭제는 DB 를 건드리지 않는다`() {
            mockMvc.perform(createReviewRequest(COURSE_ID, """{"rating":5}""")).andExpect(status().isCreated)

            mockMvc
                .perform(deleteReviewRequest(reviewId = 1, token = accessToken(USER_ID)).param("mock", "true"))
                .andExpect(status().isOk)

            assertEquals(
                "PUBLISHED",
                jdbcTemplate.queryForObject("SELECT status FROM course_reviews WHERE id = 1", String::class.java),
            )
        }

        private fun createReviewRequest(
            courseId: Long,
            body: String,
        ) = post("/api/v1/courses/$courseId/reviews")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${accessToken(USER_ID)}")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)

        private fun deleteReviewRequest(
            reviewId: Long,
            token: String,
        ) = delete("/api/v1/courses/$COURSE_ID/reviews/$reviewId")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")

        private fun accessToken(userId: Long) = jwtTokenProvider.issueAccessToken(userId)

        private fun countRows(table: String): Int =
            jdbcTemplate.queryForObject("SELECT count(*) FROM $table", Int::class.java) ?: 0

        /** 코스 701 의 별점 카운터(rating_sum, rating_cnt). */
        private fun ratingCounters(): Pair<Long, Int> {
            val row = jdbcTemplate.queryForMap("SELECT rating_sum, rating_cnt FROM courses WHERE id = $COURSE_ID")
            return (row["rating_sum"] as Number).toLong() to (row["rating_cnt"] as Number).toInt()
        }

        private companion object {
            const val COURSE_ID = 701L
            const val USER_ID = 1L
            const val OTHER_USER_ID = 2L

            /** 요청 DTO 의 태그 개수 상한을 넘기려고 쓰는 유효한 코드들(`.ai/taxonomy.md` 코스 구성·이동 태그). */
            val CourseReviewTagCodes = listOf("packed", "combo", "smooth", "efficient", "walkable", "transit")
        }
    }
