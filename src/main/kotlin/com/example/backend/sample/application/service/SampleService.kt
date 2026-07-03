package com.example.backend.sample.application.service

import com.example.backend.sample.application.dto.CreateSampleCommand
import com.example.backend.sample.application.dto.SampleResult
import com.example.backend.sample.application.port.inbound.SampleUseCase
import com.example.backend.sample.application.port.outbound.SamplePersistencePort
import com.example.backend.sample.domain.model.Sample
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 유스케이스 구현. 인바운드 포트([SampleUseCase])를 구현하고,
 * 아웃바운드 포트([SamplePersistencePort])에만 의존한다(구현 세부는 모른다).
 * 도메인 모델은 애플리케이션 밖으로 내보내지 않고 [SampleResult] 로 매핑한다.
 */
@Service
@Transactional(readOnly = true)
class SampleService(
    private val samplePersistencePort: SamplePersistencePort,
) : SampleUseCase {
    override fun list(): List<SampleResult> = samplePersistencePort.findAll().map(SampleResult::from)

    @Transactional
    override fun create(command: CreateSampleCommand): SampleResult {
        val saved = samplePersistencePort.save(Sample.create(command.name))
        return SampleResult.from(saved)
    }
}
