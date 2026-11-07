package com.example.backend.direction.application.port.inbound

import com.example.backend.common.geo.Coordinate

/** 보행자 이동 시간 인바운드 포트(공개 API). 산출 불가 시 null(fail-soft). */
interface WalkingDurationUseCase {
    /** 두 지점 사이 도보 소요(분). 산출 불가 시 null. */
    fun walkingMinutes(
        from: Coordinate,
        to: Coordinate,
    ): Int?

    /**
     * 연속한 지점쌍별 도보 소요(분) 목록. 반환 크기 = points.size - 1.
     * - 분(양수): 도보 이동 가능.
     * - -1: 도보 불가(서비스 불가 구간 NoServiceArea·1시간 초과) — 프론트 "걸어갈 수 없는 거리". 예: [10, -1, 8].
     * 한 구간이라도 산출 불가(일시적 오류)면 부분 결과 대신 예외로 실패한다(-1 오인 방지).
     */
    fun walkingSegments(points: List<Coordinate>): List<Int>
}
