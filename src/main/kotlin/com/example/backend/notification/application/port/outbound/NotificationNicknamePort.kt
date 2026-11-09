package com.example.backend.notification.application.port.outbound

/**
 * 행동자 닉네임 조회. 탈퇴 정리에서도 쓰므로 사용자 잠금([NotificationActorLookupPort])과 분리해
 * user 의 탈퇴 서비스를 다시 참조하지 않게 한다(빈 순환 방지).
 */
interface NotificationNicknamePort {
    /** 활성 사용자만 담는다. 탈퇴했거나 없는 id 는 결과에서 빠진다. */
    fun findNicknames(userIds: Collection<Long>): Map<Long, String>
}
