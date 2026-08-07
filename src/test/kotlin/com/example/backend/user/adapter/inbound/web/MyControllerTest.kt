package com.example.backend.user.adapter.inbound.web

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
@Sql(
    statements =
        [
            "TRUNCATE TABLE follows, refresh_tokens, tags, users RESTART IDENTITY CASCADE",
            "INSERT INTO users (nickname, handle, bio) VALUES ('나테스트', 'me_handle', '기존 자기소개')",
            "INSERT INTO users (nickname, handle) VALUES ('상대테스트', 'target_handle')",
            // 관심 태그 검증용 코스 태그(id 1~4). likeTagIds 는 이 중에서만 유효하다.
            "INSERT INTO tags (name) VALUES ('감성카페'), ('통창뷰'), ('데이트'), ('브런치')",
        ],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
class MyControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jdbcTemplate: JdbcTemplate,
    ) : IntegrationTestBase() {
        // 내 프로필 단독 조회는 마이페이지(GET /service/v1/mypage)로 대체돼 제거됨 — 관련 테스트 삭제.

        @Test
        fun `현재 사용자 전용 엔드포인트는 토큰이 없으면 401을 내려준다`() {
            mockMvc
                .perform(put("/api/v1/users/followers/$TARGET_ID"))
                .andExpect(status().isUnauthorized)
        }

        @Test
        fun `내 프로필 수정 mock 폴백은 DB 사용자 없이도 목 응답을 내려준다`() {
            mockMvc
                .perform(
                    patch("/api/v1/users")
                        .param("mock", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(999)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """{"nickname":"목닉네임","handle":"mock_handle","profileImageUrl":"https://example.com/mock.jpg","bio":"목 자기소개"}""",
                        ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nickname").value("목닉네임"))
                .andExpect(jsonPath("$.data.handle").value("mock_handle"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://example.com/mock.jpg"))
                .andExpect(jsonPath("$.data.bio").value("목 자기소개"))
        }

        @Test
        fun `내 프로필 수정 mockError로 4040을 주입한다`() {
            mockMvc
                .perform(
                    patch("/api/v1/users")
                        .param("mockError", "4040")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"nickname":"사용되지않음"}"""),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4040))
        }

        @Test
        fun `내 프로필을 수정한다`() {
            mockMvc
                .perform(
                    patch("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"nickname":"새닉네임"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.nickname").value("새닉네임"))
                .andExpect(jsonPath("$.data.bio").value("기존 자기소개"))
        }

        @Test
        fun `내 bio를 수정한다`() {
            mockMvc
                .perform(
                    patch("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"bio":"새 자기소개"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.bio").value("새 자기소개"))
        }

        @Test
        fun `내 프로필 이미지를 수정한다`() {
            mockMvc
                .perform(
                    patch("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"profileImageUrl":"https://example.com/new-profile.jpg"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://example.com/new-profile.jpg"))
        }

        @Test
        fun `중복되지 않은 핸들로 수정한다`() {
            mockMvc
                .perform(
                    patch("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"handle":"new_unique_handle"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.handle").value("new_unique_handle"))
        }

        @Test
        fun `닉네임을 빈 문자열로 수정하면 필드 검증 에러를 내려준다`() {
            mockMvc
                .perform(
                    patch("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"nickname":""}"""),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("nickname"))
        }

        @Test
        fun `이미 사용 중인 닉네임으로 수정하면 4091을 내려준다`() {
            mockMvc
                .perform(
                    patch("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"nickname":"상대테스트"}"""),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4091))
        }

        @Test
        fun `이미 사용 중인 핸들로 수정하면 4092를 내려준다`() {
            mockMvc
                .perform(
                    patch("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"handle":"target_handle"}"""),
                ).andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4092))
        }

        @Test
        fun `팔로우는 멱등이고 대상의 팔로워 수를 한 번만 늘린다`() {
            repeat(2) {
                mockMvc
                    .perform(
                        put("/api/v1/users/followers/$TARGET_ID")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}"),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.code").value(2000))
                    .andExpect(jsonPath("$.data.isFollowing").value(true))
                    .andExpect(jsonPath("$.data.followersCnt").value(1))
            }
        }

        @Test
        fun `팔로우 mock 폴백은 DB 사용자 없이도 목 응답을 내려준다`() {
            mockMvc
                .perform(
                    put("/api/v1/users/followers/998")
                        .param("mock", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(999)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.isFollowing").value(true))
                .andExpect(jsonPath("$.data.followersCnt").value(129))
        }

        @Test
        fun `팔로우 mockError로 4040을 주입한다`() {
            mockMvc
                .perform(
                    put("/api/v1/users/followers/$TARGET_ID")
                        .param("mockError", "4040")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4040))
        }

        @Test
        fun `팔로우한 사용자를 언팔로우하면 팔로워 수를 줄인다`() {
            mockMvc
                .perform(
                    put("/api/v1/users/followers/$TARGET_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.followersCnt").value(1))

            mockMvc
                .perform(
                    delete("/api/v1/users/followers/$TARGET_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.isFollowing").value(false))
                .andExpect(jsonPath("$.data.followersCnt").value(0))
        }

        @Test
        fun `언팔로우 mock 폴백은 DB 사용자 없이도 목 응답을 내려준다`() {
            mockMvc
                .perform(
                    delete("/api/v1/users/followers/998")
                        .param("mock", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(999)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.isFollowing").value(false))
                .andExpect(jsonPath("$.data.followersCnt").value(128))
        }

        @Test
        fun `언팔로우 mockError로 4040을 주입한다`() {
            mockMvc
                .perform(
                    delete("/api/v1/users/followers/$TARGET_ID")
                        .param("mockError", "4040")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4040))
        }

        @Test
        fun `자기 자신을 팔로우하면 4001을 내려준다`() {
            mockMvc
                .perform(
                    put("/api/v1/users/followers/$ME_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}"),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `없는 사용자를 팔로우하면 4042를 내려준다`() {
            mockMvc
                .perform(
                    put("/api/v1/users/followers/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4042))
        }

        @Test
        fun `회원 탈퇴하면 프로필 조회에서 제외된다`() {
            val accessToken = tokenFor(ME_ID)

            mockMvc
                .perform(
                    delete("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data").doesNotExist())

            mockMvc
                .perform(
                    get("/service/v1/mypage")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4042))
        }

        @Test
        fun `회원 탈퇴하면 handle 이 해제되어 재사용 가능해진다`() {
            // 탈퇴 전엔 내 handle 이 사용 중
            mockMvc
                .perform(get("/api/v1/users/availability").param("handle", "me_handle"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.available").value(false))

            mockMvc
                .perform(
                    delete("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}"),
                ).andExpect(status().isOk)

            // 탈퇴 후엔 handle 이 해제되어 사용 가능
            mockMvc
                .perform(get("/api/v1/users/availability").param("handle", "me_handle"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.available").value(true))
        }

        @Test
        fun `회원 탈퇴 mock 폴백은 실제 사용자를 탈퇴시키지 않는다`() {
            val accessToken = tokenFor(ME_ID)

            mockMvc
                .perform(
                    delete("/api/v1/users")
                        .param("mock", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))

            mockMvc
                .perform(
                    get("/service/v1/mypage")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.profile.id").value(ME_ID))
        }

        @Test
        fun `회원 탈퇴 mockError로 4040을 주입한다`() {
            mockMvc
                .perform(
                    delete("/api/v1/users")
                        .param("mockError", "4040")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4040))
        }

        private fun tokenFor(userId: Long) = jwtTokenProvider.issueAccessToken(userId)

        @Test
        fun `관심 태그(likeTagIds)를 수정하면 user_like_tags 가 전체 치환된다`() {
            patchLikeTags(listOf(1, 2, 3))
            assertEquals(setOf(1L, 2L, 3L), likeTagIdsOf(ME_ID))

            // 전체 치환 — 기존 제거·새 집합으로 교체.
            patchLikeTags(listOf(4))
            assertEquals(setOf(4L), likeTagIdsOf(ME_ID))

            // likeTagIds 를 보내지 않으면(null) 관심 태그는 그대로 둔다(빈 배열의 전체 해제와 구분).
            patchBody("""{"nickname":"태그무관수정"}""")
            assertEquals(setOf(4L), likeTagIdsOf(ME_ID))

            // 빈 배열 = 전체 해제.
            patchLikeTags(emptyList())
            assertEquals(emptySet<Long>(), likeTagIdsOf(ME_ID))
        }

        @Test
        fun `존재하지 않는 태그 id 로 수정하면 4001을 내려주고 저장하지 않는다`() {
            mockMvc
                .perform(
                    patch("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"likeTagIds":[1,999]}"""),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))

            assertEquals(emptySet<Long>(), likeTagIdsOf(ME_ID))
        }

        private fun patchLikeTags(tagIds: List<Long>) = patchBody("""{"likeTagIds":$tagIds}""")

        private fun patchBody(json: String) {
            mockMvc
                .perform(
                    patch("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(ME_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json),
                ).andExpect(status().isOk)
        }

        private fun likeTagIdsOf(userId: Long): Set<Long> =
            jdbcTemplate
                .queryForList("SELECT tag_id FROM user_like_tags WHERE user_id = ?", Long::class.java, userId)
                .filterNotNull()
                .toSet()

        private companion object {
            const val ME_ID = 1L
            const val TARGET_ID = 2L
        }
    }
