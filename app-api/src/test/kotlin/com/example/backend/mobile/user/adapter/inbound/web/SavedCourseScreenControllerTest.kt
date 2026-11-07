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
 * 저장함 · 코스 탭 화면 조합(BFF) 컨트롤러(`GET /service/v1/my/saved-courses`) 통합 테스트.
 * 저장 레코드(user) + 코스 요약(course) + 작성자 프로필(user) + 장소 상세(place) + 완주(trace) + 폴더 조합을 검증한다.
 * `/service/v1/my` 하위는 JWT 필수라 대부분 로그인 사용자(1) 토큰으로 호출한다.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/saved-course-screen-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SavedCourseScreenControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @Test
        fun `저장 코스를 코스+작성자+장소+완주+폴더로 조합해 최신 저장순으로 내려준다`() {
            mockMvc
                .perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                // 카운트 칩: 전체 2 = 안 가봄 1 + 완주 1
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.uncompletedCount").value(1))
                .andExpect(jsonPath("$.data.completedCount").value(1))
                // 폴더 칩(order_no 순, 폴더별 저장 개수)
                .andExpect(jsonPath("$.data.folders.length()").value(2))
                .andExpect(jsonPath("$.data.folders[0].id").value(1))
                .andExpect(jsonPath("$.data.folders[0].name").value("데이트"))
                .andExpect(jsonPath("$.data.folders[0].count").value(1))
                .andExpect(jsonPath("$.data.folders[1].name").value("혼자 걷기"))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty)
                // id 내림차순 → [한남(saved 2), 성수(saved 1)]
                .andExpect(jsonPath("$.data.savedCourses.length()").value(2))
                // 0) 한남 갤러리 코스 — 내 코스(작성자=조회자), 완주
                .andExpect(jsonPath("$.data.savedCourses[0].id").value(2))
                .andExpect(jsonPath("$.data.savedCourses[0].courseId").value(2))
                .andExpect(jsonPath("$.data.savedCourses[0].folderId").value(2))
                .andExpect(jsonPath("$.data.savedCourses[0].isMine").value(true))
                .andExpect(jsonPath("$.data.savedCourses[0].completed").value(true))
                .andExpect(jsonPath("$.data.savedCourses[0].completedAt").isNotEmpty)
                .andExpect(jsonPath("$.data.savedCourses[0].course.title").value("한남 갤러리 코스"))
                .andExpect(jsonPath("$.data.savedCourses[0].course.area").value("한남"))
                .andExpect(jsonPath("$.data.savedCourses[0].course.theme").value("CULTURE"))
                .andExpect(jsonPath("$.data.savedCourses[0].course.placeCount").value(2))
                // 도보 시간 합(10) → "약 10분"
                .andExpect(jsonPath("$.data.savedCourses[0].course.durationText").value("약 10분"))
                // 작성자: 닉네임·핸들·프로필
                .andExpect(jsonPath("$.data.savedCourses[0].course.author.id").value(1))
                .andExpect(jsonPath("$.data.savedCourses[0].course.author.handle").value("hyunwoo"))
                .andExpect(jsonPath("$.data.savedCourses[0].course.author.nickname").value("현우"))
                .andExpect(
                    jsonPath("$.data.savedCourses[0].course.author.profileImageUrl").value("https://img/hyunwoo.jpg"),
                )
                // 장소 상세 + 지도 핀 좌표
                .andExpect(jsonPath("$.data.savedCourses[0].course.places.length()").value(2))
                .andExpect(jsonPath("$.data.savedCourses[0].course.places[0].placeId").value(3))
                .andExpect(jsonPath("$.data.savedCourses[0].course.places[0].name").value("리움미술관"))
                .andExpect(jsonPath("$.data.savedCourses[0].course.places[0].category").value("CULTURE"))
                .andExpect(jsonPath("$.data.savedCourses[0].course.places[0].caption").value("리움에서 시작"))
                .andExpect(
                    jsonPath("$.data.savedCourses[0].course.places[0].imageUrls[0]").value("https://img/c2p0-a.jpg"),
                ).andExpect(jsonPath("$.data.savedCourses[0].course.places[0].location.latitude").value(37.5385))
                .andExpect(jsonPath("$.data.savedCourses[0].course.places[0].location.longitude").value(127.0))
                // 1) 성수 카페 코스 — 남의 코스, 안 가봄
                .andExpect(jsonPath("$.data.savedCourses[1].courseId").value(1))
                .andExpect(jsonPath("$.data.savedCourses[1].isMine").value(false))
                .andExpect(jsonPath("$.data.savedCourses[1].completed").value(false))
                .andExpect(jsonPath("$.data.savedCourses[1].completedAt").isEmpty)
                .andExpect(jsonPath("$.data.savedCourses[1].course.durationText").value("약 6분"))
                .andExpect(jsonPath("$.data.savedCourses[1].course.author.handle").value("jiho_routes"))
                .andExpect(jsonPath("$.data.savedCourses[1].course.author.nickname").value("지호"))
                // 코스 사진(course_place_images) 여러 장이 리스트로 — 장소 자체 이미지가 아님
                .andExpect(jsonPath("$.data.savedCourses[1].course.places[0].name").value("어니언 성수"))
                .andExpect(jsonPath("$.data.savedCourses[1].course.places[0].imageUrls.length()").value(2))
                .andExpect(
                    jsonPath("$.data.savedCourses[1].course.places[0].imageUrls[0]").value("https://img/c1p0-a.jpg"),
                ).andExpect(
                    jsonPath("$.data.savedCourses[1].course.places[0].imageUrls[1]").value("https://img/c1p0-b.jpg"),
                )
        }

        @Test
        fun `완주 필터로 완주한 코스만 내려준다`() {
            mockMvc
                .perform(get("$PATH?completed=true").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.savedCourses.length()").value(1))
                .andExpect(jsonPath("$.data.savedCourses[0].courseId").value(2))
                .andExpect(jsonPath("$.data.savedCourses[0].completed").value(true))
        }

        @Test
        fun `완주 필터로 안 가본 코스만 내려준다`() {
            mockMvc
                .perform(get("$PATH?completed=false").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.savedCourses.length()").value(1))
                .andExpect(jsonPath("$.data.savedCourses[0].courseId").value(1))
                .andExpect(jsonPath("$.data.savedCourses[0].completed").value(false))
        }

        @Test
        fun `폴더 필터로 해당 폴더의 저장 코스만 내려준다`() {
            mockMvc
                .perform(get("$PATH?folderId=1").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.savedCourses.length()").value(1))
                .andExpect(jsonPath("$.data.savedCourses[0].courseId").value(1))
                .andExpect(jsonPath("$.data.savedCourses[0].folderId").value(1))
        }

        @Test
        fun `토큰이 없으면 401 을 내려준다`() {
            mockMvc
                .perform(get(PATH))
                .andExpect(status().isUnauthorized)
        }

        @Test
        fun `size 가 범위를 벗어나면 400 을 내려준다`() {
            mockMvc
                .perform(get("$PATH?size=100").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `mock=true 면 DB 와 무관하게 고정 목을 내려준다`() {
            mockMvc
                .perform(get("$PATH?mock=true").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.savedCourses.length()").value(4))
        }

        private fun bearer(userId: Long) = "Bearer ${jwtTokenProvider.issueAccessToken(userId)}"

        private companion object {
            const val PATH = "/service/v1/my/saved-courses"
            const val USER_ID = 1L
        }
    }
