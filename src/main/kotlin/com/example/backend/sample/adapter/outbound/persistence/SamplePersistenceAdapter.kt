package com.example.backend.sample.adapter.outbound.persistence

import com.example.backend.sample.application.port.outbound.SamplePersistencePort
import com.example.backend.sample.domain.model.Sample
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/**
 * 아웃바운드 어댑터 — [SamplePersistencePort] 를 Exposed 로 구현한다.
 * 도메인 ↔ 테이블 행(row) 매핑을 여기서 담당하고, 도메인/애플리케이션 계층은
 * Exposed 를 전혀 알지 못한다. 트랜잭션은 애플리케이션 서비스의 @Transactional 이
 * SpringTransactionManager(exposed-spring-boot-starter)로 열어준다.
 */
@Repository
class SamplePersistenceAdapter : SamplePersistencePort {
    override fun findAll(): List<Sample> = SampleTable.selectAll().map(::toDomain)

    override fun save(sample: Sample): Sample {
        val id = SampleTable.insert { it[name] = sample.name }[SampleTable.id]
        return Sample.reconstitute(id = id, name = sample.name)
    }

    private fun toDomain(row: ResultRow): Sample =
        Sample.reconstitute(id = row[SampleTable.id], name = row[SampleTable.name])
}
