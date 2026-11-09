package com.example.backend.user.application.port.inbound

/**
 * 인바운드 포트 — 다른 도메인(place/course)·bff가 사용자 표시 정보를 조회할 때 쓰는 공개 API.
 * 크로스 도메인 참조는 inbound 포트만 허용되므로(ArchUnit) 반환 타입도 이 패키지에 둔다.
 */
interface UserSummaryUseCase {
    /** 존재하는 사용자만 반환한다(탈퇴/미존재 id는 결과에서 빠짐 — 호출측이 placeholder 처리). */
    fun findSummaries(ids: Collection<Long>): List<UserSummary>

    /** 닉네임에 [keyword] 가 들어간 활성 사용자 id 를 최대 [limit] 개 돌려준다(어드민 검색용). */
    fun findIdsByNickname(
        keyword: String,
        limit: Int,
    ): List<Long>

    data class UserSummary(
        val id: Long,
        val nickname: String,
        val profileImageUrl: String?,
        val handle: String? = null,
    )
}
