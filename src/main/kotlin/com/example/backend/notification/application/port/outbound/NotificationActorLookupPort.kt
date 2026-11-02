package com.example.backend.notification.application.port.outbound

interface NotificationActorLookupPort {
    /** 활성 사용자 행을 잠가 그 id 만 돌려준다. 탈퇴 정리와 같은 잠금이라 기록과 탈퇴를 직렬화한다. */
    fun lockActive(userIds: List<Long>): Set<Long>

    fun findNickname(actorId: Long): String?
}
