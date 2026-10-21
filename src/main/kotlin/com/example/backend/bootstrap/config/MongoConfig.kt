package com.example.backend.bootstrap.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.SessionSynchronization
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.core.mapping.MongoMappingContext

@Configuration
class MongoConfig {
    /** 문서마다 붙는 `_class` 필드를 끈다. 상속 매핑을 쓰지 않아 필요 없고, 클래스 이름 변경에 영향받지 않게 한다. */
    @Bean
    fun mappingMongoConverter(
        databaseFactory: MongoDatabaseFactory,
        mappingContext: MongoMappingContext,
        conversions: MongoCustomConversions,
    ): MappingMongoConverter =
        MappingMongoConverter(DefaultDbRefResolver(databaseFactory), mappingContext).apply {
            setCustomConversions(conversions)
            setTypeMapper(DefaultMongoTypeMapper(null))
        }

    /** MongoDB 호출을 PostgreSQL @Transactional 에 묶지 않는다. 메시지 저장은 PostgreSQL 트랜잭션과 무관하게 바로 반영한다. */
    @Bean
    fun mongoTemplate(
        databaseFactory: MongoDatabaseFactory,
        converter: MappingMongoConverter,
    ): MongoTemplate =
        MongoTemplate(databaseFactory, converter).apply {
            setSessionSynchronization(SessionSynchronization.NEVER)
        }
}
