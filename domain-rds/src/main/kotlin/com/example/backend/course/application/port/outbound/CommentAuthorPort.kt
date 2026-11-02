package com.example.backend.course.application.port.outbound

/** 댓글 작성자 표시 정보. user 도메인 값을 course 경계 안으로 복사한 것이다. */
data class CommentAuthor(
    val id: Long,
    val nickname: String,
    val profileImageUrl: String?,
)

/**
 * 아웃바운드 포트 — 댓글 작성자 표시 정보 조회. 사용자는 user 도메인 소유라 어댑터가 user 인바운드 포트에 위임한다
 * (크로스 도메인 격리). MSA 분리 시 어댑터만 서비스 클라이언트로 교체한다.
 */
interface CommentAuthorPort {
    /** 작성자 id → 표시 정보. 탈퇴했거나 없는 사용자는 결과에서 빠진다(호출측이 대체 닉네임 처리). */
    fun findAuthors(userIds: Collection<Long>): Map<Long, CommentAuthor>
}
