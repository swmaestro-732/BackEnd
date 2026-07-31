package com.example.backend.direction.application.port.inbound

import com.example.backend.common.geo.Coordinate

/** 보행자 이동 시간 인바운드 포트(공개 API). 산출 불가 시 null(fail-soft). */
interface WalkingDurationUseCase {
    /** 두 지점 사이 도보 소요(분). 산출 불가 시 null. */
    fun walkingMinutes(
        from: Coordinate,
        to: Coordinate,
    ): Int?

    /** 연속한 지점쌍별 도보 소요(분) 목록. 반환 크기 = points.size - 1. 각 구간은 산출 불가 시 null. */
    fun walkingSegments(points: List<Coordinate>): List<Int?>
}
