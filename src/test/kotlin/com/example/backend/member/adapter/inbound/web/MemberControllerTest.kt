package com.example.backend.member.adapter.inbound.web

import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
// 각 테스트 전에 테이블을 비워 DB 상태에 의존하지 않도록(결정성 확보).
@Sql(
    statements = ["TRUNCATE TABLE users RESTART IDENTITY CASCADE"],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
class MemberControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `멤버 생성 후 목록에 포함된다`() {
            mockMvc
                .post("/api/members", """{"nickname":"hello"}""")
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nickname").value("hello"))

            mockMvc
                .perform(get("/api/members"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0].nickname").value("hello"))
        }

        @Test
        fun `닉네임이 비면 400과 에러 응답`() {
            mockMvc
                .post("/api/members", """{"nickname":""}""")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("nickname"))
        }

        private fun MockMvc.post(
            url: String,
            body: String,
        ) = perform(
            post(url).contentType(MediaType.APPLICATION_JSON).content(body),
        )
    }
