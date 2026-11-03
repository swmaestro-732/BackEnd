package com.example.backend.notification.application.port.outbound

interface NotificationActorLookupPort {
    /** 활성 사용자 행을 공유 잠금으로 잠가 그 id 만 돌려준다. 탈퇴와는 직렬화되고 같은 사람에게 오는 기록끼리는 기다리지 않는다. */
    fun lockActive(userIds: List<Long>): Set<Long>

    fun findNickname(actorId: Long): String?
}
