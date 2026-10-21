package com.example.backend.chat.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import com.jayway.jsonpath.JsonPath
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.nio.charset.StandardCharsets

/**
 * 채팅방 조회·참여·나가기·내 방 목록 통합 테스트. 좌표 → 시군구 판정(area, PostGIS)부터 실제 DB 를 거친다.
 * 방 id 는 V19 시드 순서에 기대지 않고 위치 조회 응답에서 꺼낸다.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/chat-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ChatRoomControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @Test
        fun `성수역 좌표면 성동구 방과 인접 구 방을 내려준다`() {
            findRoomsAt(SEONGSU)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.current.name").value("서울특별시 성동구"))
                .andExpect(jsonPath("$.data.nearby[*].name", hasItem("서울특별시 광진구")))
        }

        @Test
        fun `경계 밖 바다 좌표는 500m 안의 가장 가까운 구로 판정한다`() {
            findRoomsAt(HAEUNDAE_OFFSHORE)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.current.name").value("부산광역시 해운대구"))
        }

        @Test
        fun `국내가 아닌 좌표는 4007을 내려준다`() {
            findRoomsAt(TOKYO)
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4007))
        }

        @Test
        fun `현재 구 방에 참여하면 CURRENT 이고 다시 참여해도 같은 결과를 내려준다`() {
            val roomId = currentRoomId(SEONGSU)

            repeat(2) {
                join(roomId, SEONGSU)
                    .andExpect(status().isCreated)
                    .andExpect(jsonPath("$.data.roomId").value(roomId))
                    .andExpect(jsonPath("$.data.type").value("CURRENT"))
            }
        }

        @Test
        fun `인접 구 방은 NEARBY 로 3개까지 참여하고 넘으면 4008을 내려준다`() {
            val nearby = nearbyRoomIds(SEONGSU)

            nearby.take(3).forEach {
                join(it, SEONGSU)
                    .andExpect(status().isCreated)
                    .andExpect(jsonPath("$.data.type").value("NEARBY"))
            }
            join(nearby[3], SEONGSU)
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4008))
        }

        @Test
        fun `현재 구도 인접 구도 아닌 방은 4032를 내려준다`() {
            join(currentRoomId(HAEUNDAE_OFFSHORE), SEONGSU)
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.code").value(4032))
        }

        @Test
        fun `없는 방은 4040을 내려준다`() {
            join(99_999, SEONGSU)
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4040))
        }

        @Test
        fun `위도가 범위를 벗어나면 4001을 내려준다`() {
            join(currentRoomId(SEONGSU), 137.0 to 127.0)
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `나가기는 여러 번 호출해도 200이고 내 방 목록에서 빠진다`() {
            val roomId = currentRoomId(SEONGSU)
            join(roomId, SEONGSU)

            myRooms()
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.rooms[0].roomId").value(roomId))
                .andExpect(jsonPath("$.data.rooms[0].name").value("서울특별시 성동구"))
                .andExpect(jsonPath("$.data.rooms[0].type").value("CURRENT"))

            repeat(2) {
                mockMvc
                    .perform(
                        delete("/api/v1/chat-rooms/$roomId/members/me").header(HttpHeaders.AUTHORIZATION, bearer()),
                    ).andExpect(status().isOk)
            }
            myRooms().andExpect(jsonPath("$.data.rooms").isEmpty)
        }

        @Test
        fun `토큰이 없으면 401을 내려준다`() {
            mockMvc
                .perform(get("/api/v1/my/chat-rooms"))
                .andExpect(status().isUnauthorized)
        }

        private fun findRoomsAt(point: Pair<Double, Double>): ResultActions =
            mockMvc.perform(
                get("/api/v1/chat-rooms")
                    .param("lat", point.first.toString())
                    .param("lng", point.second.toString())
                    .header(HttpHeaders.AUTHORIZATION, bearer()),
            )

        private fun join(
            roomId: Long,
            point: Pair<Double, Double>,
        ): ResultActions =
            mockMvc.perform(
                post("/api/v1/chat-rooms/$roomId/members")
                    .header(HttpHeaders.AUTHORIZATION, bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"lat":${point.first},"lng":${point.second}}"""),
            )

        private fun myRooms(): ResultActions =
            mockMvc.perform(get("/api/v1/my/chat-rooms").header(HttpHeaders.AUTHORIZATION, bearer()))

        private fun currentRoomId(point: Pair<Double, Double>): Long =
            JsonPath.read<Number>(body(findRoomsAt(point)), "$.data.current.roomId").toLong()

        private fun nearbyRoomIds(point: Pair<Double, Double>): List<Long> =
            JsonPath.read<List<Number>>(body(findRoomsAt(point)), "$.data.nearby[*].roomId").map { it.toLong() }

        private fun body(result: ResultActions): String =
            result.andReturn().response.getContentAsString(StandardCharsets.UTF_8)

        private fun bearer() = "Bearer ${jwtTokenProvider.issueAccessToken(USER_ID)}"

        private companion object {
            const val USER_ID = 1L
            val SEONGSU = 37.5446 to 127.0557
            val HAEUNDAE_OFFSHORE = 35.1565 to 129.1610
            val TOKYO = 35.68 to 139.76
        }
    }
