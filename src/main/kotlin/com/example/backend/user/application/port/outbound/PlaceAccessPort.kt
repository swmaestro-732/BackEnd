package com.example.backend.user.application.port.outbound

/**
 * 아웃바운드 포트 — user 도메인이 place 도메인에 필요한 것(저장 대상 장소의 존재 검증 + 카테고리 스냅샷)을 요청하는 경계.
 *
 * user 코어는 place 를 직접 알지 않고 이 포트에만 의존한다(MSA 분리 대비). 실제 place 인바운드 포트
 * 호출은 ACL 어댑터([com.example.backend.user.adapter.outbound.place.PlaceAccessAdapter])가 담당하며,
 * 도메인 분리 시 그 어댑터만 REST 클라이언트로 교체하면 코어는 무변경이다.
 */
interface PlaceAccessPort {
    /** 장소 요약을 반환한다. 존재하지 않거나 삭제된 장소면 null — 저장 대상 존재 검증에 그대로 쓴다. */
    fun findPlace(placeId: Long): PlaceRef?
}

/**
 * user 소유 DTO — place 도메인 타입(place 의 PlaceSummary)이 코어 경계 밖으로 새지 않게 한다.
 * user 가 실제로 쓰는 필드만 담는다: 존재 검증([id]) + 저장 시 복사하는 카테고리 스냅샷([category]).
 */
data class PlaceRef(
    val id: Long,
    val category: String,
)
