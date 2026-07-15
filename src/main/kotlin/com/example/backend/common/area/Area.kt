package com.example.backend.common.area

/**
 * 지역(Area) 공통 참조 타입.
 *
 * 팀 결정에 따라 지역은 특정 도메인이 아니라 [common] 에 두고 여러 도메인이 공유 참조한다.
 * 현재는 varchar 로 백킹되는 단순 타입 래퍼이며, 도메인을 참조하지 않는다(단방향 유지).
 */
@JvmInline
value class Area(
    val value: String,
)
