package com.example.backend.place.application.port.inbound

/**
 * 인바운드 포트 — 외부 제공자에서 장소 영업시간을 가져와 저장한다.
 * 신규 장소 저장 직후 리스너가, 장소 상세 조회 시 BFF 가 호출한다. 미동기화 장소만 실제로 조회한다.
 */
interface PlaceBusinessHoursSyncUseCase {
    /** 주어진 장소 중 미동기화분만 조회·저장하고 동기화한 장소 수를 반환한다. */
    fun sync(placeIds: List<Long>): Int
}
