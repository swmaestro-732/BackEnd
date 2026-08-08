package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 코스 컨트롤러(`/api/v1/courses`) 통합 테스트 — 생성·상세·임시저장 목록·편집·삭제.
 * 픽스처(course-crud-fixture.sql)로 소유자(1)·타인(2)·장소·코스 4건을 심고,
 * 작성자 식별은 JWT(subject=userId)로 한다.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/course-crud-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        // ─────────────────────────── 생성 (POST /api/v1/courses) ───────────────────────────

        @Test
        fun `발행 코스를 생성하면 201과 courseId를 내려주고 상세로 조회된다`() {
            val body =
                """
                {
                  "title": "새 발행 코스",
                  "description": "성수 카페 코스",
                  "thumbnailUrl": "https://img/new-cover.jpg",
                  "tags": ["감성카페", "데이트"],
                  "visibility": "PUBLIC",
                  "isPublished": true,
                  "places": [
                    {"placeId": 1, "orderNo": 0, "caption": "카페A", "imageUrls": ["https://img/a.jpg"]},
                    {"placeId": 2, "orderNo": 1, "caption": "카페B", "imageUrls": ["https://img/b.jpg"]}
                  ]
                }
                """.trimIndent()

            val response =
                mockMvc
                    .perform(
                        post("/api/v1/courses")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body),
                    ).andExpect(status().isCreated)
                    .andExpect(jsonPath("$.code").value(2000))
                    .andExpect(jsonPath("$.data.courseId").isNumber)
                    .andReturn()

            val courseId = extractCourseId(response.response.contentAsString)

            // 생성된 코스가 상세로 조회되고, 담은 장소(CAFE)들로 카테고리(CAFETOUR)가 도출된다.
            mockMvc
                .perform(get("/api/v1/courses/$courseId"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.course.title").value("새 발행 코스"))
                .andExpect(jsonPath("$.data.course.theme").value("CAFETOUR"))
                .andExpect(jsonPath("$.data.course.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.data.course.stats.placeCount").value(2))
        }

        @Test
        fun `임시저장이어도 장소가 2곳 미만이면 4001을 내려준다`() {
            // 장소 최소 개수(2곳)는 발행 전용이 아니라 임시저장에도 적용된다.
            val body =
                """
                {
                  "title": "임시저장 코스",
                  "visibility": "PRIVATE",
                  "isPublished": false,
                  "places": []
                }
                """.trimIndent()

            mockMvc
                .perform(
                    post("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `발행하려는데 장소가 2곳 미만이면 4001을 내려준다`() {
            val body =
                """
                {
                  "title": "발행 코스",
                  "thumbnailUrl": "https://img/cover.jpg",
                  "visibility": "PUBLIC",
                  "isPublished": true,
                  "places": [
                    {"placeId": 1, "orderNo": 0, "caption": "카페A", "imageUrls": ["https://img/a.jpg"]}
                  ]
                }
                """.trimIndent()

            mockMvc
                .perform(
                    post("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `발행하려는데 커버 이미지가 없으면 4001을 내려준다`() {
            val body =
                """
                {
                  "title": "발행 코스",
                  "visibility": "PUBLIC",
                  "isPublished": true,
                  "places": [
                    {"placeId": 1, "orderNo": 0, "caption": "카페A", "imageUrls": ["https://img/a.jpg"]},
                    {"placeId": 2, "orderNo": 1, "caption": "카페B", "imageUrls": ["https://img/b.jpg"]}
                  ]
                }
                """.trimIndent()

            mockMvc
                .perform(
                    post("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `orderNo가 중복되면 4001을 내려준다`() {
            val body =
                """
                {
                  "title": "임시저장 코스",
                  "visibility": "PUBLIC",
                  "isPublished": false,
                  "places": [
                    {"placeId": 1, "orderNo": 0, "caption": "카페A", "imageUrls": []},
                    {"placeId": 2, "orderNo": 0, "caption": "카페B", "imageUrls": []}
                  ]
                }
                """.trimIndent()

            mockMvc
                .perform(
                    post("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `임시저장은 제목 없이도 생성된다`() {
            // 제목은 발행 코스만 필수 — 임시저장(draft)은 title 키 자체를 생략할 수 있다.
            val body =
                """
                {
                  "visibility": "PRIVATE",
                  "isPublished": false,
                  "places": [
                    {"placeId": 1, "orderNo": 0, "caption": "카페A", "imageUrls": []},
                    {"placeId": 2, "orderNo": 1, "caption": "카페B", "imageUrls": []}
                  ]
                }
                """.trimIndent()

            mockMvc
                .perform(
                    post("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.courseId").isNumber)
        }

        @Test
        fun `발행하려는데 제목이 비어 있으면 4001을 내려준다`() {
            // 커버·장소·사진은 모두 갖추고 제목만 비워 발행 시 제목 필수 규칙만 걸리게 한다.
            val body =
                """
                {
                  "title": "",
                  "thumbnailUrl": "https://img/cover.jpg",
                  "visibility": "PUBLIC",
                  "isPublished": true,
                  "places": [
                    {"placeId": 1, "orderNo": 0, "caption": "카페A", "imageUrls": ["https://img/a.jpg"]},
                    {"placeId": 2, "orderNo": 1, "caption": "카페B", "imageUrls": ["https://img/b.jpg"]}
                  ]
                }
                """.trimIndent()

            mockMvc
                .perform(
                    post("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `생성 mock=true면 DB 저장 없이 고정 courseId를 내려준다`() {
            val body =
                """
                {"title": "무시됨", "visibility": "PUBLIC", "isPublished": false, "places": []}
                """.trimIndent()

            mockMvc
                .perform(
                    post("/api/v1/courses")
                        .param("mock", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.courseId").value(1))
        }

        @Test
        fun `생성 mockError로 4041을 주입한다`() {
            val body =
                """
                {"title": "무시됨", "visibility": "PUBLIC", "isPublished": false, "places": []}
                """.trimIndent()

            mockMvc
                .perform(
                    post("/api/v1/courses")
                        .param("mockError", "4041")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))
        }

        // ─────────────────────────── 임시저장 목록 (GET /api/v1/courses/drafts) ───────────────────────────

        @Test
        fun `내 임시저장 코스만 최근 수정순으로 조회한다`() {
            mockMvc
                .perform(
                    get("/api/v1/courses/drafts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(RECENT_DRAFT_ID))
                .andExpect(jsonPath("$.data[0].authorId").value(OWNER_ID))
                .andExpect(jsonPath("$.data[0].title").value("최근 수정 초안"))
                .andExpect(jsonPath("$.data[1].id").value(PRIVATE_COURSE_ID))
                .andExpect(jsonPath("$.data[1].authorId").value(OWNER_ID))
                .andExpect(jsonPath("$.data[1].title").value("비공개 초안"))
        }

        @Test
        fun `임시저장 목록 mock=true면 DB와 무관하게 목 목록을 내려준다`() {
            mockMvc
                .perform(
                    get("/api/v1/courses/drafts")
                        .param("mock", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(PRIVATE_COURSE_ID))
        }

        // ─────────────────────────── 상세 (GET /api/v1/courses/{id}) ───────────────────────────

        @Test
        fun `공개 코스 상세를 조회한다`() {
            mockMvc
                .perform(get("/api/v1/courses/$PUBLIC_COURSE_ID"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.course.id").value("$PUBLIC_COURSE_ID"))
                .andExpect(jsonPath("$.data.course.title").value("공개 발행 코스"))
                .andExpect(jsonPath("$.data.course.theme").value("CAFETOUR"))
                .andExpect(jsonPath("$.data.course.authorId").value(OWNER_ID))
                .andExpect(jsonPath("$.data.course.places.length()").value(2))
                .andExpect(jsonPath("$.data.course.stats.walkingMinutes").value(5))
                .andExpect(jsonPath("$.data.course.stats.tracingCount").value(1200))
        }

        @Test
        fun `장소별 도보 시간을 요청대로 저장하고 상세에서 내려준다`() {
            // 세 값을 한 번에 검증한다: 정상(7분) · 도보 이동 불가(-1) · 마지막 장소(null).
            // 같은 placeId 를 다시 담아도 되며(orderNo 만 중복 불가) 픽스처 장소 2곳으로 3구간을 만든다.
            val body =
                """
                {
                  "title": "도보 시간 코스",
                  "thumbnailUrl": "https://img/cover.jpg",
                  "visibility": "PUBLIC",
                  "isPublished": true,
                  "places": [
                    {"placeId": 1, "orderNo": 0, "imageUrls": ["https://img/a.jpg"], "walkingMinutes": 7},
                    {"placeId": 2, "orderNo": 1, "imageUrls": ["https://img/b.jpg"], "walkingMinutes": -1},
                    {"placeId": 1, "orderNo": 2, "imageUrls": ["https://img/c.jpg"], "walkingMinutes": null}
                  ]
                }
                """.trimIndent()

            val response =
                mockMvc
                    .perform(
                        post("/api/v1/courses")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body),
                    ).andExpect(status().isCreated)
                    .andReturn()

            mockMvc
                .perform(get("/api/v1/courses/${extractCourseId(response.response.contentAsString)}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.course.places[0].walkingMinutesToNext").value(7))
                // -1(도보 이동 불가)은 눌러 담지 않고 그대로 내려준다 — 프론트가 "도보 이동 불가"로 표시해야 하므로.
                .andExpect(jsonPath("$.data.course.places[1].walkingMinutesToNext").value(-1))
                .andExpect(jsonPath("$.data.course.places[2].walkingMinutesToNext").doesNotExist())
                // 합계는 양수만 — -1 과 null 은 빠져서 7 이다(-1 을 더했다면 6 이 된다).
                .andExpect(jsonPath("$.data.course.stats.walkingMinutes").value(7))
        }

        @Test
        fun `도보 시간이 -2 이하면 4002를 내려준다`() {
            // -1 은 도보 이동 불가 센티널이라 허용하고, 그보다 작은 값만 하한(@Min)으로 막는다.
            val body =
                """
                {
                  "title": "도보 시간 코스",
                  "visibility": "PRIVATE",
                  "isPublished": false,
                  "places": [
                    {"placeId": 1, "orderNo": 0, "imageUrls": [], "walkingMinutes": -2},
                    {"placeId": 2, "orderNo": 1, "imageUrls": []}
                  ]
                }
                """.trimIndent()

            mockMvc
                .perform(
                    post("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        @Test
        fun `walkingMinutes 를 생략하면 null 로 저장된다`() {
            // 선택 필드라 기존 클라이언트 요청(도보 시간 미포함)이 그대로 동작해야 한다.
            val body =
                """
                {
                  "title": "도보 시간 없는 코스",
                  "visibility": "PRIVATE",
                  "isPublished": false,
                  "places": [
                    {"placeId": 1, "orderNo": 0, "imageUrls": []},
                    {"placeId": 2, "orderNo": 1, "imageUrls": []}
                  ]
                }
                """.trimIndent()

            val response =
                mockMvc
                    .perform(
                        post("/api/v1/courses")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body),
                    ).andExpect(status().isCreated)
                    .andReturn()

            mockMvc
                .perform(
                    get("/api/v1/courses/${extractCourseId(response.response.contentAsString)}")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.course.places[0].walkingMinutesToNext").doesNotExist())
                .andExpect(jsonPath("$.data.course.stats.walkingMinutes").value(0))
        }

        @Test
        fun `코스 상세는 해시태그를 함께 내려준다`() {
            // 순서는 계약이 아니라(course_tags 에 순서 컬럼 없음) 구성만 검증한다.
            mockMvc
                .perform(get("/api/v1/courses/$PUBLIC_COURSE_ID"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.course.tags", containsInAnyOrder("감성카페", "데이트")))
        }

        @Test
        fun `태그가 없는 코스는 tags 가 빈 배열이다`() {
            mockMvc
                .perform(
                    get("/api/v1/courses/$PRIVATE_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.course.tags").isArray)
                .andExpect(jsonPath("$.data.course.tags.length()").value(0))
        }

        @Test
        fun `생성 요청의 태그가 상세 조회에 그대로 나온다`() {
            val body =
                """
                {
                  "title": "태그 라운드트립",
                  "thumbnailUrl": "https://img/cover.jpg",
                  "tags": ["데이트", "감성카페"],
                  "visibility": "PUBLIC",
                  "isPublished": true,
                  "places": [
                    {"placeId": 1, "orderNo": 0, "caption": "카페A", "imageUrls": ["https://img/a.jpg"]},
                    {"placeId": 2, "orderNo": 1, "caption": "카페B", "imageUrls": ["https://img/b.jpg"]}
                  ]
                }
                """.trimIndent()

            val response =
                mockMvc
                    .perform(
                        post("/api/v1/courses")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body),
                    ).andExpect(status().isCreated)
                    .andReturn()

            mockMvc
                .perform(get("/api/v1/courses/${extractCourseId(response.response.contentAsString)}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.course.tags", containsInAnyOrder("감성카페", "데이트")))
        }

        @Test
        fun `없는 코스를 조회하면 4041을 내려준다`() {
            mockMvc
                .perform(get("/api/v1/courses/99999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))
        }

        @Test
        fun `비공개 코스는 소유자만 조회하고 타인은 4041을 내려준다`() {
            // 소유자: 조회 성공
            mockMvc
                .perform(
                    get("/api/v1/courses/$PRIVATE_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.course.title").value("비공개 초안"))

            // 타인: 존재를 드러내지 않도록 404
            mockMvc
                .perform(
                    get("/api/v1/courses/$PRIVATE_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OTHER_ID)}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))

            // 비로그인: 404
            mockMvc
                .perform(get("/api/v1/courses/$PRIVATE_COURSE_ID"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))
        }

        @Test
        fun `상세 mock=true면 DB와 무관하게 목 코스를 내려준다`() {
            mockMvc
                .perform(get("/api/v1/courses/99999?mock=true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.course.title").value("비 오는 날 성수 감성 카페 코스"))
        }

        // ─────────────────────────── 편집 (PATCH /api/v1/courses/{id}) ───────────────────────────

        @Test
        fun `소유자가 코스를 편집하면 반영된다`() {
            val body =
                """
                {
                  "title": "수정된 제목",
                  "description": "수정된 설명",
                  "visibility": "PRIVATE",
                  "isPublished": false,
                  "places": [
                    {"placeId": 1, "orderNo": 0, "caption": "카페A", "imageUrls": []},
                    {"placeId": 2, "orderNo": 1, "caption": "카페B", "imageUrls": []}
                  ]
                }
                """.trimIndent()

            mockMvc
                .perform(
                    patch("/api/v1/courses/$PRIVATE_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.courseId").value(PRIVATE_COURSE_ID))

            mockMvc
                .perform(
                    get("/api/v1/courses/$PRIVATE_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.course.title").value("수정된 제목"))
        }

        @Test
        fun `타인의 코스를 편집하면 4041을 내려준다`() {
            val body =
                """
                {"title": "탈취 시도", "visibility": "PUBLIC", "isPublished": false, "places": []}
                """.trimIndent()

            mockMvc
                .perform(
                    patch("/api/v1/courses/$OTHERS_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))
        }

        @Test
        fun `게시된 코스의 장소 구성을 바꾸면 4003을 내려준다`() {
            // 발행 코스(course 1, place 1·2)의 장소를 다른 구성으로 교체 시도 → 장소 불변 위반.
            val body =
                """
                {
                  "title": "공개 발행 코스",
                  "thumbnailUrl": "https://img/cover.jpg",
                  "visibility": "PUBLIC",
                  "isPublished": true,
                  "places": [
                    {"placeId": 1, "orderNo": 0, "caption": "카페A", "imageUrls": ["https://img/a.jpg"]},
                    {"placeId": 999, "orderNo": 1, "caption": "다른 장소", "imageUrls": ["https://img/x.jpg"]}
                  ]
                }
                """.trimIndent()

            mockMvc
                .perform(
                    patch("/api/v1/courses/$PUBLIC_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4003))
        }

        @Test
        fun `편집 mockError로 4041을 주입한다`() {
            val body =
                """
                {"title": "무시됨", "visibility": "PUBLIC", "isPublished": false, "places": []}
                """.trimIndent()

            mockMvc
                .perform(
                    patch("/api/v1/courses/$PRIVATE_COURSE_ID")
                        .param("mockError", "4041")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))
        }

        // ─────────────────────────── 삭제 (DELETE /api/v1/courses/{id}) ───────────────────────────

        @Test
        fun `소유자가 코스를 삭제하면 이후 조회에서 제외된다`() {
            mockMvc
                .perform(
                    delete("/api/v1/courses/$PUBLIC_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data").doesNotExist())

            mockMvc
                .perform(get("/api/v1/courses/$PUBLIC_COURSE_ID"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))
        }

        @Test
        fun `타인의 코스를 삭제하면 4041을 내려주고 삭제되지 않는다`() {
            mockMvc
                .perform(
                    delete("/api/v1/courses/$OTHERS_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4041))

            // 소유자(2)에게는 여전히 조회된다 — 삭제되지 않았다.
            mockMvc
                .perform(
                    get("/api/v1/courses/$OTHERS_COURSE_ID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OTHER_ID)}"),
                ).andExpect(status().isOk)
        }

        @Test
        fun `삭제 mock=true면 실제 코스를 삭제하지 않는다`() {
            mockMvc
                .perform(
                    delete("/api/v1/courses/$PUBLIC_COURSE_ID")
                        .param("mock", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(OWNER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))

            mockMvc
                .perform(get("/api/v1/courses/$PUBLIC_COURSE_ID"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.course.id").value("$PUBLIC_COURSE_ID"))
        }

        private fun tokenFor(userId: Long) = jwtTokenProvider.issueAccessToken(userId)

        /** 생성 응답 본문에서 data.courseId 를 추출한다(정규식 — 얇은 JSON 파싱 의존 회피). */
        private fun extractCourseId(json: String): Long =
            Regex(""""courseId"\s*:\s*(\d+)""")
                .find(json)
                ?.groupValues
                ?.get(1)
                ?.toLong()
                ?: error("응답에서 courseId 를 찾지 못했습니다: $json")

        private companion object {
            const val OWNER_ID = 1L
            const val OTHER_ID = 2L
            const val PUBLIC_COURSE_ID = 1L
            const val PRIVATE_COURSE_ID = 2L
            const val OTHERS_COURSE_ID = 3L
            const val RECENT_DRAFT_ID = 4L
        }
    }
