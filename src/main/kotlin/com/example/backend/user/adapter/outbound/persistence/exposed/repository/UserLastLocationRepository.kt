package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.common.geo.Coordinate
import com.example.backend.common.persistence.postgis.GeographyPointColumnType
import com.example.backend.user.adapter.outbound.persistence.exposed.UserLastLocationTable
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.springframework.stereotype.Repository

/** user_last_locations 테이블 접근 리포지토리. 사용자당 한 행을 덮어쓴다. */
@Repository
class UserLastLocationRepository {
    /**
     * 활성 사용자 확인, 행 잠금, upsert 를 한 문장으로 처리한다. Exposed upsert 는 INSERT ... SELECT ... FOR SHARE 를
     * 만들 수 없어 SQL 을 직접 쓴다. 활성 사용자가 아니면 0행이다.
     */
    fun upsertIfActive(
        userId: Long,
        location: Coordinate,
    ) {
        TransactionManager.current().exec(
            UPSERT_IF_ACTIVE_SQL,
            listOf(GeographyPointColumnType() to location, LongColumnType() to userId),
            StatementType.INSERT,
        )
    }

    fun deleteByUserId(userId: Long) {
        UserLastLocationTable.deleteWhere { UserLastLocationTable.userId eq userId }
    }

    private companion object {
        /**
         * - FOR SHARE: 탈퇴가 users 행을 FOR UPDATE 로 잡고 있으면 커밋까지 기다렸다가 deleted_at 을 다시 평가해 0행이 된다.
         *   FOR SHARE 끼리는 충돌하지 않아 다른 공유 잠금과 경합하지 않는다.
         * - ON CONFLICT ... WHERE: 10분 안에 500m 안쪽 재진입이면 덮어쓰지 않아 홈 조회마다 쓰기가 생기지 않는다.
         */
        val UPSERT_IF_ACTIVE_SQL =
            """
            INSERT INTO user_last_locations (user_id, location)
            SELECT u.id, ?
            FROM users u
            WHERE u.id = ? AND u.deleted_at IS NULL
            FOR SHARE
            ON CONFLICT (user_id) DO UPDATE
                SET location = EXCLUDED.location, updated_at = now()
                WHERE user_last_locations.updated_at < now() - interval '10 minutes'
                    OR NOT ST_DWithin(user_last_locations.location, EXCLUDED.location, 500)
            """.trimIndent()
    }
}
