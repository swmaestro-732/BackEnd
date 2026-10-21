package com.example.backend.chat.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.chat.application.event.ChatMessageSentEvent
import com.example.backend.chat.application.port.inbound.ChatMessageUseCase
import com.example.backend.chat.application.port.inbound.ChatRoomUseCase
import com.example.backend.chat.application.port.inbound.dto.JoinChatRoomCommand
import com.example.backend.chat.application.port.inbound.dto.SendChatMessageCommand
import com.example.backend.chat.application.port.outbound.ChatMessagePersistencePort
import com.example.backend.chat.application.port.outbound.NewChatMessage
import com.example.backend.chat.domain.model.ChatCard
import com.example.backend.chat.domain.model.ChatCardType
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
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.concurrent.Executors

/**
 * 카드·답글·멘션, 전송 이벤트, 참여·나가기 시스템 메시지 통합 테스트.
 * 사용자 1·2 가 성동구 방에 참여한 상태에서 시작한다(입장 메시지가 seq 1, 2).
 */
@AutoConfigureMockMvc
@RecordApplicationEvents
@Sql(
    scripts = ["/sql/chat-fixture.sql", "/sql/chat-card-fixture.sql"],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
class ChatMessageContentControllerTest
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
            join(USER_ID)
            join(OTHER_ID)
        }

        @Test
        fun `장소 카드는 서버가 만든 스냅샷을 코멘트와 함께 저장하고 방 목록 미리보기에도 실린다`() {
            send("""{"clientMessageId":"p","body":"여기 좋아요","card":{"type":"PLACE","refId":501}}""")
                .andExpect(status().isCreated)

            list()
                .andExpect(jsonPath("$.data.messages[2].type").value("PLACE"))
                .andExpect(jsonPath("$.data.messages[2].body").value("여기 좋아요"))
                .andExpect(jsonPath("$.data.messages[2].card.refId").value(501))
                .andExpect(jsonPath("$.data.messages[2].card.title").value("어니언 성수"))
                .andExpect(jsonPath("$.data.messages[2].card.subtitle").value("서울 성동구 아차산로 100"))
                .andExpect(
                    jsonPath("$.data.messages[2].card.thumbnailUrl").value("https://cdn.example.com/places/501/1.jpg"),
                )
            myRooms().andExpect(jsonPath("$.data.rooms[0].lastMessage.card.title").value("어니언 성수"))
        }

        @Test
        fun `코스 카드는 전체 공개·발행 코스만 지역과 장소 수를 부제목으로 저장한다`() {
            send("""{"clientMessageId":"c","card":{"type":"COURSE","refId":1}}""").andExpect(status().isCreated)

            list()
                .andExpect(jsonPath("$.data.messages[2].type").value("COURSE"))
                .andExpect(jsonPath("$.data.messages[2].body").doesNotExist())
                .andExpect(jsonPath("$.data.messages[2].card.title").value("성수 카페 투어"))
                .andExpect(jsonPath("$.data.messages[2].card.subtitle").value("성수 · 장소 2곳"))
        }

        @Test
        fun `공유할 수 없는 장소·코스 카드는 4040을 내려주고 저장하지 않는다`() {
            listOf("COURSE" to 2, "COURSE" to 3, "COURSE" to 999, "PLACE" to 999).forEach { (type, refId) ->
                send("""{"clientMessageId":"$type-$refId","card":{"type":"$type","refId":$refId}}""")
                    .andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value(4040))
            }
            assertEquals(2L, chatMessagePersistencePort.findLastSeq(roomId))
        }

        @Test
        fun `답글은 원문 앞 50자, 본문 없는 카드면 카드 제목을 미리보기로 저장한다`() {
            val longBody = "가".repeat(60)
            send("""{"clientMessageId":"t","body":"$longBody"}""") // seq 3
            send("""{"clientMessageId":"c","card":{"type":"COURSE","refId":1}}""") // seq 4
            send("""{"clientMessageId":"r1","body":"답1","replyToSeq":3}""", OTHER_ID)
            send("""{"clientMessageId":"r2","body":"답2","replyToSeq":4}""", OTHER_ID)

            list()
                .andExpect(jsonPath("$.data.messages[4].replyTo.seq").value(3))
                .andExpect(jsonPath("$.data.messages[4].replyTo.senderId").value(USER_ID))
                .andExpect(jsonPath("$.data.messages[4].replyTo.preview").value("가".repeat(50)))
                .andExpect(jsonPath("$.data.messages[5].replyTo.preview").value("성수 카페 투어"))
        }

        @Test
        fun `없는 메시지·시스템 메시지·삭제된 메시지에는 답글을 달 수 없다`() {
            chatMessagePersistencePort.insertWithNextSeq(deletedMessage()) // seq 3

            listOf(999, 1, 3).forEach { seq ->
                send("""{"clientMessageId":"r-$seq","body":"답","replyToSeq":$seq}""")
                    .andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value(4040))
            }
        }

        @Test
        fun `멘션은 방 멤버만 중복 없이 남기고 자기 자신은 뺀다`() {
            send("""{"clientMessageId":"m","body":"@다른사람","mentionUserIds":[$OTHER_ID,$USER_ID,999,$OTHER_ID]}""")

            list().andExpect(jsonPath("$.data.messages[2].mentions", contains(OTHER_ID.toInt())))
        }

        @Test
        fun `본문과 카드가 모두 없거나 멘션이 10명을 넘으면 4002를 내려준다`() {
            send("""{"clientMessageId":"e1"}""")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
            send("""{"clientMessageId":"e2","body":"x","mentionUserIds":[${(1..11).joinToString(",")}]}""")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        @Test
        fun `삭제된 카드 메시지는 카드와 멘션을 내리지 않는다`() {
            chatMessagePersistencePort.insertWithNextSeq(
                deletedMessage().copy(
                    type = ChatMessageType.PLACE,
                    card = ChatCard(ChatCardType.PLACE, 501, "어니언 성수", null, null),
                    mentions = listOf(OTHER_ID),
                ),
            )

            list()
                .andExpect(jsonPath("$.data.messages[2].status").value("DELETED"))
                .andExpect(jsonPath("$.data.messages[2].card").doesNotExist())
                .andExpect(jsonPath("$.data.messages[2].mentions").isEmpty)
            myRooms().andExpect(jsonPath("$.data.rooms[0].lastMessage.card").doesNotExist())
        }

        @Test
        fun `새로 저장한 메시지만 전송 이벤트를 발행하고 같은 재전송은 발행하지 않는다`(events: ApplicationEvents) {
            events.clear() // setUp 의 입장 메시지 이벤트
            send("""{"clientMessageId":"once","body":"한 번"}""")
            send("""{"clientMessageId":"once","body":"한 번"}""")

            val sent = events.stream(ChatMessageSentEvent::class.java).toList()
            assertEquals(listOf(3L), sent.map { it.message.seq })
        }

        @Test
        fun `같은 재전송이 동시에 들어와도 한 건만 저장하고 모두 같은 seq 를 돌려준다`() {
            val seqs =
                Executors.newFixedThreadPool(CONCURRENCY).use { pool ->
                    (1..CONCURRENCY)
                        .map {
                            pool.submit<Long> {
                                chatMessageUseCase.send(SendChatMessageCommand(roomId, USER_ID, "same", "동시")).seq
                            }
                        }.map { it.get() }
                }

            assertEquals(setOf(3L), seqs.toSet())
            assertEquals(
                1L,
                mongoTemplate.count(Query(Criteria.where("clientMessageId").`is`("same")), "chat_messages"),
            )
            assertEquals(3L, chatMessagePersistencePort.findLastSeq(roomId))
        }

        @Test
        fun `참여·나가기는 시스템 메시지를 한 번씩만 남기고 대상 사용자 정보를 붙인다`() {
            join(OTHER_ID) // 이미 참여 — 남기지 않는다
            leave(OTHER_ID).andExpect(status().isOk)
            leave(OTHER_ID).andExpect(status().isOk) // 이미 나감 — 남기지 않는다

            list()
                .andExpect(jsonPath("$.data.messages[*].type", contains("SYSTEM", "SYSTEM", "SYSTEM")))
                .andExpect(jsonPath("$.data.messages[*].system.event", contains("JOIN", "JOIN", "LEAVE")))
                .andExpect(jsonPath("$.data.messages[1].system.userId").value(OTHER_ID))
                .andExpect(jsonPath("$.data.messages[1].system.user.nickname").value("다른사람"))
                .andExpect(jsonPath("$.data.messages[1].sender").doesNotExist())
            // 자기 입장 메시지(seq 1)는 읽은 것으로 두고, 다른 사람의 입장·퇴장은 안 읽은 수에 든다.
            myRooms()
                .andExpect(jsonPath("$.data.rooms[0].unreadCount").value(2))
                .andExpect(jsonPath("$.data.rooms[0].lastMessage.system.event").value("LEAVE"))
        }

        private fun join(userId: Long) {
            chatRoomUseCase.join(JoinChatRoomCommand(userId, roomId, SEONGSU))
        }

        private fun leave(userId: Long): ResultActions =
            mockMvc.perform(
                delete("/api/v1/chat-rooms/$roomId/members/me").header(HttpHeaders.AUTHORIZATION, bearer(userId)),
            )

        private fun send(
            json: String,
            userId: Long = USER_ID,
        ): ResultActions =
            mockMvc.perform(
                post("/api/v1/chat-rooms/$roomId/messages")
                    .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json),
            )

        private fun list(): ResultActions =
            mockMvc.perform(
                get("/api/v1/chat-rooms/$roomId/messages").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)),
            )

        private fun myRooms(): ResultActions =
            mockMvc.perform(get("/api/v1/my/chat-rooms").header(HttpHeaders.AUTHORIZATION, bearer(USER_ID)))

        private fun deletedMessage() =
            NewChatMessage(
                roomId = roomId,
                type = ChatMessageType.TEXT,
                senderId = USER_ID,
                clientMessageId = "deleted",
                body = "지운 말",
                status = ChatMessageStatus.DELETED,
                createdAt = Instant.now(),
            )

        private fun bearer(userId: Long) = "Bearer ${jwtTokenProvider.issueAccessToken(userId)}"

        private companion object {
            const val USER_ID = 1L
            const val OTHER_ID = 2L
            const val CONCURRENCY = 10
            val SEONGSU = Coordinate(latitude = 37.5446, longitude = 127.0557)
        }
    }
