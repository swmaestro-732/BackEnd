package com.example.backend.user.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/**
 * oauth_credentials 테이블 정의(SCRUM-466 계정 분리). (provider, social_id) 한 쌍 = 1 row 이며 identity 에 매달린다.
 * (provider, social_id) 는 전역 UNIQUE — 같은 소셜 계정이 여러 identity 에 링크되지 않는다.
 * 실제 스키마는 Flyway(V4)가 생성한다.
 */
internal object OAuthCredentialTable : LongIdTable("oauth_credentials") {
    val identityId = long("identity_id")
    val provider = varchar("provider", 20)
    val socialId = varchar("social_id", 255)
}
