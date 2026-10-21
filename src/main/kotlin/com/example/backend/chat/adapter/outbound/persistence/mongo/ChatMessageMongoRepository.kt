package com.example.backend.chat.adapter.outbound.persistence.mongo

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
) {
    fun incrementSeq(roomId: Long): Long =
        checkNotNull(
            mongoTemplate.findAndModify(
                Query(Criteria.where("_id").`is`(roomId)),
                Update().inc("lastSeq", 1),
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                ChatRoomCounterDocument::class.java,
            ),
        ).lastSeq

    fun insert(document: ChatMessageDocument): ChatMessageDocument = mongoTemplate.insert(document)

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
}
