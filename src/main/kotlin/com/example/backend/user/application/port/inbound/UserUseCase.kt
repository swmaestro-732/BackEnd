package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.port.inbound.dto.UserProfileResult

/**
 * 인바운드 포트 — 애플리케이션이 바깥(웹 등)에 제공하는 유스케이스 계약.
 * 입출력은 도메인이 아니라 애플리케이션 DTO(Command/Result)로 주고받는다.
 */
interface UserUseCase {
    /** 단건 프로필 조회 — 코스 상세의 작성자 카드 등에서 사용. */
    fun getProfile(
        userId: Long,
        viewerId: Long?,
    ): UserProfileResult

    /** 핸들로 프로필 조회 — 마이페이지(타인) 등에서 사용. 없으면 USER_NOT_FOUND. [getProfile] 과 같은 결과 형태. */
    fun getProfileByHandle(
        handle: String,
        viewerId: Long?,
    ): UserProfileResult

    /** 핸들(아이디) 사용 가능 여부. 예약어이거나 이미 사용 중이면 false. 회원가입 시 handle 입력 검증에 쓴다. */
    fun isHandleAvailable(handle: String): Boolean

    /** 회원 탈퇴 — 현재 사용자 소프트 삭제 + 리프레시 토큰 전량 폐기. 재가입 시 같은 소셜로 재활성화 가능. */
    fun withdraw(userId: Long)
}
