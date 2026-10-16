package com.example.backend.course.application.port.outbound

/**
 * 아웃바운드 포트 — course 가 참조하는 place 들의 요약을 조회한다(발행 시 존재 검증 + 카테고리·지역 도출 입력).
 *
 * course 코어는 place 를 직접 알지 않고 이 포트와 course 소유 DTO([PlaceRef])에만 의존한다(MSA 분리 대비).
 * 실제 place 인바운드 포트 호출은 ACL 어댑터([com.example.backend.course.adapter.outbound.place.PlaceLookupAdapter])가
 * 담당하며, 도메인 분리 시 그 어댑터만 REST 클라이언트로 교체하면 코어는 무변경이다.
 */
interface PlaceLookupPort {
    /** 주어진 place_id 들의 요약을 반환한다(존재하지 않는 id 는 결과에 없다). */
    fun findPlacesByIds(placeIds: List<Long>): List<PlaceRef>
}

/**
 * course 소유 DTO — place 도메인 타입(place 의 PlaceSummary)을 코어 경계 밖으로 새지 않게 한다.
 * course 가 실제로 쓰는 필드만 담는다: 존재 검증([id]) + 코스 카테고리·지역 도출 입력([category]·[areaCode]).
 */
data class PlaceRef(
    val id: Long,
    val category: String,
    val areaCode: String?,
)
