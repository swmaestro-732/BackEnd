package com.example.backend.mobile.user.adapter.inbound.web

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
 * 저장함 · 장소 탭 화면 조합(BFF) 컨트롤러(`GET /service/v1/my/saved-places`) 통합 테스트.
 * 저장 레코드·배지 카운트(user) + 장소 요약(place) + 지역 이름(area) 조합을 검증한다.
 * `/service/v1/my` 하위는 JWT 필수라 로그인 사용자(1) 토큰으로 호출한다.
 *
 * 픽스처(saved-place-screen-fixture.sql) 요지: 저장 레코드 5건(살아있는 것) + 소프트 삭제 1건,
 * 그중 하나는 삭제된 장소를 가리켜 **카운트엔 들어가지만 목록 항목에선 빠진다**.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/saved-place-screen-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SavedPlaceScreenControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @Test
        fun `미방문 저장을 장소 요약 + 지역 이름과 조합해 최신 저장순으로 내려준다`() {
            mockMvc
                .perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                // 배지 카운트: 전체 5 = 미방문 4 + 방문 1. 조회 필터와 무관한 전체 기준이다.
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.unvisitedCount").value(4))
                .andExpect(jsonPath("$.data.visitedCount").value(1))
                // 카테고리 칩: 개수 내림차순(CAFE 3 → CULTURE 1). 미분류(null)는 빠진다.
                .andExpect(jsonPath("$.data.categoryCounts.length()").value(2))
                .andExpect(jsonPath("$.data.categoryCounts[0].category").value("CAFE"))
                .andExpect(jsonPath("$.data.categoryCounts[0].count").value(3))
                .andExpect(jsonPath("$.data.categoryCounts[1].category").value("CULTURE"))
                .andExpect(jsonPath("$.data.categoryCounts[1].count").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty)
                // 미방문 저장 레코드는 4건(5·4·3·2)이지만 5번은 장소가 삭제돼 항목에서 빠진다 → 3건.
                .andExpect(jsonPath("$.data.savedPlaces.length()").value(3))
                // 0) 저장 4 — 카테고리 미분류(null), 장소 area_code 없음 → area null, 이미지 없음
                .andExpect(jsonPath("$.data.savedPlaces[0].id").value(4))
                .andExpect(jsonPath("$.data.savedPlaces[0].placeId").value(4))
                .andExpect(jsonPath("$.data.savedPlaces[0].category").isEmpty)
                .andExpect(jsonPath("$.data.savedPlaces[0].visited").value(false))
                .andExpect(jsonPath("$.data.savedPlaces[0].savedAt").isNotEmpty)
                .andExpect(jsonPath("$.data.savedPlaces[0].place.name").value("지역미상 바"))
                .andExpect(jsonPath("$.data.savedPlaces[0].place.category").value("BAR"))
                .andExpect(jsonPath("$.data.savedPlaces[0].place.area").isEmpty)
                .andExpect(jsonPath("$.data.savedPlaces[0].place.imageUrl").isEmpty)
                // 1) 저장 3 — 한남동 장소. 지역 이름은 area 도메인이 법정동코드로 해석한 읍면동 이름이다.
                .andExpect(jsonPath("$.data.savedPlaces[1].id").value(3))
                .andExpect(jsonPath("$.data.savedPlaces[1].category").value("CULTURE"))
                .andExpect(jsonPath("$.data.savedPlaces[1].place.name").value("리움미술관"))
                .andExpect(jsonPath("$.data.savedPlaces[1].place.area").value("한남동"))
                .andExpect(jsonPath("$.data.savedPlaces[1].place.location.latitude").value(37.5385))
                .andExpect(jsonPath("$.data.savedPlaces[1].place.location.longitude").value(127.0))
                // 2) 저장 2 — 성수동1가 장소
                .andExpect(jsonPath("$.data.savedPlaces[2].id").value(2))
                .andExpect(jsonPath("$.data.savedPlaces[2].category").value("CAFE"))
                .andExpect(jsonPath("$.data.savedPlaces[2].place.name").value("센터커피 성수"))
                .andExpect(jsonPath("$.data.savedPlaces[2].place.area").value("성수동1가"))
                .andExpect(jsonPath("$.data.savedPlaces[2].place.imageUrl").value("https://img/center.jpg"))
        }

        @Test
        fun `거리(walkingTime)는 1차 구현 범위 밖이라 항상 null 이다`() {
            mockMvc
                .perform(
                    get("$PATH?userLat=37.5445&userLng=127.0578").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)),
                ).andExpect(status().isOk)
                // 위치를 넘겨도(계약상 받기만 한다) 도보 시간은 내려가지 않는다.
                .andExpect(jsonPath("$.data.savedPlaces[0].place.walkingTime").isEmpty)
                .andExpect(jsonPath("$.data.savedPlaces[1].place.walkingTime").isEmpty)
        }

        @Test
        fun `visited=true 면 방문한 저장만 내려준다`() {
            mockMvc
                .perform(get("$PATH?visited=true").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.savedPlaces.length()").value(1))
                .andExpect(jsonPath("$.data.savedPlaces[0].id").value(1))
                .andExpect(jsonPath("$.data.savedPlaces[0].visited").value(true))
                .andExpect(jsonPath("$.data.savedPlaces[0].place.name").value("어니언 성수"))
                .andExpect(jsonPath("$.data.savedPlaces[0].place.area").value("성수동1가"))
                // 배지 카운트는 탭을 바꿔도 같은 전체 기준 값이다.
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.unvisitedCount").value(4))
                .andExpect(jsonPath("$.data.visitedCount").value(1))
        }

        @Test
        fun `카테고리 칩으로 필터링해도 배지 카운트는 전체 기준이다`() {
            mockMvc
                .perform(get("$PATH?category=CAFE").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isOk)
                // 미방문 CAFE 저장은 5·2 지만 5번은 장소가 삭제됨 → 2번만 남는다.
                .andExpect(jsonPath("$.data.savedPlaces.length()").value(1))
                .andExpect(jsonPath("$.data.savedPlaces[0].id").value(2))
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.categoryCounts[0].count").value(3))
        }

        @Test
        fun `페이지 메타는 저장 레코드 기준이라 장소 해석으로 빠진 항목에 영향받지 않는다`() {
            mockMvc
                .perform(get("$PATH?size=2").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isOk)
                // 저장 레코드 2건(5·4)을 잘라내 hasNext·nextCursor 를 만든다 — 커서는 레코드 id(4).
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("4"))
                // 그중 5번은 장소가 삭제돼 항목은 1건뿐이지만, 커서는 위와 같이 유지된다(다음 페이지가 어긋나지 않게).
                .andExpect(jsonPath("$.data.savedPlaces.length()").value(1))
                .andExpect(jsonPath("$.data.savedPlaces[0].id").value(4))
        }

        @Test
        fun `커서를 주면 그보다 이전에 저장한 것만 내려준다`() {
            mockMvc
                .perform(get("$PATH?cursor=3").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.savedPlaces.length()").value(1))
                .andExpect(jsonPath("$.data.savedPlaces[0].id").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false))
        }

        @Test
        fun `소프트 삭제된 저장은 목록과 카운트 어디에도 없다`() {
            // 픽스처의 저장 6(장소 3 을 재저장했다가 취소)이 집계되면 total 6·CULTURE 2·항목 4건이 된다.
            mockMvc
                .perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.categoryCounts[1].category").value("CULTURE"))
                .andExpect(jsonPath("$.data.categoryCounts[1].count").value(1))
                .andExpect(jsonPath("$.data.savedPlaces.length()").value(3))
                // 최신 저장순 첫 항목이 6 이 아니라 4 다(6 은 취소된 저장).
                .andExpect(jsonPath("$.data.savedPlaces[0].id").value(4))
        }

        @Test
        fun `알 수 없는 카테고리 이름이면 400 을 내려준다`() {
            mockMvc
                .perform(get("$PATH?category=NOT_A_CATEGORY").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `커서 형식이 잘못되면 400 을 내려준다`() {
            mockMvc
                .perform(get("$PATH?cursor=not-a-number").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `size 가 범위를 벗어나면 400 을 내려준다`() {
            mockMvc
                .perform(get("$PATH?size=100").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `토큰이 없으면 401 을 내려준다`() {
            mockMvc
                .perform(get(PATH))
                .andExpect(status().isUnauthorized)
        }

        @Test
        fun `mock=true 면 DB 와 무관하게 고정 목을 내려준다`() {
            mockMvc
                .perform(get("$PATH?mock=true").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.savedPlaces.length()").value(5))
                // 목은 거리 텍스트를 그대로 유지한다(프론트 계약 확인용).
                .andExpect(jsonPath("$.data.savedPlaces[0].place.walkingTime").value("도보 11분"))
        }

        private fun bearer(userId: Long) = "Bearer ${jwtTokenProvider.issueAccessToken(userId)}"

        private companion object {
            const val PATH = "/service/v1/my/saved-places"
            const val USER_ID = 1L
        }
    }
