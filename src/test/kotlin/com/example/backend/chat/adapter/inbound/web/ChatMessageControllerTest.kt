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
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.concurrent.Executors

/**
 * 채팅 메시지 전송·조회 통합 테스트. 메시지는 MongoDB(레플리카셋, CI·docker-compose)에 실제로 저장한다.
 * 각 테스트 전에 메시지·카운터 문서를 비운다(인덱스는 남긴다).
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/chat-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ChatMessageControllerTest
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

        @BeforeEach
        fun setUp() {
            mongoTemplate.remove(Query(), "chat_messages")
            mongoTemplate.remove(Query(), "chat_room_counters")
            roomId = chatRoomUseCase.findRoomsAt(SEONGSU).current.id
        }

        @Test
        fun `참여하지 않은 방에 보내면 4033을 내려준다`() {
            send("m-1", "안녕")
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.code").value(4033))
        }

        @Test
        fun `보낸 순서대로 seq 를 1부터 매기고 같은 clientMessageId 재전송은 기존 메시지를 돌려준다`() {
            join()

            send("m-1", "첫 번째").andExpect(status().isCreated).andExpect(jsonPath("$.data.seq").value(1))
            send("m-2", "두 번째").andExpect(jsonPath("$.data.seq").value(2))
            send("m-1", "첫 번째")
                .andExpect(jsonPath("$.data.seq").value(1))
                .andExpect(jsonPath("$.data.clientMessageId").value("m-1"))
        }

        @Test
        fun `최신 메시지를 seq 오름차순으로 작성자 정보와 함께 내려준다`() {
            join()
            sendAll(3)

            list("size" to "2")
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.messages[*].seq", contains(2, 3)))
                .andExpect(jsonPath("$.data.messages[0].type").value("TEXT"))
                .andExpect(jsonPath("$.data.messages[0].body").value("메시지 2"))
                .andExpect(jsonPath("$.data.messages[0].sender.nickname").value("채팅주체"))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.throughSeq").value(3))
        }

        @Test
        fun `beforeSeq 는 이전 메시지를 내려주고 throughSeq 는 없다`() {
            join()
            sendAll(3)

            list("beforeSeq" to "3")
                .andExpect(jsonPath("$.data.messages[*].seq", contains(1, 2)))
                .andExpect(jsonPath("$.data.hasMore").value(false))
                .andExpect(jsonPath("$.data.throughSeq").doesNotExist())
        }

        @Test
        fun `afterSeq 로 놓친 메시지를 채우고 남았으면 throughSeq 는 이번에 확인한 곳까지다`() {
            join()
            sendAll(3)

            list("afterSeq" to "0", "size" to "2")
                .andExpect(jsonPath("$.data.messages[*].seq", contains(1, 2)))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.throughSeq").value(2))
            list("afterSeq" to "2")
                .andExpect(jsonPath("$.data.messages[*].seq", contains(3)))
                .andExpect(jsonPath("$.data.hasMore").value(false))
                .andExpect(jsonPath("$.data.throughSeq").value(3))
        }

        @Test
        fun `삭제된 메시지는 본문 없이 자리만 내려주고 없는 작성자는 sender 가 없다`() {
            join()
            chatMessagePersistencePort.insertWithNextSeq(
                newMessage(senderId = 999, clientMessageId = "gone", status = ChatMessageStatus.DELETED),
            )

            list()
                .andExpect(jsonPath("$.data.messages[0].status").value("DELETED"))
                .andExpect(jsonPath("$.data.messages[0].body").doesNotExist())
                .andExpect(jsonPath("$.data.messages[0].sender").doesNotExist())
        }

        @Test
        fun `잘못된 조회·전송 요청은 4001 또는 4002를 내려준다`() {
            join()

            list("afterSeq" to "1", "beforeSeq" to "3")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
            list("size" to "500")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
            send("m-1", "  ")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        @Test
        fun `같은 방에 동시에 보내도 seq 가 빠짐없이 이어진다`() {
            join()

            Executors.newFixedThreadPool(CONCURRENCY).use { pool ->
                (1..CONCURRENCY)
                    .map { i ->
                        pool.submit {
                            chatMessageUseCase.send(SendChatMessageCommand(roomId, USER_ID, "c-$i", "동시 $i"))
                        }
                    }.forEach { it.get() }
            }

            val seqs =
                mongoTemplate
                    .find(
                        Query(Criteria.where("roomId").`is`(roomId)).with(Sort.by("seq")),
                        Map::class.java,
                        "chat_messages",
                    ).map { (it["seq"] as Number).toLong() }
            assertEquals((1L..CONCURRENCY).toList(), seqs)
            assertEquals(CONCURRENCY.toLong(), chatMessagePersistencePort.findLastSeq(roomId))
        }

        @Test
        fun `같은 clientMessageId 를 다시 저장하면 먼저 저장된 메시지를 돌려주고 번호를 쓰지 않는다`() {
            val first =
                chatMessagePersistencePort.insertWithNextSeq(
                    newMessage(senderId = USER_ID, clientMessageId = "dup"),
                )
            val second =
                chatMessagePersistencePort.insertWithNextSeq(
                    newMessage(senderId = USER_ID, clientMessageId = "dup"),
                )

            assertEquals(first.seq, second.seq)
            assertEquals(1L, chatMessagePersistencePort.findLastSeq(roomId))
        }

        private fun join() {
            chatRoomUseCase.join(JoinChatRoomCommand(USER_ID, roomId, SEONGSU))
        }

        private fun sendAll(count: Int) = (1..count).forEach { send("m-$it", "메시지 $it") }

        private fun send(
            clientMessageId: String,
            body: String,
        ): ResultActions =
            mockMvc.perform(
                post("/api/v1/chat-rooms/$roomId/messages")
                    .header(HttpHeaders.AUTHORIZATION, bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"clientMessageId":"$clientMessageId","body":"$body"}"""),
            )

        private fun list(vararg params: Pair<String, String>): ResultActions =
            mockMvc.perform(
                get("/api/v1/chat-rooms/$roomId/messages")
                    .header(HttpHeaders.AUTHORIZATION, bearer())
                    .apply { params.forEach { (name, value) -> param(name, value) } },
            )

        private fun newMessage(
            senderId: Long,
            clientMessageId: String,
            status: ChatMessageStatus = ChatMessageStatus.VISIBLE,
        ) = NewChatMessage(
            roomId = roomId,
            type = ChatMessageType.TEXT,
            senderId = senderId,
            clientMessageId = clientMessageId,
            body = "본문",
            status = status,
            createdAt = Instant.now(),
        )

        private fun bearer() = "Bearer ${jwtTokenProvider.issueAccessToken(USER_ID)}"

        private companion object {
            const val USER_ID = 1L
            const val CONCURRENCY = 20
            val SEONGSU = Coordinate(latitude = 37.5446, longitude = 127.0557)
        }
    }
