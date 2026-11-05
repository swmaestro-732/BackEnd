package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.common.geo.Coordinate
import com.example.backend.common.persistence.postgis.makePoint
import com.example.backend.common.persistence.postgis.stDWithin
import com.example.backend.user.adapter.outbound.persistence.exposed.UserLastLocationTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.not
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.upsert
import org.springframework.stereotype.Repository
import java.time.Clock
import java.time.Duration
import kotlin.time.toKotlinInstant

/** user_last_locations 테이블 접근 리포지토리. 사용자당 한 행을 덮어쓴다. */
@Repository
class UserLastLocationRepository(
    private val clock: Clock,
) {
    /**
     * 홈 진입마다 불려 쓰기가 많다. 기존 행이 [MIN_UPDATE_INTERVAL] 안에 갱신됐고 [MIN_MOVE_METERS] 안이면
     * 덮어쓰지 않는다(같은 자리 반복 진입 생략).
     */
    fun upsert(
        userId: Long,
        location: Coordinate,
    ) {
        val staleBefore = clock.instant().minus(MIN_UPDATE_INTERVAL).toKotlinInstant()
        UserLastLocationTable.upsert(
            UserLastLocationTable.userId,
            onUpdate = {
                it[UserLastLocationTable.location] = insertValue(UserLastLocationTable.location)
                it[UserLastLocationTable.updatedAt] = CurrentTimestamp
            },
            where = {
                (UserLastLocationTable.updatedAt less staleBefore) or
                    not(
                        UserLastLocationTable.location.stDWithin(
                            makePoint(location.latitude, location.longitude),
                            MIN_MOVE_METERS,
                        ),
                    )
            },
        ) {
            it[UserLastLocationTable.userId] = userId
            it[UserLastLocationTable.location] = location
        }
    }

    fun deleteByUserId(userId: Long) {
        UserLastLocationTable.deleteWhere { UserLastLocationTable.userId eq userId }
    }

    private companion object {
        val MIN_UPDATE_INTERVAL: Duration = Duration.ofMinutes(10)
        const val MIN_MOVE_METERS = 500.0
    }
}
