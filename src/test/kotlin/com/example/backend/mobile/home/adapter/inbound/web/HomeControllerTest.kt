package com.example.backend.mobile.home.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import com.jayway.jsonpath.JsonPath
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
 * 홈 화면(BFF) 컨트롤러 통합 테스트 — `GET /service/v2/home` 과 추천 코스 전체보기 `GET /service/v2/home/recommended-courses`.
 * 추천 코스는 PUBLIC 발행 코스만 저장수 내림차순, 최신순으로 랭킹되고 카드 메타(지역, 장소 수, 총 도보 시간)가 붙는다
 * (course-feed-fixture.sql). 테스트 환경엔 검색엔진이 없어 근처 저장 장소는 fail-soft 로 빈 배열이어야 한다.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/course-feed-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class HomeControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @Test
        fun `비로그인 홈은 프로필 없이 추천 코스를 카드 메타와 함께 내려주고 근처 저장 장소는 비어 있다`() {
            mockMvc
                .perform(get(HOME_PATH).param("userLat", "37.544").param("userLng", "127.056"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.profile").isEmpty)
                // PUBLIC 발행 3건만(PRIVATE, FOLLOWER, 미발행 제외), 저장수 우선
                .andExpect(jsonPath("$.data.recommendedCourses.length()").value(3))
                .andExpect(jsonPath("$.data.recommendedCourses[0].id").value(2))
                .andExpect(jsonPath("$.data.recommendedCourses[0].title").value("공개 인기 코스"))
                .andExpect(jsonPath("$.data.recommendedCourses[0].theme").value("CAFETOUR"))
                .andExpect(jsonPath("$.data.recommendedCourses[0].area").value("성수"))
                .andExpect(jsonPath("$.data.recommendedCourses[0].placeCount").value(3))
                .andExpect(jsonPath("$.data.recommendedCourses[0].walkingMinutes").value(12))
                .andExpect(jsonPath("$.data.recommendedCourses[0].savesCnt").value(5))
                // 장소 없는 코스는 0곳, 0분
                .andExpect(jsonPath("$.data.recommendedCourses[1].id").value(1))
                .andExpect(jsonPath("$.data.recommendedCourses[1].placeCount").value(0))
                .andExpect(jsonPath("$.data.recommendedCourses[1].walkingMinutes").value(0))
                .andExpect(jsonPath("$.data.recommendedCourses[2].id").value(6))
                .andExpect(jsonPath("$.data.nearbySavedPlaces.length()").value(0))
        }

        @Test
        fun `로그인 홈은 프로필을 채우고 검색엔진 장애 시 근처 저장 장소만 빈 배열로 내려준다`() {
            mockMvc
                .perform(
                    get(HOME_PATH)
                        .param("userLat", "37.544")
                        .param("userLng", "127.056")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1L)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.profile.nickname").value("작성자"))
                .andExpect(jsonPath("$.data.recommendedCourses.length()").value(3))
                // 저장 장소 2곳이 있지만 검색엔진이 없어(503) 이 섹션만 비고 홈 전체는 성공한다.
                .andExpect(jsonPath("$.data.nearbySavedPlaces.length()").value(0))
        }

        @Test
        fun `좌표를 한쪽만 보내면 400 이다`() {
            mockMvc
                .perform(get(HOME_PATH).param("userLat", "37.544"))
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `mock=true면 DB와 무관하게 피그마 예시 홈 목을 내려준다`() {
            mockMvc
                .perform(get("$HOME_PATH?mock=true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.profile.nickname").isNotEmpty)
                .andExpect(jsonPath("$.data.recommendedCourses.length()").value(2))
                .andExpect(jsonPath("$.data.recommendedCourses[0].title").value("비 오는 날 성수 감성 카페 코스"))
                .andExpect(jsonPath("$.data.recommendedCourses[0].area").value("성수"))
                .andExpect(jsonPath("$.data.recommendedCourses[0].placeCount").value(4))
                .andExpect(jsonPath("$.data.recommendedCourses[1].title").value("연남동 골목 브런치 산책"))
                .andExpect(jsonPath("$.data.nearbySavedPlaces.length()").value(2))
                .andExpect(jsonPath("$.data.nearbySavedPlaces[0].name").value("어니언 성수"))
                .andExpect(jsonPath("$.data.nearbySavedPlaces[0].rating").value(4.8))
                .andExpect(jsonPath("$.data.nearbySavedPlaces[0].walkingMinutes").value(6))
                .andExpect(jsonPath("$.data.nearbySavedPlaces[0].visited").value(false))
                .andExpect(jsonPath("$.data.nearbySavedPlaces[1].name").value("대림창고"))
        }

        @Test
        fun `전체보기는 size로 첫 페이지를 조회하고 복합 커서로 중복 없이 다음 페이지를 잇는다`() {
            val firstPageBody =
                mockMvc
                    .perform(get(RECOMMENDED_PATH).param("size", "2"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.code").value(2000))
                    .andExpect(jsonPath("$.data.courses.length()").value(2))
                    .andExpect(jsonPath("$.data.courses[0].id").value(2))
                    .andExpect(jsonPath("$.data.courses[0].placeCount").value(3))
                    .andExpect(jsonPath("$.data.courses[0].walkingMinutes").value(12))
                    .andExpect(jsonPath("$.data.courses[1].id").value(1))
                    .andExpect(jsonPath("$.data.hasNext").value(true))
                    .andExpect(jsonPath("$.data.nextCursor").isNotEmpty)
                    .andReturn()
                    .response.contentAsString
            val nextCursor: String = JsonPath.read(firstPageBody, "$.data.nextCursor")

            mockMvc
                .perform(
                    get(RECOMMENDED_PATH)
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
        fun `전체보기에 잘못된 커서면 4001을 내려준다`() {
            mockMvc
                .perform(get(RECOMMENDED_PATH).param("cursor", "not-a-feed-cursor"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `전체보기 size가 범위를 벗어나면 4002를 내려준다`() {
            mockMvc
                .perform(get(RECOMMENDED_PATH).param("size", "0"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        private fun bearer(userId: Long) = "Bearer ${jwtTokenProvider.issueAccessToken(userId)}"

        private companion object {
            const val HOME_PATH = "/service/v2/home"
            const val RECOMMENDED_PATH = "/service/v2/home/recommended-courses"
        }
    }
