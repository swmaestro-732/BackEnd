package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.application.port.outbound.UserPersistencePort
import com.example.backend.user.domain.model.User
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/**
 * 아웃바운드 어댑터 — [UserPersistencePort] 를 Exposed 로 구현한다.
 * 도메인 ↔ 테이블 행(row) 매핑을 여기서 담당하고, 도메인/애플리케이션 계층은
 * Exposed 를 전혀 알지 못한다. 트랜잭션은 애플리케이션 서비스의 @Transactional 이
 * SpringTransactionManager(exposed-spring-boot-starter)로 열어준다.
 */
@Repository
class UserPersistenceAdapter : UserPersistencePort {
    override fun findAll(): List<User> = UserTable.selectAll().map(::toDomain)

    override fun save(user: User): User {
        val id =
            UserTable.insert {
                it[nickname] = user.nickname
            }[UserTable.id]
        return User.reconstitute(id = id, nickname = user.nickname)
    }

    private fun toDomain(row: ResultRow): User =
        User.reconstitute(
            id = row[UserTable.id],
            nickname = row[UserTable.nickname],
        )
}
