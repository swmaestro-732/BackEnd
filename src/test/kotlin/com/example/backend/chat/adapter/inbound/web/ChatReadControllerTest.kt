package com.example.backend.chat.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.chat.application.port.inbound.ChatMessageUseCase
import com.example.backend.chat.application.port.inbound.ChatRoomUseCase
import com.example.backend.chat.application.port.inbound.dto.JoinChatRoomCommand
import com.example.backend.chat.application.port.inbound.dto.SendChatMessageCommand
import com.example.backend.chat.application.port.outbound.ChatMessagePersistencePort
import com.example.backend.chat.application.port.outbound.NewChatMessage
import com.example.backend.chat.domain.model.ChatMessageStatus
import com.example.backend.chat.domain.model.ChatMessageType
import com.example.backend.common.geo.Coordinate
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

/** 읽음 처리와 내 방 목록의 안 읽은 수·마지막 메시지·정렬 통합 테스트. */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/chat-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ChatReadControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
        private val chatRoomUseCase: ChatRoomUseCase,
        private val chatMessageUseCase: ChatMessageUseCase,
        private val chatMessagePersistencePort: ChatMessagePersistencePort,
        private val mongoTemplate: MongoTemplate,
    ) : IntegrationTestBase() {
        private var roomId = 0L
        private var nearbyRoomId = 0L

        @BeforeEach
        fun setUp() {
            mongoTemplate.remove(Query(), "chat_messages")
            mongoTemplate.remove(Query(), "chat_room_counters")
            val rooms = chatRoomUseCase.findRoomsAt(SEONGSU)
            roomId = rooms.current.id
            nearbyRoomId = rooms.nearby.first().id
        }

        @Test
        fun `새로 참여하면 이미 쌓인 메시지와 자기 입장 메시지는 읽은 것으로 시작한다`() {
            join(SENDER_ID, roomId) // seq 1
            sendAll(SENDER_ID, 3) // seq 2~4
            join(READER_ID, roomId) // seq 5

            myRooms(READER_ID)
                .andExpect(jsonPath("$.data.rooms[0].unreadCount").value(0))
                .andExpect(jsonPath("$.data.rooms[0].lastMessage.seq").value(5))
                .andExpect(jsonPath("$.data.rooms[0].lastMessage.type").value("SYSTEM"))
                .andExpect(jsonPath("$.data.rooms[0].lastMessage.system.event").value("JOIN"))
                .andExpect(jsonPath("$.data.rooms[0].lastMessage.system.userId").value(READER_ID))
        }

        @Test
        fun `읽음 번호는 올라가기만 하고 방의 마지막 seq 를 넘지 않는다`() {
            join(SENDER_ID, roomId) // seq 1
            join(READER_ID, roomId) // seq 2
            sendAll(SENDER_ID, 5) // seq 3~7
            myRooms(READER_ID).andExpect(jsonPath("$.data.rooms[0].unreadCount").value(5))

            markRead(READER_ID, roomId, 6)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.lastReadSeq").value(6))
            myRooms(READER_ID).andExpect(jsonPath("$.data.rooms[0].unreadCount").value(1))

            markRead(READER_ID, roomId, 2).andExpect(jsonPath("$.data.lastReadSeq").value(6))
            markRead(READER_ID, roomId, 999).andExpect(jsonPath("$.data.lastReadSeq").value(7))
            myRooms(READER_ID).andExpect(jsonPath("$.data.rooms[0].unreadCount").value(0))
        }

        @Test
        fun `참여하지 않은 방은 4033, 음수 seq 는 4002를 내려준다`() {
            markRead(READER_ID, roomId, 1)
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.code").value(4033))

            join(READER_ID, roomId)
            markRead(READER_ID, roomId, -1)
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        @Test
        fun `내 방 목록은 최근 메시지가 온 방부터 정렬한다`() {
            join(SENDER_ID, roomId)
            sendAll(SENDER_ID, 1)
            join(SENDER_ID, nearbyRoomId)

            myRooms(SENDER_ID).andExpect(jsonPath("$.data.rooms[0].roomId").value(nearbyRoomId))

            sendAll(SENDER_ID, 1, from = 2)
            myRooms(SENDER_ID)
                .andExpect(jsonPath("$.data.rooms[0].roomId").value(roomId))
                .andExpect(jsonPath("$.data.rooms[1].lastMessage.type").value("SYSTEM"))
        }

        @Test
        fun `마지막 메시지가 삭제됐으면 본문 없이 상태만 내려준다`() {
            join(SENDER_ID, roomId)
            chatMessagePersistencePort.insertWithNextSeq(
                NewChatMessage(
                    roomId = roomId,
                    type = ChatMessageType.TEXT,
                    senderId = SENDER_ID,
                    clientMessageId = "deleted",
                    body = "지운 말",
                    status = ChatMessageStatus.DELETED,
                    createdAt = Instant.now(),
                ),
            )

            myRooms(SENDER_ID)
                .andExpect(jsonPath("$.data.rooms[0].lastMessage.status").value("DELETED"))
                .andExpect(jsonPath("$.data.rooms[0].lastMessage.body").doesNotExist())
        }

        private fun join(
            userId: Long,
            roomId: Long,
        ) {
            chatRoomUseCase.join(JoinChatRoomCommand(userId, roomId, SEONGSU))
        }

        private fun sendAll(
            userId: Long,
            count: Int,
            from: Int = 1,
        ) = (from until from + count).forEach {
            chatMessageUseCase.send(SendChatMessageCommand(roomId, userId, "m-$it", "메시지 $it"))
        }

        private fun markRead(
            userId: Long,
            roomId: Long,
            seq: Long,
        ): ResultActions =
            mockMvc.perform(
                put("/api/v1/chat-rooms/$roomId/members/me/read")
                    .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"seq":$seq}"""),
            )

        private fun myRooms(userId: Long): ResultActions =
            mockMvc.perform(get("/api/v1/my/chat-rooms").header(HttpHeaders.AUTHORIZATION, bearer(userId)))

        private fun bearer(userId: Long) = "Bearer ${jwtTokenProvider.issueAccessToken(userId)}"

        private companion object {
            const val SENDER_ID = 1L
            const val READER_ID = 2L
            val SEONGSU = Coordinate(latitude = 37.5446, longitude = 127.0557)
        }
    }
