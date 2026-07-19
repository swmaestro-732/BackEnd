package com.example.backend.user.adapter.inbound.web

import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** User 도메인 팔로우/언팔로우 모킹 API(`/api/v1/users/{userId}/followers`) 검증. */
@AutoConfigureMockMvc
class FollowControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `팔로우는 팔로우 상태와 팔로워 수를 내려준다`() {
            mockMvc
                .perform(post("/api/v1/users/10/followers"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.isFollowing").value(true))
                .andExpect(jsonPath("$.data.followersCnt").isNumber)
        }

        @Test
        fun `언팔로우는 해제 상태를 내려준다`() {
            mockMvc
                .perform(delete("/api/v1/users/10/followers/20"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.isFollowing").value(false))
        }

        @Test
        fun `팔로우 mockError=4040이면 404`() {
            mockMvc
                .perform(post("/api/v1/users/10/followers").param("mockError", "4040"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4040))
        }
    }
