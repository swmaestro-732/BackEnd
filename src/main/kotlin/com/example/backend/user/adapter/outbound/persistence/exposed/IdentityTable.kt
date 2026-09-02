package com.example.backend.user.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/**
 * identities 테이블 정의(SCRUM-466 계정 분리). 인증 주체 1명 = 1 row.
 * created_at/updated_at 은 DB DEFAULT(now())가 채우므로 여기선 id 만 다룬다.
 * 실제 스키마는 Flyway(V4)가 생성한다.
 */
internal object IdentityTable : LongIdTable("identities")
