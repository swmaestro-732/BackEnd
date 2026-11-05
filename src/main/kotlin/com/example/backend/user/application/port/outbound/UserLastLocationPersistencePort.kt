package com.example.backend.user.application.port.outbound

import com.example.backend.common.geo.Coordinate

/** 아웃바운드 포트. 사용자별 최근 좌표 1건 저장소. */
interface UserLastLocationPersistencePort {
    /** [userId] 의 최근 좌표를 [location] 으로 덮어쓴다. 행이 없으면 만든다. */
    fun save(
        userId: Long,
        location: Coordinate,
    )

    fun find(userId: Long): Coordinate?

    fun deleteByUser(userId: Long)
}
