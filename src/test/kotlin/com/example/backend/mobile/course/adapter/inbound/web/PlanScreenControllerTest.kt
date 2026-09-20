package com.example.backend.mobile.course.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 계획 상세 화면 조합(`GET /service/v1/my/plans/{planId}`) 통합 테스트.
 * 픽스처(plan-screen-fixture.sql)는 계획 1(소유자 1, 장소 3곳 중 하나는 삭제된 장소)과 계획 2(타인 소유)를 둔다.
 *
 * 도메인 상세와 달리 장소 이름·카테고리·주소·좌표·대표 이미지가 붙는지, 삭제된 장소는 요약 없이도 자리를 지키는지 본다.
 * 소유권 은닉(없음·타인 동일 4047)은 계획 도메인이 판정하므로 그대로 올라오는지 확인한다.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/plan-screen-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PlanScreenControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @Test
        fun `계획 레코드에 장소 이름·카테고리·좌표·이미지를 붙여 내려준다`() {
            mockMvc
                .perform(screenRequest(PLAN_ID, accessToken(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.planId").value(PLAN_ID))
                .andExpect(jsonPath("$.data.title").value("토요일 성수 데이트"))
                .andExpect(jsonPath("$.data.memo").value("3시 전엔 출발"))
                .andExpect(jsonPath("$.data.plannedDate").value("2026-09-20"))
                .andExpect(jsonPath("$.data.sourceCourseId").doesNotExist())
                .andExpect(jsonPath("$.data.placeCount").value(3))
                .andExpect(jsonPath("$.data.places.length()").value(3))
                // 첫 장소 — 계획 쪽 순서·메모 + 장소 쪽 이름·카테고리·주소·이미지·좌표
                .andExpect(jsonPath("$.data.places[0].placeId").value(1))
                .andExpect(jsonPath("$.data.places[0].orderNo").value(0))
                .andExpect(jsonPath("$.data.places[0].memo").value("웨이팅 있으면 옆집으로"))
                .andExpect(jsonPath("$.data.places[0].name").value("어니언 성수"))
                .andExpect(jsonPath("$.data.places[0].categories[0]").value("CAFE"))
                .andExpect(jsonPath("$.data.places[0].address").value("서울 성동구 아차산로9길 8"))
                .andExpect(jsonPath("$.data.places[0].imageUrl").value("https://cdn/p1.jpg"))
                .andExpect(jsonPath("$.data.places[0].location.latitude").value(37.5445))
                .andExpect(jsonPath("$.data.places[0].location.longitude").value(127.0575))
                // 대표 이미지가 없는 장소는 imageUrl 만 빠진다
                .andExpect(jsonPath("$.data.places[1].name").value("대림창고 갤러리"))
                .andExpect(jsonPath("$.data.places[1].imageUrl").doesNotExist())
                .andExpect(jsonPath("$.data.places[1].memo").doesNotExist())
        }

        @Test
        fun `삭제된 장소는 요약 없이 자리만 지킨다`() {
            mockMvc
                .perform(screenRequest(PLAN_ID, accessToken(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.places[2].placeId").value(3))
                .andExpect(jsonPath("$.data.places[2].orderNo").value(2))
                .andExpect(jsonPath("$.data.places[2].memo").value("삭제된 장소")) // 계획 쪽 메모는 남는다
                .andExpect(jsonPath("$.data.places[2].name").doesNotExist())
                .andExpect(jsonPath("$.data.places[2].location").doesNotExist())
                .andExpect(jsonPath("$.data.places[2].categories.length()").value(0))
        }

        @Test
        fun `없는 계획과 타인 계획은 같은 4047 로 은닉한다`() {
            mockMvc
                .perform(screenRequest(99999L, accessToken(USER_ID)))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4047))

            mockMvc
                .perform(screenRequest(OTHERS_PLAN_ID, accessToken(USER_ID)))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4047))
        }

        /** `/service/v1/my` 하위는 SecurityConfig 경로 매처가 JWT 를 강제한다. */
        @Test
        fun `토큰이 없으면 401을 내려준다`() {
            mockMvc.perform(get("/service/v1/my/plans/$PLAN_ID")).andExpect(status().isUnauthorized)
        }

        @Test
        fun `mock=true 면 DB 조회 없이 고정 화면 목을 내려준다`() {
            mockMvc
                .perform(screenRequest(PLAN_ID, accessToken(USER_ID)).param("mock", "true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.planId").value(1))
                .andExpect(jsonPath("$.data.sourceCourseId").value(12))
                .andExpect(jsonPath("$.data.places.length()").value(2))
                .andExpect(jsonPath("$.data.places[0].name").value("어니언 성수"))
        }

        private fun screenRequest(
            planId: Long,
            token: String,
        ) = get("/service/v1/my/plans/$planId").header(HttpHeaders.AUTHORIZATION, "Bearer $token")

        private fun accessToken(userId: Long) = jwtTokenProvider.issueAccessToken(userId)

        private companion object {
            const val PLAN_ID = 1L
            const val OTHERS_PLAN_ID = 2L
            const val USER_ID = 1L
        }
    }
