package com.example.backend.notification.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.awaitility.Awaitility.await
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration

@AutoConfigureMockMvc
@TestPropertySource(properties = ["app.version.cache-ttl=0s"])
@Sql(scripts = ["/sql/notification-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = ["/sql/notification-cleanup.sql"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class NotificationControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @Test
        fun `목록은 본인 알림만 id 내림차순과 다음 커서로 반환한다`() {
            mockMvc
                .perform(get(NOTIFICATIONS).param("size", "2").header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.items[*].id", contains(570103, 570102)))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value(570102))
                .andExpect(jsonPath("$.data.items[0].type").value("USER_FOLLOWED"))
                .andExpect(jsonPath("$.data.items[0].targetType").value("USER"))
                .andExpect(jsonPath("$.data.items[0].targetId").value(OTHER_USER_ID))
                .andExpect(jsonPath("$.data.items[0].actorCount").value(1))
                .andExpect(jsonPath("$.data.items[0].lastActorId").value(OTHER_USER_ID))
                .andExpect(jsonPath("$.data.items[0].actorNickname").value("알림함타인"))
                .andExpect(jsonPath("$.data.items[0].courseTitle").isEmpty)
                .andExpect(jsonPath("$.data.items[0].courseImageUrl").isEmpty)
                .andExpect(jsonPath("$.data.items[0].isRead").value(false))
                .andExpect(jsonPath("$.data.items[0].createdAt").value("2026-10-24T09:20:01Z"))
                .andExpect(jsonPath("$.data.items[0].updatedAt").value("2026-10-24T09:20:01Z"))
                .andExpect(jsonPath("$.data.items[1].isRead").value(true))
                .andExpect(jsonPath("$.data.items[1].actorCount").value(2))

            mockMvc
                .perform(
                    get(NOTIFICATIONS)
                        .param("size", "2")
                        .param("cursor", "570102")
                        .header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.items[*].id", contains(570101)))
                .andExpect(jsonPath("$.data.items[0].courseTitle").value("좋아요 알림 코스"))
                .andExpect(jsonPath("$.data.items[0].courseImageUrl").value("https://example.com/course.jpg"))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty)
        }

        @Test
        fun `커서가 마지막 알림이면 빈 목록을 반환한다`() {
            mockMvc
                .perform(
                    get(NOTIFICATIONS).param("cursor", "570101").header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.items").isEmpty)
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty)
        }

        @Test
        fun `미읽음 개수는 본인 알림 중 읽지 않은 두 건이다`() {
            mockMvc
                .perform(get("$NOTIFICATIONS/unread-count").header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.count").value(2))
        }

        @Test
        fun `남의 알림과 없는 알림을 읽어도 200이고 소유자 상태는 유지한다`() {
            listOf(570104L, Long.MAX_VALUE).forEach { id ->
                mockMvc
                    .perform(readOne(id))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.code").value(2000))
            }

            mockMvc
                .perform(get(NOTIFICATIONS).header(HttpHeaders.AUTHORIZATION, tokenFor(OTHER_USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.items[*].id", contains(570104)))
                .andExpect(jsonPath("$.data.items[0].isRead").value(false))
            mockMvc
                .perform(get("$NOTIFICATIONS/unread-count").header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID)))
                .andExpect(jsonPath("$.data.count").value(2))
        }

        @Test
        fun `본인 알림은 반복해서 읽어도 성공하고 미읽음은 한 번만 줄어든다`() {
            repeat(2) {
                mockMvc
                    .perform(readOne(570103L))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.code").value(2000))
            }

            mockMvc
                .perform(get("$NOTIFICATIONS/unread-count").header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.count").value(1))
            mockMvc
                .perform(get(NOTIFICATIONS).header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID)))
                .andExpect(jsonPath("$.data.items[0].isRead").value(true))
        }

        @Test
        fun `모두 읽음은 갱신한 개수를 반환하고 다른 사용자 알림은 유지한다`() {
            mockMvc
                .perform(readAll())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.count").value(2))
            mockMvc
                .perform(readAll())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.count").value(0))
            mockMvc
                .perform(get("$NOTIFICATIONS/unread-count").header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID)))
                .andExpect(jsonPath("$.data.count").value(0))
            mockMvc
                .perform(get("$NOTIFICATIONS/unread-count").header(HttpHeaders.AUTHORIZATION, tokenFor(OTHER_USER_ID)))
                .andExpect(jsonPath("$.data.count").value(1))
        }

        @Test
        fun `저장한 설정이 없으면 모든 카테고리가 켜져 있다`() {
            mockMvc
                .perform(get(SETTINGS).header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.activityLike").value(true))
                .andExpect(jsonPath("$.data.activityComment").value(true))
                .andExpect(jsonPath("$.data.activityFollow").value(true))
                .andExpect(jsonPath("$.data.activityReview").value(true))
        }

        @Test
        fun `설정 부분 수정은 null과 생략한 필드를 유지하고 다음 조회에 반영한다`() {
            mockMvc
                .perform(
                    patch(SETTINGS)
                        .header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"activityLike":false,"activityFollow":false}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.activityLike").value(false))
                .andExpect(jsonPath("$.data.activityComment").value(true))
                .andExpect(jsonPath("$.data.activityFollow").value(false))
                .andExpect(jsonPath("$.data.activityReview").value(true))

            mockMvc
                .perform(
                    patch(SETTINGS)
                        .header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """{"activityLike":null,"activityComment":false,"activityReview":false}""",
                        ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
            mockMvc
                .perform(get(SETTINGS).header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.activityLike").value(false))
                .andExpect(jsonPath("$.data.activityComment").value(false))
                .andExpect(jsonPath("$.data.activityFollow").value(false))
                .andExpect(jsonPath("$.data.activityReview").value(false))
        }

        @Test
        fun `읽음 요청 본문이 없거나 read 가 true 가 아니면 400 이다`() {
            listOf("{}", """{"read":false}""").forEach { body ->
                mockMvc
                    .perform(
                        patch(NOTIFICATIONS)
                            .header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body),
                    ).andExpect(status().isBadRequest)
            }
            mockMvc
                .perform(get("$NOTIFICATIONS/unread-count").header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID)))
                .andExpect(jsonPath("$.data.count").value(2))
        }

        private fun readOne(id: Long) =
            patch("$NOTIFICATIONS/$id")
                .header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(READ_BODY)

        private fun readAll() =
            patch(NOTIFICATIONS)
                .header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(READ_BODY)

        @Test
        fun `알림 API는 로그인하지 않으면 모두 401을 반환한다`() {
            listOf(
                get(NOTIFICATIONS),
                get("$NOTIFICATIONS/unread-count"),
                patch("$NOTIFICATIONS/570101").contentType(MediaType.APPLICATION_JSON).content(READ_BODY),
                patch(NOTIFICATIONS).contentType(MediaType.APPLICATION_JSON).content(READ_BODY),
                get(SETTINGS),
                patch(SETTINGS).contentType(MediaType.APPLICATION_JSON).content("{}"),
            ).forEach { request -> mockMvc.perform(request).andExpect(status().isUnauthorized) }
        }

        @Test
        fun `안드로이드 빌드 4는 차단하고 빌드 5는 알림과 설정에 접근한다`() {
            listOf(NOTIFICATIONS, SETTINGS).forEach { path ->
                mockMvc
                    .perform(
                        get(path)
                            .header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID))
                            .header("X-App-Platform", "android")
                            .header("X-App-Build", "4"),
                    ).andExpect(status().`is`(426))
                    .andExpect(jsonPath("$.code").value(4260))
                mockMvc
                    .perform(
                        get(path)
                            .header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID))
                            .header("X-App-Platform", "android")
                            .header("X-App-Build", "5"),
                    ).andExpect(status().isOk)
            }
        }

        @Test
        fun `좋아요 API의 커밋 후 작성자 알림함에 비동기 알림 한 건이 기록된다`() {
            mockMvc
                .perform(get(NOTIFICATIONS).header(HttpHeaders.AUTHORIZATION, tokenFor(AUTHOR_ID)))
                .andExpect(jsonPath("$.data.items").isEmpty)

            mockMvc
                .perform(post("/api/v1/courses/570001/likes").header(HttpHeaders.AUTHORIZATION, tokenFor(USER_ID)))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.likesCnt").value(1))

            await()
                .pollInterval(Duration.ofMillis(50))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted {
                    mockMvc
                        .perform(get(NOTIFICATIONS).header(HttpHeaders.AUTHORIZATION, tokenFor(AUTHOR_ID)))
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.data.items.length()").value(1))
                        .andExpect(jsonPath("$.data.items[0].type").value("COURSE_LIKED"))
                        .andExpect(jsonPath("$.data.items[0].targetType").value("COURSE"))
                        .andExpect(jsonPath("$.data.items[0].targetId").value(570001))
                        .andExpect(jsonPath("$.data.items[0].lastActorId").value(USER_ID))
                        .andExpect(jsonPath("$.data.items[0].actorNickname").isEmpty)
                        .andExpect(jsonPath("$.data.items[0].courseTitle").value("좋아요 알림 코스"))
                        .andExpect(jsonPath("$.data.items[0].actorCount").value(1))
                        .andExpect(jsonPath("$.data.items[0].isRead").value(false))
                }
        }

        private fun tokenFor(userId: Long) = "Bearer ${jwtTokenProvider.issueAccessToken(userId)}"

        private companion object {
            const val NOTIFICATIONS = "/api/v1/my/notifications"
            const val SETTINGS = "/api/v1/my/notification-settings"
            const val READ_BODY = """{"read":true}"""
            const val USER_ID = 570001L
            const val OTHER_USER_ID = 570002L
            const val AUTHOR_ID = 570003L
        }
    }
