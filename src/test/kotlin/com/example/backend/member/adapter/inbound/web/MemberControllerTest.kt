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
    statements = ["TRUNCATE TABLE members RESTART IDENTITY CASCADE"],
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
                .post("/api/members", """{"name":"hello","area":"서울"}""")
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("hello"))
                .andExpect(jsonPath("$.area").value("서울"))

            mockMvc
                .perform(get("/api/members"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0].name").value("hello"))
                .andExpect(jsonPath("$[0].area").value("서울"))
        }

        @Test
        fun `이름이 비면 400과 에러 응답`() {
            mockMvc
                .post("/api/members", """{"name":"","area":"서울"}""")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
        }

        private fun MockMvc.post(
            url: String,
            body: String,
        ) = perform(
            post(url).contentType(MediaType.APPLICATION_JSON).content(body),
        )
    }
