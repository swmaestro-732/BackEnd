package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.sql.Timestamp
import java.time.Instant

@AutoConfigureMockMvc
@Sql(scripts = ["/sql/course-comments-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseCommentControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jdbcTemplate: JdbcTemplate,
        private val jwtEncoder: JwtEncoder,
    ) : IntegrationTestBase() {
        @Test
        fun `댓글 생성은 201과 commentId를 반환하고 댓글 수를 증가시킨다`() {
            val response =
                mockMvc
                    .perform(
                        post(BASE_PATH)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"content":"새 댓글"}"""),
                    ).andExpect(status().isCreated)
                    .andExpect(jsonPath("$.code").value(2000))
                    .andExpect(jsonPath("$.data.commentId").isNumber)
                    .andReturn()
            val commentId =
                Regex(""""commentId"\s*:\s*(\d+)""")
                    .find(response.response.contentAsString)!!
                    .groupValues[1]
                    .toLong()

            mockMvc
                .perform(
                    get(BASE_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.items[0].id").value(commentId))
                .andExpect(jsonPath("$.data.items[0].authorId").value(OWNER_ID))
                .andExpect(jsonPath("$.data.items[0].content").value("새 댓글"))
                .andExpect(jsonPath("$.data.items[0].createdAt").isString)
                .andExpect(jsonPath("$.data.items[0].updatedAt").isString)
                .andExpect(jsonPath("$.data.items[0].isMine").value(true))
            assertCount(COURSE_ID, 4)
            assertEquals("ACTIVE", commentRow(commentId)["status"])
        }

        @Test
        fun `없는 코스나 삭제된 코스에 댓글을 생성하면 4041이다`() {
            for (courseId in listOf(99999L, 3L)) {
                mockMvc
                    .perform(
                        post("/api/v1/courses/$courseId/comments")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"content":"댓글"}"""),
                    ).andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value(4041))
            }
            assertEquals(6, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM course_comments", Int::class.java))
            assertCount(3L, 1)
        }

        @Test
        fun `비로그인 목록은 최신순이며 삭제 댓글과 다른 코스 댓글을 제외한다`() {
            mockMvc
                .perform(get(BASE_PATH))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].id").value(3))
                .andExpect(jsonPath("$.data.items[1].id").value(2))
                .andExpect(jsonPath("$.data.items[2].id").value(1))
                .andExpect(jsonPath("$.data.items[0].isMine").value(false))
                .andExpect(jsonPath("$.data.items[1].isMine").value(false))
                .andExpect(jsonPath("$.data.items[2].isMine").value(false))
                .andExpect(jsonPath("$.data.items[0].createdAt").value("2026-09-01T02:00:00Z"))
                .andExpect(jsonPath("$.data.items[0].updatedAt").value("2026-09-01T02:00:00Z"))
                .andExpect(jsonPath("$.data.hasNext").value(false))
        }

        @Test
        fun `목록은 본인 여부와 다음 페이지를 표시하고 커서 댓글을 제외한다`() {
            mockMvc
                .perform(
                    get(BASE_PATH)
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(3))
                .andExpect(jsonPath("$.data.items[0].isMine").value(true))
                .andExpect(jsonPath("$.data.items[1].id").value(2))
                .andExpect(jsonPath("$.data.items[1].isMine").value(false))
                .andExpect(jsonPath("$.data.hasNext").value(true))

            mockMvc
                .perform(get(BASE_PATH).param("size", "2").param("cursor", "2"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false))
        }

        @Test
        fun `마지막 댓글 이후 커서는 빈 마지막 페이지를 반환한다`() {
            mockMvc
                .perform(get(BASE_PATH).param("cursor", "1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.items").isArray)
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(false))
        }

        @Test
        fun `댓글 수정은 내용과 수정 시각을 바꾸고 생성 시각과 카운터를 유지한다`() {
            mockMvc
                .perform(
                    patch("$BASE_PATH/3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"content":"수정한 댓글"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
            val row = commentRow(3L)
            val original = Instant.parse("2026-09-01T02:00:00Z")
            assertEquals("수정한 댓글", row["content"])
            assertEquals(original, (row["created_at"] as Timestamp).toInstant())
            assertTrue((row["updated_at"] as Timestamp).toInstant().isAfter(original))
            assertCount(COURSE_ID, 3)
        }

        @Test
        fun `타인 댓글 수정은 4047이고 내용을 유지한다`() {
            mockMvc
                .perform(
                    patch("$BASE_PATH/3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OTHER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"content":"타인의 수정"}"""),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4047))
            assertEquals("최신 댓글", commentRow(3L)["content"])
            assertCount(COURSE_ID, 3)
        }

        @Test
        fun `다른 코스 경로로 본인 댓글을 수정하거나 삭제할 수 없다`() {
            mockMvc
                .perform(
                    patch("/api/v1/courses/2/comments/3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"content":"잘못된 경로"}"""),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4047))
            mockMvc
                .perform(
                    delete("/api/v1/courses/2/comments/3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4047))
            assertEquals("최신 댓글", commentRow(3L)["content"])
            assertEquals("ACTIVE", commentRow(3L)["status"])
            assertCount(COURSE_ID, 3)
            assertCount(2L, 1)
        }

        @Test
        fun `없는 댓글이나 이미 삭제된 댓글의 수정과 삭제는 4047이다`() {
            for (commentId in listOf(4L, 99999L)) {
                mockMvc
                    .perform(
                        patch("$BASE_PATH/$commentId")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"content":"수정 시도"}"""),
                    ).andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value(4047))
                mockMvc
                    .perform(
                        delete("$BASE_PATH/$commentId")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                    ).andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value(4047))
            }
            assertEquals("이미 삭제된 댓글", commentRow(4L)["content"])
            assertCount(COURSE_ID, 3)
        }

        @Test
        fun `댓글은 소프트 삭제되고 재삭제해도 카운터는 한 번만 감소한다`() {
            mockMvc
                .perform(
                    delete("$BASE_PATH/3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.message").value("댓글이 삭제되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
            val row = commentRow(3L)
            assertEquals("DELETED", row["status"])
            assertNotNull(row["deleted_at"])
            assertEquals(row["deleted_at"], row["updated_at"])
            assertCount(COURSE_ID, 2)

            mockMvc
                .perform(get(BASE_PATH))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(2))
            mockMvc
                .perform(
                    delete("$BASE_PATH/3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4047))
            assertCount(COURSE_ID, 2)
        }

        @Test
        fun `타인 댓글 삭제는 4047이고 카운터를 유지한다`() {
            mockMvc
                .perform(
                    delete("$BASE_PATH/3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OTHER_ID)}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4047))
            assertEquals("ACTIVE", commentRow(3L)["status"])
            assertCount(COURSE_ID, 3)
        }

        @Test
        fun `삭제된 코스의 본인 댓글 삭제는 코스 카운터를 변경하지 않는다`() {
            mockMvc
                .perform(
                    delete("/api/v1/courses/3/comments/6")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isOk)
            assertEquals("DELETED", commentRow(6L)["status"])
            assertCount(3L, 1)
        }

        @Test
        fun `댓글 생성과 수정은 빈 내용과 1000자를 초과하는 내용을 거부한다`() {
            for (content in listOf("", "   ", "가".repeat(1001))) {
                for (request in listOf(post(BASE_PATH), patch("$BASE_PATH/3"))) {
                    mockMvc
                        .perform(
                            request
                                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""{"content":"$content"}"""),
                        ).andExpect(status().isBadRequest)
                        .andExpect(jsonPath("$.code").value(4002))
                        .andExpect(jsonPath("$.fieldErrors[0].field").value("content"))
                }
            }
            assertEquals("최신 댓글", commentRow(3L)["content"])
            assertCount(COURSE_ID, 3)
        }

        @Test
        fun `내용이 없거나 null인 생성과 수정은 거부한다`() {
            for (body in listOf("{}", """{"content":null}""")) {
                for (request in listOf(post(BASE_PATH), patch("$BASE_PATH/3"))) {
                    mockMvc
                        .perform(
                            request
                                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body),
                        ).andExpect(status().isBadRequest)
                }
            }
            assertCount(COURSE_ID, 3)
        }

        @Test
        fun `1000자 댓글의 생성과 수정은 허용한다`() {
            val content = "가".repeat(1000)
            mockMvc
                .perform(
                    post(BASE_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"content":"$content"}"""),
                ).andExpect(status().isCreated)
            mockMvc
                .perform(
                    patch("$BASE_PATH/3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"content":"$content"}"""),
                ).andExpect(status().isOk)
            assertEquals(content, commentRow(3L)["content"])
            assertCount(COURSE_ID, 4)
        }

        @Test
        fun `비로그인 사용자는 댓글을 작성 수정 삭제할 수 없다`() {
            for (request in listOf(post(BASE_PATH), patch("$BASE_PATH/3"), delete("$BASE_PATH/3"))) {
                mockMvc
                    .perform(
                        request
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"content":"인증 없는 댓글"}"""),
                    ).andExpect(status().isUnauthorized)
            }
            assertEquals("최신 댓글", commentRow(3L)["content"])
            assertEquals("ACTIVE", commentRow(3L)["status"])
            assertCount(COURSE_ID, 3)
        }

        @Test
        fun `access 목적이 아닌 JWT로 댓글을 작성 수정 삭제할 수 없다`() {
            // subject 해석을 통과시켜 @AccessTokenRequired의 purpose 검사를 검증한다.
            val now = Instant.now()
            val claims =
                JwtClaimsSet
                    .builder()
                    .subject(OWNER_ID.toString())
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(60))
                    .claim("purpose", "registration")
                    .build()
            val header = JwsHeader.with(MacAlgorithm.HS256).build()
            val registrationToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue

            for (request in listOf(post(BASE_PATH), patch("$BASE_PATH/3"), delete("$BASE_PATH/3"))) {
                mockMvc
                    .perform(
                        request
                            .header(HttpHeaders.AUTHORIZATION, "Bearer $registrationToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"content":"회원가입 토큰 댓글"}"""),
                    ).andExpect(status().isForbidden)
            }
            assertEquals("최신 댓글", commentRow(3L)["content"])
            assertEquals("ACTIVE", commentRow(3L)["status"])
            assertCount(COURSE_ID, 3)
        }

        private fun tokenFor(userId: Long) = jwtTokenProvider.issueAccessToken(userId)

        private fun commentRow(commentId: Long) =
            jdbcTemplate.queryForMap("SELECT * FROM course_comments WHERE id = ?", commentId)

        private fun assertCount(
            courseId: Long,
            expected: Int,
        ) {
            assertEquals(
                expected,
                jdbcTemplate.queryForObject("SELECT comments_cnt FROM courses WHERE id = ?", Int::class.java, courseId),
            )
        }

        private companion object {
            const val OWNER_ID = 1L
            const val OTHER_ID = 2L
            const val COURSE_ID = 1L
            const val BASE_PATH = "/api/v1/courses/1/comments"
        }
    }
