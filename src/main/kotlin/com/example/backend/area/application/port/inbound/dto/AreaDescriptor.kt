package com.example.backend.area.application.port.inbound.dto

import com.example.backend.area.domain.model.AreaLevel

/**
 * 시도·시군구·읍면동 계층 조회 결과.
 *
 * 도메인 엔티티가 아니라 인바운드 유스케이스가 외부에 제공하는 읽기 모델이다.
 */
data class AreaDescriptor(
    val prefix: String,
    val shortName: String,
    val fullName: String,
    val level: AreaLevel,
)
