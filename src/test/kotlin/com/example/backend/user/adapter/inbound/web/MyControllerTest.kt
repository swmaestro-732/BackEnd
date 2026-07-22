package com.example.backend.user.adapter.inbound.web

import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 현재 사용자("나") 기준 리소스 모킹 API(`/api/v1/my`) 검증. 목 데이터라 DB 픽스처가 필요 없다.
 * 프로필 GET/PATCH/DELETE + 팔로잉 PUT/DELETE.
 */
@AutoConfigureMockMvc
@WithMockUser
class MyControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `내 프로필 조회는 카운트 포함 프로필을 내려준다`() {
            mockMvc
                .perform(get("/api/v1/my/profile"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.id").isNumber)
                .andExpect(jsonPath("$.data.nickname").isNotEmpty)
                .andExpect(jsonPath("$.data.followersCnt").isNumber)
                .andExpect(jsonPath("$.data.coursesCnt").isNumber)
        }

        @Test
        fun `내 프로필 조회 mockError=4040이면 404`() {
            mockMvc
                .perform(get("/api/v1/my/profile").param("mockError", "4040"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4040))
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        @Test
        fun `내 프로필 수정은 넘어온 필드를 반영한다`() {
            mockMvc
                .perform(
                    patch("/api/v1/my/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"nickname":"새닉네임"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.nickname").value("새닉네임"))
        }

        @Test
        fun `내 프로필 수정 닉네임을 빈 문자열로 보내면 4002`() {
            mockMvc
                .perform(
                    patch("/api/v1/my/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"nickname":""}"""),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("nickname"))
        }

        @Test
        fun `회원 탈퇴는 본문 없는 성공을 내려준다`() {
            mockMvc
                .perform(delete("/api/v1/my"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        @Test
        fun `팔로우는 팔로우 상태와 팔로워 수를 내려준다`() {
            mockMvc
                .perform(put("/api/v1/my/followings/10"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.isFollowing").value(true))
                .andExpect(jsonPath("$.data.followersCnt").isNumber)
        }

        @Test
        fun `언팔로우는 해제 상태를 내려준다`() {
            mockMvc
                .perform(delete("/api/v1/my/followings/10"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.isFollowing").value(false))
        }

        @Test
        fun `팔로우 mockError=4040이면 404`() {
            mockMvc
                .perform(put("/api/v1/my/followings/10").param("mockError", "4040"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4040))
        }

        @Test
        @WithAnonymousUser
        fun `인증 없으면 401`() {
            mockMvc
                .perform(get("/api/v1/my/profile"))
                .andExpect(status().isUnauthorized)
        }
    }
