package com.example.backend.area.adapter.outbound.persistence.exposed.repository

import com.example.backend.area.adapter.outbound.persistence.AreaTable
import com.example.backend.area.domain.model.Area
import com.example.backend.area.domain.model.AreaCode
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/**
 * area 테이블 접근 리포지토리 — Exposed DSL 로만 조회한다(DAO 미사용).
 * ResultRow → 도메인 [Area] 변환은 이 어댑터 내부 private 매퍼([toDomain])로만 한다.
 * 트랜잭션은 애플리케이션 서비스의 @Transactional 이 SpringTransactionManager 로 열어준다.
 */
@Repository
class AreaRepository {
    /** is_active = TRUE 인 전체 지역을 읽어 도메인으로 변환한다(어댑터 캐시의 원본). */
    fun findAllActive(): List<Area> =
        AreaTable
            .selectAll()
            .where { AreaTable.isActive eq true }
            .map(::toDomain)

    private fun toDomain(row: ResultRow): Area =
        Area(
            code = AreaCode(row[AreaTable.code]),
            sidoName = row[AreaTable.sidoName],
            sigunguName = row[AreaTable.sigunguName],
            dongName = row[AreaTable.dongName],
            isActive = row[AreaTable.isActive],
        )
}
