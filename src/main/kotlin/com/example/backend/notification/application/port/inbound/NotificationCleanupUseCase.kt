package com.example.backend.notification.application.port.inbound

/**
 * 인바운드 포트: 탈퇴 정리 전용. 사용자 잠금에 의존하지 않도록 [NotificationUseCase] 와 분리한다
 * (user 탈퇴 흐름이 이 포트를 부르므로, 잠금 때문에 user 의 탈퇴 서비스를 다시 참조하면 순환 의존이 된다).
 * 닉네임은 탈퇴 서비스와 무관한 사용자 요약 조회로 얻는다.
 */
interface NotificationCleanupUseCase {
    /**
     * 받은 알림과 수신 설정을 지운다. 이 사용자가 들어간 묶음은 행동자에서 빼고 인원을 남은 사람 수로 맞추며,
     * 대표 행동자였으면 남은 사람과 그 닉네임으로 바꾼다. 남은 사람이 없으면 지운다.
     * 같은 user id 로 재가입해도 "처음 계정처럼" 시작하게 한다.
     */
    fun purgeByUser(userId: Long)

    /** 보관 기간([RETENTION])이 지난 알림을 지우고 지운 수를 돌려준다. */
    fun purgeExpired(): Int

    companion object {
        val RETENTION: java.time.Duration = java.time.Duration.ofDays(90)
    }
}
