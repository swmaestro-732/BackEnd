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
class UserLastLocationRepository(
    private val userRepository: UserRepository,
) {
    /**
     * 활성 사용자일 때만 좌표를 저장한다. 활성 사용자가 아니면 아무것도 쓰지 않는다.
     * - 먼저 잠금 없이 조회해 10분 안에 500m 안쪽 재진입이면 바로 끝낸다. 홈 조회 대부분은 여기서 끝나 잠금과 쓰기가 없다.
     * - users 행 FOR SHARE: 탈퇴가 그 행을 FOR UPDATE 로 잡고 있으면 커밋까지 기다렸다가 deleted_at 을 다시 평가한다.
     *   탈퇴가 위치 행을 지운 뒤에 다시 써서 탈퇴자 위치가 남는 일을 막는다. FOR SHARE 끼리는 서로 막지 않는다.
     * - ON CONFLICT ... WHERE: 조회와 잠금 사이에 다른 요청이 먼저 쓴 경우에도 같은 조건으로 덮어쓰기를 건너뛴다.
     * 10분 판단은 updated_at 과 같은 DB 시각(now())으로 한다.
     */
    fun upsertIfActive(
        userId: Long,
        location: Coordinate,
    ) {
        val isRecent =
            UserLastLocationTable
                .select(UserLastLocationTable.userId)
                .where { (UserLastLocationTable.userId eq userId) and recentNear(location) }
                .any()
        if (isRecent) return
        if (userId !in userRepository.lockActiveShared(listOf(userId))) return
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
            queryBuilder { append("(now() - interval '10 minutes')") }
    }

    private companion object {
        const val MOVE_THRESHOLD_METERS = 500.0
    }
}
