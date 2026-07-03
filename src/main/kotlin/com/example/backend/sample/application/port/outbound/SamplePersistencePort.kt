package com.example.backend.sample.application.port.outbound

import com.example.backend.sample.domain.model.Sample

/**
 * 아웃바운드 포트 — 애플리케이션이 영속성에 요구하는 계약.
 * 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 */
interface SamplePersistencePort {
    fun findAll(): List<Sample>

    /** 저장 후 식별자가 부여된 Sample 을 반환한다. */
    fun save(sample: Sample): Sample
}
