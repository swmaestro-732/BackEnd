package com.example.backend.user.application.port.inbound

enum class UserActivityKind {
    FOLLOWED,

    /** 언팔로우. 알림 묶음에서 행동자를 뺀다. */
    UNFOLLOWED,

    ;

    fun isRetraction(): Boolean = this == UNFOLLOWED
}
