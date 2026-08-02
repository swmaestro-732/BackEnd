package com.example.backend.mobile.course.adapter.inbound.web

import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 공개 코스 피드(BFF) 컨트롤러(`GET /service/v1/courses`) 통합 테스트.
 * 공개 후보(course 도메인) + 코스별 저장수(user 도메인) 조합을 검증한다 —
 * PUBLIC·발행 코스만, 저장수 내림차순·최신순으로 랭킹된다(course-feed-fixture.sql).
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/course-feed-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseFeedControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `공개 발행 코스만 저장수 내림차순·최신순으로 내려준다`() {
            mockMvc
                .perform(get("/service/v1/courses"))
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
        }

        @Test
        fun `mock=true면 DB와 무관하게 고정 피드 목을 내려준다`() {
            mockMvc
                .perform(get("/service/v1/courses?mock=true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.courses.length()").value(3))
                .andExpect(jsonPath("$.data.courses[0].title").value("비 오는 날 성수 감성 카페 코스"))
                .andExpect(jsonPath("$.data.courses[0].savesCnt").value(342))
        }
    }
