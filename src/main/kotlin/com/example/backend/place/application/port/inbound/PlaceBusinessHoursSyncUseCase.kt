package com.example.backend.place.application.port.inbound

/**
 * 인바운드 포트 — 외부 제공자에서 장소 영업시간을 가져와 저장한다.
 * 신규 장소 저장 후 리스너가 [sync] 를, 부팅 러너가 [backfillAll] 을 호출한다.
 */
interface PlaceBusinessHoursSyncUseCase {
    /** 주어진 장소 중 미동기화분만 조회·저장하고 동기화한 장소 수를 반환한다. */
    fun sync(placeIds: List<Long>): Int

    /** 미동기화 장소 전체를 페이지 단위로 조회·저장하고 동기화한 장소 수를 반환한다. */
    fun backfillAll(): Int
}
