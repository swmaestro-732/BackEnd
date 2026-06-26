package com.example.backend.domain.sample

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
    statements = ["TRUNCATE TABLE samples RESTART IDENTITY CASCADE"],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
class SampleControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `샘플 생성 후 목록에 포함된다`() {
            mockMvc
                .post("/api/samples", """{"name":"hello"}""")
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("hello"))

            mockMvc
                .perform(get("/api/samples"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0].name").value("hello"))
        }

        @Test
        fun `이름이 비면 400과 에러 응답`() {
            mockMvc
                .post("/api/samples", """{"name":""}""")
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
