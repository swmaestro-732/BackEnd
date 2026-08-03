package com.example.backend.support

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File

/**
 * springdoc 가 코드에서 생성한 OpenAPI 스펙(= 구현의 진실)을 build/openapi.json 으로 export 한다.
 * CI 의 check_contract.py 가 이 파일을 design.yaml(약속) 과 대조한다.
 *
 * "테스트"인 이유: 전체 컨텍스트를 띄워야 실제 스펙이 나오고, 기존 @SpringBootTest 인프라
 * (CI Postgres)를 그대로 재사용하기 위함. 산출물만 뽑고 단언은 최소.
 */
// addFilters = false: 문서 스펙만 뽑는 목적이라 JWT 보안 필터를 끈다(인증 테스트가 아님).
@AutoConfigureMockMvc(addFilters = false)
class OpenApiExportTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `springdoc OpenAPI 스펙을 build_openapi_json 으로 내보낸다`() {
            val json =
                mockMvc
                    .perform(get("/v3/api-docs"))
                    .andExpect(status().isOk)
                    .andReturn()
                    .response
                    .contentAsString

            File("build/openapi.json").apply {
                parentFile.mkdirs()
                writeText(json)
            }
            // 그룹 분리(user/course/...) 때문에 기본 엔드포인트가 전부를 담는지 진단용 로그.
            val paths = Regex("\"(/[^\"]+)\":\\{").findAll(json).map { it.groupValues[1] }.toList()
            println("[openapi-export] paths=${paths.size} recommended-tags=${paths.any { it.contains("recommended-tags") }}")
        }
    }
