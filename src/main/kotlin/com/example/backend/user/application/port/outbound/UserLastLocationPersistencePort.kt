package com.example.backend.user.application.port.outbound

import com.example.backend.common.geo.Coordinate

/** 아웃바운드 포트. 사용자별 최근 좌표 1건 저장소. */
interface UserLastLocationPersistencePort {
    /** [userId] 의 저장된 위치가 10분 안에 갱신됐고 [location] 에서 500m 안쪽이면 true. 잠금 없이 읽기만 한다. */
    fun isRecentNear(
        userId: Long,
        location: Coordinate,
    ): Boolean

    /**
     * [userId] 의 최근 좌표를 [location] 으로 덮어쓴다. 행이 없으면 만든다.
     * 그 사이 다른 요청이 같은 자리로 갱신했으면([isRecentNear] 조건) 덮어쓰지 않는다. 활성 사용자 확인은 호출부가 한다.
     */
    fun save(
        userId: Long,
        location: Coordinate,
    )

    fun deleteByUser(userId: Long)
}
