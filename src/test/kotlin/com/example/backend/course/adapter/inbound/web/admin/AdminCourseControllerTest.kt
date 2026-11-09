package com.example.backend.course.adapter.inbound.web.admin

import com.example.backend.admin.application.port.outbound.AdminTokenPort
import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.course.application.event.CourseDeletedEvent
import com.example.backend.course.application.event.CourseSavedEvent
import com.example.backend.support.IntegrationTestBase
import com.jayway.jsonpath.JsonPath
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 어드민 코스 관리 API 통합 테스트(SCRUM-752). 픽스처(admin-course-fixture)의 코스:
 * 1 사용자 공개, 2 큐레이터 공개, 3 사용자 초안, 4 큐레이터 숨김, 5 사용자 삭제.
 * 색인은 이벤트의 공개 여부로 저장과 삭제를 고르므로(CourseSearchSyncListenerTest) 발행된 이벤트의 코스 상태를 본다.
 */
@AutoConfigureMockMvc
@RecordApplicationEvents
@Sql(scripts = ["/sql/admin-course-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminCourseControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val adminTokenPort: AdminTokenPort,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jdbcTemplate: JdbcTemplate,
    ) : IntegrationTestBase() {
        @Autowired
        private lateinit var events: ApplicationEvents

        @Test
        fun `앱 access token 은 403 으로 거부한다`() {
            mockMvc
                .perform(
                    get(
                        "/admin/v1/courses",
                    ).header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(1L)}"),
                ).andExpect(status().isForbidden)
        }

        @Test
        fun `목록은 기본으로 삭제되지 않은 코스를 id 내림차순으로 주고 상태 필터를 적용한다`() {
            mockMvc
                .perform(admin(get("/admin/v1/courses")))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.items[*].id", contains(4, 3, 2, 1)))
                .andExpect(jsonPath("$.data.items[3].author.nickname").value("일반작성자"))
                .andExpect(jsonPath("$.data.items[3].placeCount").value(2))
                .andExpect(jsonPath("$.data.items[3].areaName").value("성수"))
                .andExpect(jsonPath("$.data.items[3].likesCnt").value(3))
                .andExpect(jsonPath("$.data.items[3].pendingReportCount").value(0))

            assertIds("status=ACTIVE", 2, 1)
            assertIds("status=DRAFT", 3)
            assertIds("status=HIDDEN", 4)
            assertIds("status=DELETED", 5)
            mockMvc
                .perform(admin(get("/admin/v1/courses?status=DELETED")))
                .andExpect(jsonPath("$.data.items[0].status").value("DELETED"))
        }

        @Test
        fun `목록은 작성자 구분 지역 테마 검색어로 거르고 페이지를 나눈다`() {
            assertIds("author=CURATOR", 4, 2)
            assertIds("author=USER", 3, 1)
            assertIds("areaCode=11200", 1)
            assertIds("theme=DATE", 2)
            assertIds("q=큐레이터", 4, 2) // 제목 또는 작성자 닉네임
            assertIds("q=초안", 3)
            assertIds("q=1", 1) // 정확한 코스 id
            mockMvc
                .perform(admin(get("/admin/v1/courses?page=1&size=3")))
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.size").value(3))
                .andExpect(jsonPath("$.data.items[*].id", contains(1)))
        }

        @Test
        fun `검색어의 LIKE 와일드카드는 문자 그대로 찾는다`() {
            jdbcTemplate.update("UPDATE courses SET title = '할인 100% 코스' WHERE id = 3")

            assertIds("q=%", 3)
            assertIds("q=_")
            assertIds("q=\\")
        }

        @Test
        fun `상세는 삭제된 코스도 장소 정보와 함께 돌려준다`() {
            mockMvc
                .perform(admin(get("/admin/v1/courses/2")))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.companions", contains("DATE")))
                .andExpect(jsonPath("$.data.tags", contains("성수")))
                .andExpect(jsonPath("$.data.author.id").value(2))
                .andExpect(jsonPath("$.data.places[0].name").value("카페A"))
                .andExpect(jsonPath("$.data.places[0].category").value("CAFE"))
                .andExpect(jsonPath("$.data.places[0].address").value("서울 성수동 1"))
                .andExpect(jsonPath("$.data.places[0].latitude").value(37.54))
                .andExpect(jsonPath("$.data.places[0].imageUrls[0]").value("https://img/p3.jpg"))
                .andExpect(jsonPath("$.data.places[0].walkingMinutes").value(7))

            mockMvc
                .perform(admin(get("/admin/v1/courses/5")))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.status").value("DELETED"))
        }

        @Test
        fun `큐레이터 코스는 발행 상태에서도 장소까지 수정하고 감사 기록을 남긴다`() {
            val updatedAt = currentUpdatedAt(2)

            mockMvc
                .perform(
                    admin(
                        patch("/admin/v1/courses/2"),
                    ).json(editBody(updatedAt, title = "고친 큐레이터 코스", reversed = true)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.title").value("고친 큐레이터 코스"))
                .andExpect(jsonPath("$.data.places[0].placeId").value(2))
                .andExpect(jsonPath("$.data.places[1].placeId").value(1))

            assertEquals(listOf("UPDATE"), actions(2))
        }

        @Test
        fun `사용자 코스 수정은 403 4101 이고 updatedAt 이 다르면 409 4102 다`() {
            mockMvc
                .perform(admin(patch("/admin/v1/courses/1")).json(editBody(currentUpdatedAt(1))))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.code").value(4101))

            mockMvc
                .perform(admin(patch("/admin/v1/courses/2")).json(editBody("2020-01-01T00:00:00Z")))
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4102))
            assertEquals(emptyList<String?>(), actions(2))
        }

        @Test
        fun `사용자 코스를 숨기면 앱에서 빠지고 색인 삭제 개수 감소 작성자 알림 감사 기록이 남는다`() {
            mockMvc.perform(admin(post("/admin/v1/courses/1/hide"))).andExpect(status().isOk)

            mockMvc.perform(get("/service/v1/courses/1")).andExpect(status().isNotFound)
            // 색인기는 공개가 아닌 코스를 지운다(CourseSearchSyncListenerTest). 여기선 숨긴 상태가 이벤트에 실렸는지 본다.
            val saved = events.stream(CourseSavedEvent::class.java).toList().single()
            assertEquals(false, saved.newCourse.isPubliclyVisible)
            assertEquals("HIDDEN", courseStatus(1))
            assertEquals(0, publicCount(1))
            assertEquals(listOf("HIDE"), actions(1))
            awaitNotificationIdle()
            assertEquals(listOf("COURSE_HIDDEN_BY_ADMIN"), notifications(1))
            assertEquals(
                "사용자 공개 코스",
                jdbcTemplate.queryForObject(
                    "SELECT course_title FROM notifications WHERE user_id = 1",
                    String::class.java,
                ),
            )
        }

        @Test
        fun `큐레이터 코스를 숨기면 작성자 알림은 보내지 않는다`() {
            mockMvc.perform(admin(post("/admin/v1/courses/2/hide"))).andExpect(status().isOk)

            awaitNotificationIdle()
            assertEquals(emptyList<String?>(), notifications(2))
            assertEquals(listOf("HIDE"), actions(2))
        }

        @Test
        fun `숨김을 해제하면 활성으로 돌아와 다시 색인하고 개수가 늘어난다`() {
            mockMvc.perform(admin(post("/admin/v1/courses/4/unhide"))).andExpect(status().isOk)

            assertEquals("ACTIVE", courseStatus(4))
            val saved = events.stream(CourseSavedEvent::class.java).toList().single()
            assertEquals(4L, saved.newCourse.id)
            assertEquals(true, saved.newCourse.isPubliclyVisible)
            assertEquals(2, publicCount(2))
            assertEquals(listOf("UNHIDE"), actions(4))

            // 이미 활성인 코스는 해제할 수 없다.
            mockMvc
                .perform(admin(post("/admin/v1/courses/4/unhide")))
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4103))
        }

        @Test
        fun `사용자 코스를 삭제하면 작성자에게 알리고 삭제된 코스는 숨김 상태로 복원한다`() {
            mockMvc.perform(admin(delete("/admin/v1/courses/1"))).andExpect(status().isOk)
            assertEquals("DELETED", courseStatus(1))
            assertEquals(0, publicCount(1))
            assertEquals(
                1L,
                events
                    .stream(CourseDeletedEvent::class.java)
                    .toList()
                    .single()
                    .courseId,
            )
            awaitNotificationIdle()
            assertEquals(listOf("COURSE_DELETED_BY_ADMIN"), notifications(1))

            mockMvc.perform(admin(post("/admin/v1/courses/5/restore"))).andExpect(status().isOk)
            assertEquals("HIDDEN", courseStatus(5))
            assertNull(jdbcTemplate.queryForObject("SELECT deleted_at FROM courses WHERE id = 5", Any::class.java))
            // 복원은 숨김이라 앱에는 아직 안 보이고 개수도 그대로다. 공개는 unhide 로 따로 한다.
            mockMvc.perform(get("/service/v1/courses/5")).andExpect(status().isNotFound)
            assertEquals(0, publicCount(1))
            mockMvc.perform(admin(post("/admin/v1/courses/5/unhide"))).andExpect(status().isOk)
            mockMvc.perform(get("/service/v1/courses/5")).andExpect(status().isOk)
            assertEquals(1, publicCount(1))
            assertEquals(listOf("DELETE"), actions(1))
            assertEquals(listOf("RESTORE", "UNHIDE"), actions(5))
        }

        @Test
        fun `숨긴 코스를 삭제했다 복원하면 숨김으로 돌아오고 색인과 개수가 그대로다`() {
            mockMvc.perform(admin(delete("/admin/v1/courses/4"))).andExpect(status().isOk)
            mockMvc.perform(admin(post("/admin/v1/courses/4/restore"))).andExpect(status().isOk)

            assertEquals("HIDDEN", courseStatus(4))
            // 숨김 코스는 개수에 잡히지 않아 삭제(old=null)와 복원(old=null, new=null) 모두 변화가 없다.
            assertEquals(
                null,
                events
                    .stream(CourseDeletedEvent::class.java)
                    .toList()
                    .single()
                    .oldVisibility,
            )
            val restored = events.stream(CourseSavedEvent::class.java).toList().single()
            assertEquals(null, restored.oldVisibility)
            assertEquals(null, restored.newVisibility)
            assertEquals(false, restored.newCourse.isPubliclyVisible)
            assertEquals(1, publicCount(2))
        }

        @Test
        fun `숨긴 큐레이터 코스를 어드민이 편집해도 숨김 그대로라 색인에서 빠지고 개수가 그대로다`() {
            mockMvc
                .perform(admin(patch("/admin/v1/courses/4")).json(editBody(currentUpdatedAt(4))))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.status").value("HIDDEN"))

            val saved = events.stream(CourseSavedEvent::class.java).toList().single()
            assertEquals(false, saved.newCourse.isPubliclyVisible)
            assertEquals(null, saved.oldVisibility)
            assertEquals(null, saved.newVisibility)
            assertEquals(1, publicCount(2))
        }

        @Test
        fun `작성자가 탈퇴한 코스는 복원하지 않는다`() {
            jdbcTemplate.update("UPDATE users SET deleted_at = now() WHERE id = 1")

            mockMvc
                .perform(admin(post("/admin/v1/courses/5/restore")))
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4103))
            assertEquals("DELETED", courseStatus(5))
            assertEquals(emptyList<String?>(), actions(5))
        }

        @Test
        fun `일괄 조치는 건마다 처리해 부분 성공을 건별 결과로 돌려준다`() {
            mockMvc
                .perform(admin(post("/admin/v1/courses/bulk")).json("""{"ids":[1,4,999],"action":"HIDE"}"""))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].success").value(true))
                .andExpect(jsonPath("$.data[1].success").value(false))
                .andExpect(jsonPath("$.data[1].code").value(4103))
                .andExpect(jsonPath("$.data[2].success").value(false))
                .andExpect(jsonPath("$.data[2].code").value(4041))

            assertEquals("HIDDEN", courseStatus(1))
            assertEquals(listOf("HIDE"), actions(1))
            assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM admin_actions", Int::class.java))
        }

        @Test
        fun `일괄 조치는 1건 이상 50건 이하만 받는다`() {
            val ids = (1..51).joinToString(",")
            mockMvc
                .perform(admin(post("/admin/v1/courses/bulk")).json("""{"ids":[$ids],"action":"DELETE"}"""))
                .andExpect(status().isBadRequest)
            mockMvc
                .perform(admin(post("/admin/v1/courses/bulk")).json("""{"ids":[],"action":"DELETE"}"""))
                .andExpect(status().isBadRequest)
        }

        private fun assertIds(
            query: String,
            vararg ids: Int,
        ) {
            mockMvc
                .perform(admin(get("/admin/v1/courses").param(query.substringBefore("="), query.substringAfter("="))))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.total").value(ids.size))
                .andExpect(jsonPath("$.data.items[*].id", containsInAnyOrder(*ids.toTypedArray())))
        }

        private fun currentUpdatedAt(courseId: Long): String {
            val body =
                mockMvc
                    .perform(admin(get("/admin/v1/courses/$courseId")))
                    .andReturn()
                    .response.contentAsString
            return JsonPath.read(body, "$.data.updatedAt")
        }

        private fun editBody(
            expectedUpdatedAt: String,
            title: String = "고친 제목",
            reversed: Boolean = false,
        ): String {
            val (first, second) = if (reversed) 2 to 1 else 1 to 2
            return """
                {
                  "expectedUpdatedAt": "$expectedUpdatedAt",
                  "title": "$title",
                  "thumbnailUrl": "https://img/new-cover.jpg",
                  "visibility": "PUBLIC",
                  "isPublished": true,
                  "places": [
                    {"placeId": $first, "orderNo": 0, "caption": "첫 장소", "imageUrls": ["https://img/a.jpg"], "walkingMinutes": 3},
                    {"placeId": $second, "orderNo": 1, "caption": "둘째 장소", "imageUrls": ["https://img/b.jpg"]}
                  ]
                }
                """.trimIndent()
        }

        private fun admin(request: MockHttpServletRequestBuilder) =
            request.header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${adminTokenPort.issue("admin@example.com", "admin-google-sub").token}",
            )

        private fun MockHttpServletRequestBuilder.json(body: String) =
            contentType(MediaType.APPLICATION_JSON).content(body)

        private fun courseStatus(courseId: Long) =
            jdbcTemplate.queryForObject("SELECT status FROM courses WHERE id = ?", String::class.java, courseId)

        private fun publicCount(userId: Long) =
            jdbcTemplate.queryForObject("SELECT public_courses_cnt FROM users WHERE id = ?", Int::class.java, userId)

        private fun actions(courseId: Long): List<String?> =
            jdbcTemplate.queryForList(
                "SELECT action FROM admin_actions WHERE target_type = 'COURSE' AND target_id = ? " +
                    "AND admin_email = 'admin@example.com' ORDER BY id",
                String::class.java,
                courseId,
            )

        private fun notifications(userId: Long): List<String?> =
            jdbcTemplate.queryForList(
                "SELECT type FROM notifications WHERE user_id = ? ORDER BY id",
                String::class.java,
                userId,
            )
    }
