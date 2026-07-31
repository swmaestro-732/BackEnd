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
 * 코스 상세 화면 조합(BFF) 컨트롤러(`/service/v1/courses/{id}`) 통합 테스트.
 * 코스 CRUD 와 같은 픽스처(course-crud-fixture.sql)를 재사용한다 —
 * 코스 상세([CourseUseCase]) + 작성자 프로필([UserUseCase]) + 장소 요약([PlaceQueryUseCase]) 조합을 검증한다.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/course-crud-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseMobileControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @Test
        fun `공개 코스 화면을 코스+작성자+장소로 조합해 내려준다`() {
            mockMvc
                .perform(get("/service/v1/courses/$PUBLIC_COURSE_ID"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                // 코스 상세(course 도메인)
                .andExpect(jsonPath("$.data.course.id").value("$PUBLIC_COURSE_ID"))
                .andExpect(jsonPath("$.data.course.title").value("공개 발행 코스"))
                .andExpect(jsonPath("$.data.course.themes[0]").value("CAFETOUR"))
                .andExpect(jsonPath("$.data.course.stats.placeCount").value(2))
                .andExpect(jsonPath("$.data.course.stats.walkingMinutes").value(5))
                .andExpect(jsonPath("$.data.course.stats.tracingCount").value(1200))
                .andExpect(jsonPath("$.data.course.viewer.hasSaved").value(false))
                // 작성자 프로필(user 도메인) — 비로그인이라 팔로우 관계는 false, handle/이미지 없으면 빈 문자열
                .andExpect(jsonPath("$.data.course.author.id").value(OWNER_ID))
                .andExpect(jsonPath("$.data.course.author.nickname").value("코스작성자"))
                .andExpect(jsonPath("$.data.course.author.handle").value(""))
                .andExpect(jsonPath("$.data.course.author.isFollowing").value(false))
                .andExpect(jsonPath("$.data.course.author.isFollower").value(false))
                // 장소 이름·카테고리(place 도메인)가 코스 장소와 결합된다
                .andExpect(jsonPath("$.data.course.places.length()").value(2))
                .andExpect(jsonPath("$.data.course.places[0].placeId").value(1))
                .andExpect(jsonPath("$.data.course.places[0].name").value("카페A"))
                .andExpect(jsonPath("$.data.course.places[0].caption").value("카페A"))
                .andExpect(jsonPath("$.data.course.places[0].walkingMinutesToNext").value(5))
                .andExpect(jsonPath("$.data.course.places[0].categories[0]").value("CAFE"))
                .andExpect(jsonPath("$.data.course.places[0].images[0].imageUrl").value("https://img/place-a.jpg"))
                // 장소 좌표(place 도메인 location)가 지도 핀용으로 함께 내려온다 — 카페A: POINT(127.05 37.54)
                .andExpect(jsonPath("$.data.course.places[0].location.latitude").value(37.54))
                .andExpect(jsonPath("$.data.course.places[0].location.longitude").value(127.05))
                .andExpect(jsonPath("$.data.course.places[1].placeId").value(2))
                .andExpect(jsonPath("$.data.course.places[1].name").value("카페B"))
                .andExpect(jsonPath("$.data.course.places[1].location.latitude").value(37.55))
                .andExpect(jsonPath("$.data.course.places[1].location.longitude").value(127.06))
                // 리뷰 조회 유스케이스 도입 전까지 실 응답의 리뷰 요약은 null 이다(목 값 노출 금지)
                .andExpect(jsonPath("$.data.reviewSummary").isEmpty)
        }

        @Test
        fun `없는 코스를 조회하면 4041을 내려준다`() {
            mockMvc
                .perform(get("/service/v1/courses/99999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))
        }

        @Test
        fun `비공개 코스는 소유자만 조합해 조회하고 타인·비로그인은 4041을 내려준다`() {
            // 소유자: 조회 성공(장소 없는 초안)
            mockMvc
                .perform(
                    get("/service/v1/courses/$PRIVATE_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.course.title").value("비공개 초안"))
                .andExpect(jsonPath("$.data.course.places.length()").value(0))
                .andExpect(jsonPath("$.data.course.author.id").value(OWNER_ID))

            // 타인: 존재를 드러내지 않도록 404
            mockMvc
                .perform(
                    get("/service/v1/courses/$PRIVATE_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OTHER_ID)}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))

            // 비로그인: 404
            mockMvc
                .perform(get("/service/v1/courses/$PRIVATE_COURSE_ID"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))
        }

        @Test
        fun `mock=true면 DB와 무관하게 고정 화면 목을 내려준다`() {
            mockMvc
                .perform(get("/service/v1/courses/99999?mock=true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.course.title").value("비 오는 날 성수 감성 카페 코스"))
                .andExpect(jsonPath("$.data.course.stats.tracingCount").value(1200))
                .andExpect(jsonPath("$.data.reviewSummary.totalCount").value(6))
        }

        @Test
        fun `mockError로 4041을 주입한다`() {
            mockMvc
                .perform(get("/service/v1/courses/$PUBLIC_COURSE_ID?mockError=4041"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))
        }

        private fun tokenFor(userId: Long) = jwtTokenProvider.issueAccessToken(userId)

        private companion object {
            const val OWNER_ID = 1L
            const val OTHER_ID = 2L
            const val PUBLIC_COURSE_ID = 1L
            const val PRIVATE_COURSE_ID = 2L
        }
    }
