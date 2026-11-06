package com.example.backend.chat.adapter.outbound.persistence.mongo

import com.example.backend.chat.application.port.outbound.NewChatMessage
import com.mongodb.MongoException
import com.mongodb.client.MongoClient
import com.mongodb.client.TransactionBody
import org.springframework.dao.DataAccessException
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository

@Repository
class ChatMessageMongoRepository(
    private val mongoTemplate: MongoTemplate,
    private val mongoClient: MongoClient,
) {
    /**
     * 카운터 증가와 메시지 저장을 한 트랜잭션으로 묶는다. 같은 방 동시 전송은 카운터 쓰기 충돌로
     * 뒤쪽이 재시도하므로 한 줄로 서고, 커밋 순서가 seq 순서가 된다. 실패하면 카운터도 되돌아가 번호가 비지 않는다.
     */
    fun insertWithNextSeq(message: NewChatMessage): ChatMessageDocument =
        mongoClient.startSession().use { session ->
            session.withTransaction(
                TransactionBody {
                    // 재시도로 여러 번 실행될 수 있어 저장 두 개만 둔다.
                    unwrapMongoException {
                        val template = mongoTemplate.withSession(session)
                        val seq =
                            checkNotNull(
                                template.findAndModify(
                                    Query(Criteria.where("_id").`is`(message.roomId)),
                                    Update().inc("lastSeq", 1),
                                    FindAndModifyOptions.options().upsert(true).returnNew(true),
                                    ChatRoomCounterDocument::class.java,
                                ),
                            ).lastSeq
                        template.insert(ChatMessageDocument.from(message, seq))
                    }
                },
            )
        }

    fun findLastSeq(roomId: Long): Long =
        mongoTemplate.findById(roomId, ChatRoomCounterDocument::class.java)?.lastSeq ?: 0

    fun findByClientMessageId(
        roomId: Long,
        senderId: Long,
        clientMessageId: String,
    ): ChatMessageDocument? =
        mongoTemplate.findOne(
            Query(
                Criteria
                    .where("roomId")
                    .`is`(roomId)
                    .and("senderId")
                    .`is`(senderId)
                    .and("clientMessageId")
                    .`is`(clientMessageId),
            ),
            ChatMessageDocument::class.java,
        )

    fun findBefore(
        roomId: Long,
        beforeSeq: Long?,
        limit: Int,
    ): List<ChatMessageDocument> {
        // and() 의 반환값으로 쿼리를 만들어야 seq 조건이 들어간다(앞 Criteria 를 쓰면 roomId 조건만 나간다).
        val roomCriteria = Criteria.where("roomId").`is`(roomId)
        val criteria = if (beforeSeq == null) roomCriteria else roomCriteria.and("seq").lt(beforeSeq)
        return mongoTemplate.find(
            Query(criteria).with(Sort.by(Sort.Direction.DESC, "seq")).limit(limit),
            ChatMessageDocument::class.java,
        )
    }

    fun findAfter(
        roomId: Long,
        afterSeq: Long,
        limit: Int,
    ): List<ChatMessageDocument> =
        mongoTemplate.find(
            Query(
                Criteria
                    .where("roomId")
                    .`is`(roomId)
                    .and("seq")
                    .gt(afterSeq),
            ).with(Sort.by(Sort.Direction.ASC, "seq"))
                .limit(limit),
            ChatMessageDocument::class.java,
        )

    /**
     * MongoTemplate 은 드라이버 예외를 Spring 예외로 바꾼다. withTransaction 은 MongoException 의
     * 오류 라벨(TransientTransactionError 등)을 보고 재시도하므로 원래 예외로 되돌려 던진다.
     */
    private inline fun <T> unwrapMongoException(block: () -> T): T =
        try {
            block()
        } catch (e: DataAccessException) {
            throw (e.cause as? MongoException) ?: e
        }
}
