package com.example.backend.notification.application.port.inbound

/**
 * 인바운드 포트 — 탈퇴 정리 전용. 사용자 조회에 의존하지 않도록 [NotificationUseCase] 와 분리한다
 * (user 탈퇴 흐름이 이 포트를 부르므로, 행위자 조회로 user 를 다시 참조하면 순환 의존이 된다).
 */
interface NotificationCleanupUseCase {
    /**
     * 받은 알림, 마지막 행위자가 이 사용자인 알림(남의 알림함에 남은 닉네임 스냅샷), 수신 설정을 지운다.
     * 같은 user id 로 재가입해도 "처음 계정처럼" 시작하게 한다.
     */
    fun purgeByUser(userId: Long)
}
