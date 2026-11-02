package com.example.backend.chat.adapter.outbound.persistence.mongo

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.PartialIndexFilter
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Component

/**
 * 부팅 시 채팅 메시지 인덱스를 만든다. 같은 정의를 다시 만들면 아무 일도 없어 서버 여러 대가 동시에 떠도 안전하다.
 * 실패해도 부팅을 막지 않는다(fail-soft). 이미 있는 인덱스의 옵션은 바뀌지 않으니 변경은 collMod 로 한다.
 */
@Component
class ChatMongoIndexInitializer(
    private val mongoTemplate: MongoTemplate,
) : ApplicationRunner {
    private val log = KotlinLogging.logger {}

    override fun run(args: ApplicationArguments) {
        runCatching {
            // DocumentDB 는 트랜잭션 안에서 컬렉션을 만들지 못해 첫 메시지 전에 만들어 둔다. 메시지 컬렉션은 인덱스 생성 때 생긴다.
            if (!mongoTemplate.collectionExists(ChatRoomCounterDocument::class.java)) {
                mongoTemplate.createCollection(ChatRoomCounterDocument::class.java)
            }
            val indexOps = mongoTemplate.indexOps(ChatMessageDocument::class.java)
            indexOps.createIndex(
                Index()
                    .on("roomId", Sort.Direction.ASC)
                    .on("seq", Sort.Direction.DESC)
                    .unique()
                    .named("uq_chat_messages_room_seq"),
            )
            // 시스템 메시지(clientMessageId 필드 없음)끼리 충돌하지 않게 필드가 있을 때만 유니크를 건다.
            // DocumentDB 부분 인덱스는 $type 을 지원하지 않아 $exists 를 쓴다.
            indexOps.createIndex(
                Index()
                    .on("roomId", Sort.Direction.ASC)
                    .on("senderId", Sort.Direction.ASC)
                    .on("clientMessageId", Sort.Direction.ASC)
                    .unique()
                    .partial(PartialIndexFilter.of(Criteria.where("clientMessageId").exists(true)))
                    .named("uq_chat_messages_client_message"),
            )
        }.onSuccess { log.info { "MongoDB 채팅 인덱스 준비 완료" } }
            .onFailure { log.error(it) { "MongoDB 채팅 인덱스 생성 실패(무시): ${it.message}" } }
    }
}
