package com.example.backend.course.application.port.outbound

/** 아웃바운드 포트: 어드민 목록의 작성자 닉네임 표시와 닉네임 검색(user 도메인). */
interface CourseAuthorPort {
    /** 탈퇴했거나 없는 사용자는 결과에서 빠진다. */
    fun findNicknames(userIds: Collection<Long>): Map<Long, String>

    fun findIdsByNickname(
        keyword: String,
        limit: Int,
    ): List<Long>
}
