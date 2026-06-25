package com.example.backend.domain.sample

import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/**
 * Exposed DSL 기반 리포지토리.
 * @Transactional(서비스 계층)이 SpringTransactionManager(exposed-spring-boot-starter)로
 * 트랜잭션을 열어주므로 DSL 호출이 동작한다.
 */
@Repository
class SampleRepository {
    fun findAll(): List<Sample> = Samples.selectAll().map { Sample(it[Samples.id], it[Samples.name]) }

    fun create(name: String): Int = Samples.insert { it[Samples.name] = name }[Samples.id]
}
