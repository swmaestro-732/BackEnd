package com.example.backend.bootstrap.config

import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
class AdminCorsIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `어드민 웹 오리진의 preflight 는 인증 없이 통과하고 허용 헤더를 받는다`() {
            mockMvc
                .perform(
                    options("/admin/v1/courses")
                        .header(HttpHeaders.ORIGIN, ADMIN_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"),
                ).andExpect(status().isOk)
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ADMIN_ORIGIN))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
        }

        @Test
        fun `허용하지 않은 오리진의 preflight 는 403 이다`() {
            mockMvc
                .perform(
                    options("/admin/v1/courses")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"),
                ).andExpect(status().isForbidden)
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
        }

        @Test
        fun `어드민 밖 경로에는 어드민 오리진이어도 CORS 헤더를 주지 않는다`() {
            mockMvc
                .perform(get("/actuator/health").header(HttpHeaders.ORIGIN, ADMIN_ORIGIN))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
        }

        private companion object {
            const val ADMIN_ORIGIN = "https://chilsami-admin.courmy.com"
        }
    }
