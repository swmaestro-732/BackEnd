package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.common.geo.Coordinate
import com.example.backend.common.persistence.postgis.stDWithin
import com.example.backend.user.adapter.outbound.persistence.exposed.UserLastLocationTable
import com.example.backend.user.adapter.outbound.persistence.exposed.UserTable
import org.jetbrains.exposed.v1.core.QueryParameter
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.not
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
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
     * 활성 사용자일 때만 좌표를 저장한다. 활성 사용자가 아니면 아무것도 쓰지 않는다.
     * - users 행 FOR SHARE: 탈퇴가 그 행을 FOR UPDATE 로 잡고 있으면 커밋까지 기다렸다가 deleted_at 을 다시 평가한다.
     *   탈퇴가 위치 행을 지운 뒤에 다시 써서 탈퇴자 위치가 남는 일을 막는다. FOR SHARE 끼리는 서로 막지 않는다.
     * - ON CONFLICT ... WHERE: 10분 안에 500m 안쪽 재진입이면 덮어쓰지 않아 홈 조회마다 쓰기가 생기지 않는다.
     */
    fun upsertIfActive(
        userId: Long,
        location: Coordinate,
    ) {
        if (!lockActiveShared(userId)) return
        val stale = (clock.instant() - REFRESH_INTERVAL).toKotlinInstant()
        UserLastLocationTable.upsert(
            onUpdate = {
                it[UserLastLocationTable.location] = insertValue(UserLastLocationTable.location)
                it[UserLastLocationTable.updatedAt] = CurrentTimestamp
            },
            where = {
                (UserLastLocationTable.updatedAt less stale) or
                    not(
                        UserLastLocationTable.location.stDWithin(
                            QueryParameter(location, UserLastLocationTable.location.columnType),
                            MOVE_THRESHOLD_METERS,
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

    // ponytail: #144 의 UserRepository.lockActiveShared 와 같은 잠금이다. #144 머지 후 그쪽으로 합친다.
    private fun lockActiveShared(userId: Long): Boolean =
        UserTable
            .select(UserTable.id)
            .where { (UserTable.id eq userId) and UserTable.deletedAt.isNull() }
            .forUpdate(ForUpdateOption.PostgreSQL.ForShare)
            .any()

    private companion object {
        val REFRESH_INTERVAL: Duration = Duration.ofMinutes(10)
        const val MOVE_THRESHOLD_METERS = 500.0
    }
}
