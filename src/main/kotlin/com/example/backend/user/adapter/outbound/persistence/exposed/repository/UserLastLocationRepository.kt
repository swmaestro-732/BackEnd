package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.common.geo.Coordinate
import com.example.backend.common.persistence.postgis.stDWithin
import com.example.backend.user.adapter.outbound.persistence.exposed.UserLastLocationTable
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.QueryParameter
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.not
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.upsert
import org.springframework.stereotype.Repository
import kotlin.time.Instant

/** user_last_locations 테이블 접근 리포지토리. 사용자당 한 행을 덮어쓴다. */
@Repository
class UserLastLocationRepository {
    fun isRecentNear(
        userId: Long,
        location: Coordinate,
    ): Boolean =
        UserLastLocationTable
            .select(UserLastLocationTable.userId)
            .where { (UserLastLocationTable.userId eq userId) and recentNear(location) }
            .any()

    /** ON CONFLICT ... WHERE: 조회와 저장 사이에 다른 요청이 같은 자리로 먼저 쓴 경우에도 덮어쓰기를 건너뛴다. */
    fun upsert(
        userId: Long,
        location: Coordinate,
    ) {
        UserLastLocationTable.upsert(
            onUpdate = {
                it[UserLastLocationTable.location] = insertValue(UserLastLocationTable.location)
                it[UserLastLocationTable.updatedAt] = CurrentTimestamp
            },
            where = { not(recentNear(location)) },
        ) {
            it[UserLastLocationTable.userId] = userId
            it[UserLastLocationTable.location] = location
        }
    }

    fun deleteByUserId(userId: Long) {
        UserLastLocationTable.deleteWhere { UserLastLocationTable.userId eq userId }
    }

    /** 저장된 위치가 10분 안에 갱신됐고 [location] 에서 500m 안쪽이다. */
    private fun recentNear(location: Coordinate): Op<Boolean> =
        (UserLastLocationTable.updatedAt greaterEq RefreshCutoff) and
            UserLastLocationTable.location.stDWithin(
                QueryParameter(location, UserLastLocationTable.location.columnType),
                MOVE_THRESHOLD_METERS,
            )

    /** DB 시각 기준 갱신 기준점 `now() - 10분`. updated_at 이 DB now() 로 채워지므로 같은 시계로 비교한다. */
    private object RefreshCutoff : Expression<Instant>() {
        override fun toQueryBuilder(queryBuilder: QueryBuilder) =
            queryBuilder { append("(now() - interval '$REFRESH_MINUTES minutes')") }
    }

    private companion object {
        const val REFRESH_MINUTES = 10
        const val MOVE_THRESHOLD_METERS = 500.0
    }
}
