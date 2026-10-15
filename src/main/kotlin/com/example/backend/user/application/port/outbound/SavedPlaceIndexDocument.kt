package com.example.backend.user.application.port.outbound

import com.example.backend.common.geo.Coordinate
import java.time.Instant

/**
 * 저장 관계와 장소 좌표 + 표시용 정적 정보 스냅샷의 색인 문서. 검색엔진 타입은 어댑터 안에서만 사용한다.
 * 홈 근처 조회가 ES 결과만으로 카드를 그리도록 이름/카테고리/이미지/평점을 중복 저장한다(DB 왕복 제거).
 * 방문 여부(visited)는 가변 관계 속성이라 색인하지 않고 조회 시 DB 에서 읽는다. 평점은 저장/재색인 시점 스냅샷.
 */
data class SavedPlaceIndexDocument(
    val userId: Long,
    val placeId: Long,
    val location: Coordinate,
    val savedAt: Instant,
    val name: String,
    val category: String,
    val imageUrl: String?,
    val rating: Double,
    val ratingCount: Int,
)
