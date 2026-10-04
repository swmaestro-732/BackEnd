package com.example.backend.mobile.place.adapter.inbound.web

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
 * 장소 상세(BFF) 컨트롤러(`GET /service/v1/places/{placeId}`) 통합 테스트.
 * place 도메인 인바운드 포트로 장소를 조회해 화면을 조합한다 — 리뷰·이 근처 코스·저장 여부는
 * 백엔드가 없어 빈/false 스텁으로 내려간다(place-detail-screen-fixture.sql).
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/place-detail-screen-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PlaceDetailScreenControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `심어둔 장소는 200으로 조회되고 리뷰·이 근처 코스·저장 여부는 스텁으로 내려간다`() {
            mockMvc
                .perform(get("/service/v1/places/501"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.place.name").value("어니언 성수"))
                .andExpect(jsonPath("$.data.place.reviewSummary.totalCount").value(0))
                .andExpect(jsonPath("$.data.place.viewer.hasSaved").value(false))
                .andExpect(jsonPath("$.data.nearbyCourses.length()").value(0))
        }

        @Test
        fun `없는 장소면 4043 에러`() {
            mockMvc
                .perform(get("/service/v1/places/999999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4043))
        }

        @Test
        fun `mock=true면 DB와 무관하게 고정 장소 목을 내려준다`() {
            mockMvc
                .perform(get("/service/v1/places/999999?mock=true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.place.name").value("어니언 성수"))
        }
    }
