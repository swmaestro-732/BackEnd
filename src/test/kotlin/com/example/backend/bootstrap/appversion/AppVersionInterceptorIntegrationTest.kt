package com.example.backend.bootstrap.appversion

import com.example.backend.support.IntegrationTestBase
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 캐시 TTL 을 0 으로 낮춰 매 요청마다 DB 정책을 다시 읽게 한다. */
@AutoConfigureMockMvc
@TestPropertySource(properties = ["app.version.cache-ttl=0s"])
class AppVersionInterceptorIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @BeforeEach
        fun insertPolicy() {
            transaction {
                AppVersionPolicyTable.deleteAll()
                AppVersionPolicyTable.insert {
                    it[feature] = "course-detail"
                    it[platform] = "android"
                    it[minBuild] = 10
                }
            }
        }

        @AfterEach
        fun cleanUp() {
            transaction { AppVersionPolicyTable.deleteAll() }
        }

        @Test
        fun `기준 미달 안드로이드 빌드는 426 엔벨로프를 받는다`() {
            mockMvc
                .perform(get("/api/v1/courses/999999").header("X-App-Platform", "android").header("X-App-Build", "9"))
                .andExpect(status().`is`(426))
                .andExpect(jsonPath("$.code").value(4260))
        }

        @Test
        fun `기준을 충족한 빌드는 강업 검사를 통과해 컨트롤러까지 도달한다`() {
            mockMvc
                .perform(get("/api/v1/courses/999999").header("X-App-Platform", "android").header("X-App-Build", "10"))
                .andExpect(status().isNotFound)
        }

        @Test
        fun `빌드 헤더가 없으면 정책이 있어도 통과한다`() {
            mockMvc
                .perform(get("/api/v1/courses/999999"))
                .andExpect(status().isNotFound)
        }

        @Test
        fun `빌드 헤더가 정수가 아니면 400 INVALID_APP_HEADER를 받는다`() {
            mockMvc
                .perform(get("/api/v1/courses/999999").header("X-App-Platform", "android").header("X-App-Build", "abc"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4005))
        }

        @Test
        fun `명시적 iOS 헤더는 iOS 정책이 없으면 통과한다`() {
            mockMvc
                .perform(
                    get("/api/v1/courses/999999")
                        .header("X-App-Platform", "ios")
                        .header("X-App-Build", "1"),
                ).andExpect(status().isNotFound)
        }

        @Test
        fun `빌드만 보내거나 알 수 없는 플랫폼이면 400을 받는다`() {
            mockMvc
                .perform(get("/api/v1/courses/999999").header("X-App-Build", "9"))
                .andExpect(status().isBadRequest)
            mockMvc
                .perform(get("/api/v1/courses/999999").header("X-App-Platform", "web").header("X-App-Build", "9"))
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `플랫폼만 보내고 빌드가 없으면 400을 받는다`() {
            mockMvc
                .perform(get("/api/v1/courses/999999").header("X-App-Platform", "android"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4005))
        }

        @Test
        fun `iOS 정책이 있으면 기준 미달 iOS 빌드를 차단한다`() {
            transaction {
                AppVersionPolicyTable.insert {
                    it[feature] = "course-detail"
                    it[platform] = "ios"
                    it[minBuild] = 30
                }
            }

            // android(min 10) 기준이면 통과할 빌드 29가 ios(min 30) 기준에는 미달 — 플랫폼별 정책 분리
            mockMvc
                .perform(get("/api/v1/courses/999999").header("X-App-Platform", "ios").header("X-App-Build", "29"))
                .andExpect(status().`is`(426))
                .andExpect(jsonPath("$.code").value(4260))
        }

        @Test
        fun `메서드 어노테이션이 클래스 어노테이션보다 우선한다`() {
            transaction {
                AppVersionPolicyTable.insert {
                    it[feature] = "course-create"
                    it[platform] = "android"
                    it[minBuild] = 20
                }
            }

            // 빌드 15는 클래스 그룹(course-detail, min 10)은 충족하지만 메서드 그룹(course-create, min 20)에 미달 → 426
            mockMvc
                .perform(get("/api/v1/courses/drafts").header("X-App-Platform", "android").header("X-App-Build", "15"))
                .andExpect(status().`is`(426))
                .andExpect(jsonPath("$.code").value(4260))
        }

        @Test
        fun `어노테이션 없는 actuator 헬스체크는 통과한다`() {
            mockMvc
                .perform(get("/actuator/health"))
                .andExpect(status().isOk)
        }
    }
