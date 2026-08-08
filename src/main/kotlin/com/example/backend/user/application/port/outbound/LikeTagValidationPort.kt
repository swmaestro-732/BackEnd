package com.example.backend.user.application.port.outbound

/**
 * 아웃바운드 포트 — 관심 태그(코스 태그) 존재 검증. 태그는 course 도메인 소유라, user 는 이 포트로만 접근하고
 * 어댑터가 course 인바운드에 위임한다. MSA 분리 시 어댑터만 course 서비스 클라이언트로 교체한다.
 */
interface LikeTagValidationPort {
    /** 주어진 태그 id 중 실제 존재하는 것만 반환한다. */
    fun findExistingTagIds(tagIds: List<Long>): Set<Long>
}
