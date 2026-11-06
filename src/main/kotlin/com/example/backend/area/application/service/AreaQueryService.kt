package com.example.backend.area.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.area.application.port.inbound.dto.AreaDescriptor
import com.example.backend.area.application.port.outbound.AreaDirectoryPort
import com.example.backend.area.application.port.outbound.SigunguBoundaryPort
import com.example.backend.common.geo.Coordinate
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
    private val sigunguBoundaryPort: SigunguBoundaryPort,
) : AreaQueryUseCase {
    override fun searchAreas(keyword: String): List<AreaDescriptor> = areaDirectoryPort.search(keyword)

    override fun resolveSearchPrefixes(keyword: String): List<String> = areaDirectoryPort.resolveSearchPrefixes(keyword)

    override fun findAreaByCode(code: String): AreaDescriptor? = areaDirectoryPort.findByCode(code)

    override fun findSigunguCodeAt(coordinate: Coordinate): String? =
        sigunguBoundaryPort.findCodeContaining(coordinate)
            ?: sigunguBoundaryPort.findNearestCode(coordinate, BOUNDARY_FALLBACK_METERS)

    override fun findAdjacentSigunguCodes(sigunguCode: String): List<String> =
        sigunguBoundaryPort.findAdjacentCodes(sigunguCode)

    private companion object {
        // 경계 단순화 오차(약 30m)와 해변·항구처럼 경계 바로 밖에 찍히는 좌표를 보완한다.
        const val BOUNDARY_FALLBACK_METERS = 500.0
    }
}
