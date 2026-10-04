package com.example.backend.bootstrap.config

import com.example.backend.support.IntegrationTestBase
import com.example.backend.support.NoriOpenSearchContainer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 실제 OpenSearch 컨테이너에 붙어 **성공 경로**(연결→클러스터 정보 수신, health UP)를 검증한다 —
 * [OpenSearchConfigTest] 가 커버하지 못하는 "실연결" 절반을 채운다.
 *
 * 무거워서(이미지 빌드+기동) 일반 `test`·PR CI 에서는 제외되고, 로컬 `./gradlew opensearchIt` 또는
 * OpenSearch 코드 변경 시 워크플로에서만 실행된다(build.gradle.kts 의 opensearchIt 태스크).
 *
 * 컨테이너는 [NoriOpenSearchContainer](nori 설치·보안 끔·평문 HTTP)를 공유한다 — 실 AWS FGAC(TLS+basic auth)는
 * AWS 전용이라 로컬 재현 불가하고, 여기서 검증할 것은 "클라이언트 배선·health 인디케이터가 실제로 동작하는가"이다.
 * endpoint 를 `http://host:port` 스킴 포함으로 넘겨 [OpenSearchConfig] 의 임의 스킴·포트 경로도 함께 탄다.
 */
@AutoConfigureMockMvc
class OpenSearchIntegrationTest
    @Autowired
    constructor(
        private val openSearchClient: OpenSearchClient,
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `실제 OpenSearch 에 붙어 클러스터 버전을 받아온다`() {
            val version = openSearchClient.info().version().number()
            assertTrue(version.isNotBlank()) { "OpenSearch info() 응답에 버전이 없다: $version" }
        }

        @Test
        fun `실연결 시 actuator health 의 집계가 UP`() {
            mockMvc
                .perform(get("/actuator/health"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("UP"))
        }

        companion object {
            @JvmStatic
            @DynamicPropertySource
            fun openSearchProperties(registry: DynamicPropertyRegistry) {
                registry.add("opensearch.endpoint") { NoriOpenSearchContainer.endpoint() }
                registry.add("opensearch.username") { "admin" }
                registry.add("opensearch.password") { "admin" }
            }
        }
    }
