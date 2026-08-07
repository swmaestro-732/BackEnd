package com.example.backend.user.application.port.inbound.dto

/** 유스케이스 출력 — 사용자 관심 지역 한 건. [name] 은 area 마스터에서 푼 표시 이름(동/시군구 짧은 이름). */
data class UserAreaResult(
    val code: String,
    val name: String,
)
