package com.example.backend.user.application.port.outbound

/**
 * 아웃바운드 포트 — 사용자의 관심 태그(코스 태그) 저장. `user_like_tags` 조인 테이블을 다룬다.
 * tag_id 는 course 도메인 `tags` 를 가리키는 크로스 도메인 참조라 FK 없이 id 로만 저장한다.
 */
interface UserLikeTagPort {
    /** 사용자의 관심 태그를 [tagIds] 로 전체 치환한다(기존 전부 삭제 후 삽입). 빈 목록이면 전부 삭제. */
    fun replaceLikeTags(
        userId: Long,
        tagIds: List<Long>,
    )

    /** 사용자의 관심 태그 id 목록을 조회한다. 저장 순서를 담는 컬럼이 없어 tag_id 오름차순으로 안정 정렬한다. */
    fun findLikeTagIds(userId: Long): List<Long>
}
