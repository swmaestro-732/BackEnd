package com.example.backend.bootstrap.config

import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.ApplicationContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * opensearch.endpoint 가 설정되면 클라이언트 빈이 생성되고(연결 배선),
 * OpenSearch 가 불통이어도 `/actuator/health` 가 UP(200)을 유지하는지(ALB 보호 — 불통 시 UNKNOWN) 검증한다.
 * 실제 도메인이 없어도 되도록 도달 불가한 로컬 값을 넣는다(클라이언트 생성은 연결하지 않으므로 부팅에 무해).
 */
@AutoConfigureMockMvc
@TestPropertySource(
    // endpoint 양끝에 공백을 넣어 가드(@ConditionalOnExpression)·HttpHost 양쪽의 trim 정규화를 회귀 검증한다.
    properties = [
        "opensearch.endpoint= localhost ",
        "opensearch.username=admin",
        "opensearch.password=admin",
    ],
)
class OpenSearchConfigTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val context: ApplicationContext,
    ) : IntegrationTestBase() {
        @Test
        fun `endpoint 가 있으면 OpenSearchClient 빈이 생성된다`() {
            assertTrue(context.getBeanNamesForType(OpenSearchClient::class.java).isNotEmpty())
        }

        @Test
        fun `OpenSearch 불통이어도 actuator health 는 UP(200)을 유지한다`() {
            mockMvc
                .perform(get("/actuator/health"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("UP"))
        }
    }
