package com.example.backend.place.adapter.inbound.web

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 장소 리뷰 작성(`POST /api/v1/places/{placeId}/reviews`) 통합 테스트.
 * 픽스처(place-review-fixture.sql)는 장소 601 하나만 두고 리뷰 테이블을 비운다.
 *
 * 저장 결과(리뷰 본문·사진 순서·태그 코드)는 DB 를 직접 읽어 확인하고,
 * 입력 검증은 웹 DTO(4002 fieldErrors)와 도메인 불변식(4001)이 각각 어디서 걸리는지까지 본다.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/place-review-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PlaceReviewControllerTest
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
                        PLACE_ID,
                        """
                        {
                          "rating": 5,
                          "content": "통창 뷰가 좋아요",
                          "photoUrls": ["https://cdn.example.com/1.jpg", "https://cdn.example.com/2.jpg"],
                          "tagCodes": ["coffee", "view"]
                        }
                        """.trimIndent(),
                    ),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.reviewId").value(1))

            val review =
                jdbcTemplate.queryForMap(
                    "SELECT place_id, user_id, status, rating, content FROM place_reviews WHERE id = 1",
                )
            assertEquals(PLACE_ID, review["place_id"])
            assertEquals(USER_ID, review["user_id"])
            assertEquals("PUBLISHED", review["status"])
            assertEquals(5, review["rating"]) // smallint 은 JDBC 가 Integer 로 읽는다
            assertEquals("통창 뷰가 좋아요", review["content"])

            // 사진은 목록 순서가 곧 노출 순서(order_no)다.
            assertEquals(
                listOf("https://cdn.example.com/1.jpg", "https://cdn.example.com/2.jpg"),
                jdbcTemplate.queryForList(
                    "SELECT image_url FROM place_review_photos WHERE place_review_id = 1 ORDER BY order_no",
                    String::class.java,
                ),
            )
            // 태그는 마스터 테이블 없이 enum 이름으로 저장된다(V4).
            assertEquals(
                listOf("COFFEE", "VIEW"),
                jdbcTemplate.queryForList(
                    "SELECT tag FROM place_review_tag_links WHERE place_review_id = 1 ORDER BY tag",
                    String::class.java,
                ),
            )
        }

        @Test
        fun `별점만 남겨도 201이고 한마디·사진·태그는 비어 있다`() {
            mockMvc
                .perform(createReviewRequest(PLACE_ID, """{"rating":4}"""))
                .andExpect(status().isCreated)

            assertEquals(
                null,
                jdbcTemplate.queryForObject("SELECT content FROM place_reviews WHERE id = 1", String::class.java),
            )
            assertEquals(0, countRows("place_review_photos"))
            assertEquals(0, countRows("place_review_tag_links"))
        }

        @Test
        fun `한마디 앞뒤 공백은 잘라내고 저장한다`() {
            mockMvc
                .perform(createReviewRequest(PLACE_ID, """{"rating":4,"content":"  좋아요  "}"""))
                .andExpect(status().isCreated)

            assertEquals(
                "좋아요",
                jdbcTemplate.queryForObject("SELECT content FROM place_reviews WHERE id = 1", String::class.java),
            )
        }

        @Test
        fun `같은 사용자가 같은 장소에 또 써도 막지 않는다`() {
            // 재방문마다 남길 수 있어야 한다 — 스키마에도 유니크 제약이 없다.
            mockMvc.perform(createReviewRequest(PLACE_ID, """{"rating":5}""")).andExpect(status().isCreated)
            mockMvc.perform(createReviewRequest(PLACE_ID, """{"rating":3}""")).andExpect(status().isCreated)

            assertEquals(2, countRows("place_reviews"))
        }

        @Test
        fun `없는 장소에 쓰면 4043을 내려준다`() {
            mockMvc
                .perform(createReviewRequest(999999L, """{"rating":4}"""))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4043))

            assertEquals(0, countRows("place_reviews"))
        }

        @Test
        fun `모르는 태그 코드는 4001이고 리뷰를 남기지 않는다`() {
            mockMvc
                .perform(createReviewRequest(PLACE_ID, """{"rating":4,"tagCodes":["nosuchtag"]}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))

            assertEquals(0, countRows("place_reviews"))
        }

        @Test
        fun `별점이 범위를 벗어나면 4002를 내려준다`() {
            mockMvc
                .perform(createReviewRequest(PLACE_ID, """{"rating":6}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("rating"))

            assertEquals(0, countRows("place_reviews"))
        }

        @Test
        fun `사진이 6장을 넘으면 4002를 내려준다`() {
            val photoUrls = (1..7).joinToString(",") { "\"https://cdn.example.com/$it.jpg\"" }

            mockMvc
                .perform(createReviewRequest(PLACE_ID, """{"rating":4,"photoUrls":[$photoUrls]}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("photoUrls"))
        }

        @Test
        fun `태그가 상한을 넘으면 4002를 내려준다`() {
            val tagCodes = PlaceReviewTagCodes.take(6).joinToString(",") { "\"$it\"" }

            mockMvc
                .perform(createReviewRequest(PLACE_ID, """{"rating":4,"tagCodes":[$tagCodes]}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("tagCodes"))
        }

        @Test
        fun `한마디가 1000자를 넘으면 도메인 불변식이 4001로 막는다`() {
            // 요청 DTO 에는 길이 제한이 없어 도메인(PlaceReview.create)에서 걸린다 — 필드 단위 4002 가 아니다.
            mockMvc
                .perform(createReviewRequest(PLACE_ID, """{"rating":4,"content":"${"가".repeat(1001)}"}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))

            assertEquals(0, countRows("place_reviews"))
        }

        @Test
        fun `토큰이 없으면 401을 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/places/$PLACE_ID/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"rating":4}"""),
                ).andExpect(status().isUnauthorized)
        }

        /**
         * 회원가입 토큰에는 subject(userId)가 없어 `@CurrentUserId` 해석에서 먼저 막힌다 —
         * 인자 해석이 메서드 시큐리티(`@AccessTokenRequired`)보다 앞서므로 403 이 아니라 401 이다.
         * (경로 매처로 보호하는 `/api/v1/folders` 는 필터 단계에서 걸려 403 이 난다 — 같은 토큰이라도 응답이 다르다.)
         */
        @Test
        fun `회원가입 토큰으로는 리뷰를 쓸 수 없다`() {
            val registrationToken = jwtTokenProvider.issueRegistrationToken(SocialProvider.KAKAO, "social-1")

            mockMvc
                .perform(
                    post("/api/v1/places/$PLACE_ID/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $registrationToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"rating":4}"""),
                ).andExpect(status().isUnauthorized)

            assertEquals(0, countRows("place_reviews"))
        }

        @Test
        fun `mock=true 면 DB 저장 없이 목 reviewId 를 내려준다`() {
            mockMvc
                .perform(createReviewRequest(PLACE_ID, """{"rating":4}""").param("mock", "true"))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.reviewId").value(7))

            assertEquals(0, countRows("place_reviews"))
        }

        private fun createReviewRequest(
            placeId: Long,
            body: String,
        ) = post("/api/v1/places/$placeId/reviews")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(USER_ID)}")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)

        private fun countRows(table: String): Int =
            jdbcTemplate.queryForObject("SELECT count(*) FROM $table", Int::class.java) ?: 0

        private companion object {
            const val PLACE_ID = 601L
            const val USER_ID = 1L

            /** 요청 DTO 의 태그 개수 상한을 넘기려고 쓰는 유효한 코드들(`.ai/taxonomy.md` 공통 태그). */
            val PlaceReviewTagCodes = listOf("friendly", "browsing", "helpful", "quick", "clean", "interior")
        }
    }
