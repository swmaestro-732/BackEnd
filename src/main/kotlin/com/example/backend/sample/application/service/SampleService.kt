package com.example.backend.sample.application.service

import com.example.backend.sample.application.port.inbound.SampleUseCase
import com.example.backend.sample.application.port.outbound.SampleRepository
import com.example.backend.sample.domain.Sample
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 유스케이스 구현. 인바운드 포트([SampleUseCase])를 구현하고,
 * 아웃바운드 포트([SampleRepository])에만 의존한다(구현 세부는 모른다).
 * 트랜잭션 경계는 이 애플리케이션 계층에서 관리한다.
 */
@Service
@Transactional(readOnly = true)
class SampleService(
    private val sampleRepository: SampleRepository,
) : SampleUseCase {
    override fun list(): List<Sample> = sampleRepository.findAll()

    @Transactional
    override fun create(name: String): Sample = sampleRepository.save(Sample.create(name))
}
