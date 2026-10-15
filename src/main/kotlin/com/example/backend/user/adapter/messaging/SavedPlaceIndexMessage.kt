package com.example.backend.user.adapter.messaging

/**
 * 저장 장소 색인 폴백 큐(SQS) 와이어 메시지 — 발행(outbound)·수신(inbound) 어댑터가 공유하는 직렬화 계약.
 * 중립 위치(adapter.messaging)에 둬 inbound↔outbound 결합을 만들지 않는다(CourseCountMessage 선례).
 *
 * 동기 색인(AFTER_COMMIT) 이 실패한 연산을 실어 컨슈머가 같은 색인 포트로 재처리한다. upsert/delete 가 멱등이라
 * eventId 없이 연산 종류([op])와 문서 필드만 실으면 충분하다. INDEX 는 전 필드, DELETE 는 키([userId]/[placeId])만 쓴다.
 */
data class SavedPlaceIndexMessage(
    val op: String,
    val userId: Long,
    val placeId: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val savedAtEpochMs: Long? = null,
    val name: String? = null,
    val category: String? = null,
    val imageUrl: String? = null,
    val rating: Double? = null,
    val ratingCount: Int? = null,
    val removedAtEpochMs: Long? = null,
) {
    companion object {
        const val OP_INDEX = "INDEX"
        const val OP_DELETE = "DELETE"
    }
}
