package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.port.inbound.dto.CreateUserCommand
import com.example.backend.user.application.port.inbound.dto.UserProfileResult
import com.example.backend.user.application.port.inbound.dto.UserResult

/**
 * 인바운드 포트 — 애플리케이션이 바깥(웹 등)에 제공하는 유스케이스 계약.
 * 입출력은 도메인이 아니라 애플리케이션 DTO(Command/Result)로 주고받는다.
 */
interface UserUseCase {
    fun list(): List<UserResult>

    fun create(command: CreateUserCommand): UserResult

    /** 단건 프로필 조회 — 코스 상세의 작성자 카드 등에서 사용. */
    fun getProfile(
        userId: Long,
        viewerId: Long?,
    ): UserProfileResult

    /**
     * 여러 프로필을 한 번에 조회 — 저장함·피드 등 작성자 카드가 여럿인 목록 화면용(작성자 수만큼의 N+1 회피).
     * 없는·삭제된 사용자는 결과에서 빠진다(단건과 달리 예외를 던지지 않음). 반환 순서는 보장하지 않는다.
     */
    fun getProfiles(
        userIds: List<Long>,
        viewerId: Long?,
    ): List<UserProfileResult>

    /** 핸들(아이디) 사용 가능 여부. 예약어이거나 이미 사용 중이면 false. 회원가입 시 handle 입력 검증에 쓴다. */
    fun isHandleAvailable(handle: String): Boolean
}
