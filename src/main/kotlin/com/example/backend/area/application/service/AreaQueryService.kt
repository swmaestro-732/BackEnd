package com.example.backend.area.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.area.application.port.inbound.dto.AreaDescriptor
import com.example.backend.area.application.port.outbound.AreaDirectoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 행정구역 조회 애플리케이션 서비스. 트랜잭션 경계는 [Transactional] 로 잡고(SpringTransactionManager),
 * Exposed 의 transaction 블록을 이 계층에 노출하지 않는다.
 *
 * 저장소·캐시·계층 파생 방식은 [AreaDirectoryPort] 뒤에 숨긴다.
 */
@Service
@Transactional(readOnly = true)
class AreaQueryService(
    private val areaDirectoryPort: AreaDirectoryPort,
) : AreaQueryUseCase {
    override fun searchAreas(keyword: String): List<AreaDescriptor> = areaDirectoryPort.search(keyword)

    override fun findAreaByCode(code: String): AreaDescriptor? = areaDirectoryPort.findByCode(code)
}
