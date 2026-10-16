package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.UserAreaTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository
import java.time.Clock
import kotlin.time.toKotlinInstant

/** user_areas 테이블 접근 리포지토리 — 회원가입·프로필 수정의 관심 지역(법정동코드) 저장. */
@Repository
class UserAreaRepository(
    private val clock: Clock,
) {
    /** 사용자의 관심 지역을 전체 치환한다 — 재가입(재활성화) 시 이전 행이 남지 않게 한다. */
    fun replaceByUserId(
        userId: Long,
        areaCodes: List<String>,
    ) {
        UserAreaTable.deleteWhere { UserAreaTable.userId eq userId }
        // batchInsert 는 DB 기본값(now())에 기대는 걸 허용하지 않아 updated_at 을 명시한다.
        val now = clock.instant().toKotlinInstant()
        UserAreaTable.batchInsert(areaCodes) { code ->
            this[UserAreaTable.userId] = userId
            this[UserAreaTable.areaCode] = code
            this[UserAreaTable.updatedAt] = now
        }
    }

    /** 사용자의 관심 지역 코드 목록을 등록 순서(id 오름차순)대로 조회한다. */
    fun findByUserId(userId: Long): List<String> =
        UserAreaTable
            .select(UserAreaTable.areaCode)
            .where { UserAreaTable.userId eq userId }
            .orderBy(UserAreaTable.id)
            .map { it[UserAreaTable.areaCode] }
}
