package com.example.backend.user.domain.model

/** 사용자 상태(users.status). enum 이름 문자열로 저장한다(enumerationByName — V3 enum 저장 컨벤션). */
enum class UserStatus {
    ACTIVE,
    SUSPENDED,
    PENDING,
    WITHDRAWN,
    DELETED,
}
