package com.example.backend.place.application.port.inbound

/**
 * 인바운드 포트 — 외부 제공자에서 장소 영업시간을 가져와 저장한다. 미동기화 장소만 실제로 조회한다.
 * [sync] 는 커밋 후 비동기 리스너가, [requestSync] 는 상세 조회 같은 요청 경로가 부른다.
 */
interface PlaceBusinessHoursSyncUseCase {
    /** 주어진 장소 중 미동기화분만 조회·저장하고 동기화한 장소 수를 반환한다. */
    fun sync(placeIds: List<Long>): Int

    /** 미동기화 장소가 있으면 동기화 요청 이벤트를 발행한다. 실제 호출은 호출부 트랜잭션 커밋 후 비동기로 일어난다. */
    fun requestSync(placeIds: List<Long>)
}
