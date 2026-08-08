package com.example.backend.user.application.port.outbound

/**
 * 아웃바운드 포트 — 관심 태그(코스 태그) 조회. 태그는 course 도메인 소유라, user 는 이 포트로만 접근하고
 * 어댑터가 course 인바운드에 위임한다. MSA 분리 시 어댑터만 course 서비스 클라이언트로 교체한다.
 */
interface LikeTagPort {
    /** 주어진 태그 id 중 실제 존재하는 것만 반환한다(저장 전 존재 검증용). */
    fun findExistingTagIds(tagIds: List<Long>): Set<Long>

    /** 주어진 태그 id 의 표시 이름을 id → name 으로 반환한다. 없는 id 는 결과에서 빠진다. */
    fun findTagNames(tagIds: List<Long>): Map<Long, String>
}
