package com.example.backend.user.adapter.inbound.web

import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
@Sql(
    statements =
        [
            "TRUNCATE TABLE follows, refresh_tokens, users RESTART IDENTITY CASCADE",
            "INSERT INTO users (nickname, handle) VALUES ('나테스트', 'me_handle')",
        ],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
class UserControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        // 다른 사용자 프로필 조회(GET /{userId})는 마이페이지(GET /service/v1/mypage/{handle})로 대체돼 제거 — 관련 테스트 삭제.

        @Test
        fun `핸들 사용 가능 여부 - 미사용 값은 가능, 사용중·예약어는 불가`() {
            mockMvc
                .perform(get("/api/v1/users/availability").param("handle", "newbie"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.available").value(true))

            mockMvc
                .perform(get("/api/v1/users/availability").param("handle", "me_handle"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.available").value(false))

            mockMvc
                .perform(get("/api/v1/users/availability").param("handle", "admin"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.available").value(false))
        }
    }
