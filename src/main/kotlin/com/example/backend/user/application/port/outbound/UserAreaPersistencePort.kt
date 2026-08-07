package com.example.backend.user.application.port.outbound

/** 아웃바운드 포트 — 사용자 관심 지역(user_areas) 영속성 계약. */
interface UserAreaPersistencePort {
    /** 사용자의 관심 지역(법정동코드)을 통째로 교체한다 — 기존 행을 지우고 새로 삽입한다. */
    fun replaceAreas(
        userId: Long,
        areaCodes: List<String>,
    )

    /** 사용자의 관심 지역 코드 목록을 등록 순서대로 조회한다. */
    fun findAreaCodes(userId: Long): List<String>
}
