package com.example.backend.course.application.port.outbound

/**
 * 아웃바운드 포트 — 코스 발행/공개범위변경/삭제로 작성자의 공개범위별 코스 개수가 바뀌었음을 알린다.
 * course 애플리케이션은 이 포트(자기 도메인 계약)만 알고, user 쪽 인바운드 호출은 어댑터가 담당한다.
 * MSA 분리 시 이 포트의 어댑터만 user 서비스 클라이언트(HTTP/메시지)로 바꿔 끼우면 서비스 코드는 그대로다.
 * 크로스 도메인 경계라 공개범위 enum 대신 버킷별 원시 int 델타만 넘긴다.
 */
interface AuthorCourseCountPort {
    fun applyDelta(
        authorId: Long,
        publicDelta: Int,
        followerDelta: Int,
        privateDelta: Int,
    )
}
