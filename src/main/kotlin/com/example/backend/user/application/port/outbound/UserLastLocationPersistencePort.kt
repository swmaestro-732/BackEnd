package com.example.backend.user.application.port.outbound

import com.example.backend.common.geo.Coordinate

/** 아웃바운드 포트. 사용자별 최근 좌표 1건 저장소. */
interface UserLastLocationPersistencePort {
    /**
     * 활성 사용자면 [userId] 의 최근 좌표를 [location] 으로 덮어쓴다. 행이 없으면 만든다.
     * 탈퇴했거나 없는 사용자면 아무것도 하지 않고, 최근에 같은 자리로 갱신됐으면 생략한다.
     */
    fun saveIfActive(
        userId: Long,
        location: Coordinate,
    )

    fun deleteByUser(userId: Long)
}
