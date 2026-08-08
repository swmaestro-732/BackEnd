package com.example.backend.mobile.home.adapter.inbound.web

import com.example.backend.support.IntegrationTestBase
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 공개 코스 피드(BFF) 컨트롤러(`GET /service/v1/home`) 통합 테스트.
 * 공개 후보(course 도메인) + 코스별 저장수(user 도메인) 조합을 검증한다 —
 * PUBLIC·발행 코스만, 저장수 내림차순·최신순으로 랭킹된다(course-feed-fixture.sql).
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/course-feed-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class HomeFeedControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `공개 발행 코스만 저장수 내림차순·최신순으로 내려준다`() {
            mockMvc
                .perform(get("/service/v1/home"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                // PUBLIC·발행 3건만(PRIVATE·FOLLOWER·미발행 제외)
                .andExpect(jsonPath("$.data.courses.length()").value(3))
                // 1위: course 2(저장 5건) — 최신은 아니지만 저장수 우선
                .andExpect(jsonPath("$.data.courses[0].id").value(2))
                .andExpect(jsonPath("$.data.courses[0].title").value("공개 인기 코스"))
                .andExpect(jsonPath("$.data.courses[0].savesCnt").value(5))
                // 2위: course 1(저장 1건)
                .andExpect(jsonPath("$.data.courses[1].id").value(1))
                .andExpect(jsonPath("$.data.courses[1].savesCnt").value(1))
                // 3위: course 6(저장 0건)
                .andExpect(jsonPath("$.data.courses[2].id").value(6))
                .andExpect(jsonPath("$.data.courses[2].savesCnt").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty)
        }

        @Test
        fun `size로 첫 페이지를 조회하고 복합 커서로 중복 없이 다음 페이지를 잇는다`() {
            val firstPageBody =
                mockMvc
                    .perform(get("/service/v1/home").param("size", "2"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.code").value(2000))
                    .andExpect(jsonPath("$.data.courses.length()").value(2))
                    .andExpect(jsonPath("$.data.courses[0].id").value(2))
                    .andExpect(jsonPath("$.data.courses[1].id").value(1))
                    .andExpect(jsonPath("$.data.hasNext").value(true))
                    .andExpect(jsonPath("$.data.nextCursor").isNotEmpty)
                    .andReturn()
                    .response.contentAsString
            val nextCursor: String = JsonPath.read(firstPageBody, "$.data.nextCursor")

            mockMvc
                .perform(
                    get("/service/v1/home")
                        .param("size", "2")
                        .param("cursor", nextCursor),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.courses.length()").value(1))
                .andExpect(jsonPath("$.data.courses[0].id").value(6))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty)
        }

        @Test
        fun `잘못된 커서면 4001을 내려준다`() {
            mockMvc
                .perform(get("/service/v1/home").param("cursor", "not-a-feed-cursor"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `mock=true면 DB와 무관하게 고정 피드 목을 내려준다`() {
            mockMvc
                .perform(get("/service/v1/home?mock=true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.courses.length()").value(3))
                .andExpect(jsonPath("$.data.courses[0].title").value("비 오는 날 성수 감성 카페 코스"))
                .andExpect(jsonPath("$.data.courses[0].savesCnt").value(342))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty)
        }

        @Test
        fun `size가 범위를 벗어나면 4002를 내려준다`() {
            mockMvc
                .perform(get("/service/v1/home").param("size", "0"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }
    }
