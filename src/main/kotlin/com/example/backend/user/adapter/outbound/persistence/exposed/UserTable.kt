package com.example.backend.user.adapter.outbound.persistence.exposed

import com.example.backend.user.domain.model.User
import com.example.backend.user.domain.model.UserStatus
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * Exposed 테이블 정의. 영속성 세부사항이므로 아웃바운드 어댑터 내부에만 둔다.
 * 실제 스키마는 Flyway(V1__init.sql)가 생성한다. users 의 나머지 컬럼은 DB 기본값을 사용하므로
 * 여기서는 user 도메인이 다루는 컬럼만 선언한다(프로필 요약 조회에 필요한 컬럼 포함).
 * LongIdTable 이 id(EntityID<Long>, "id" 컬럼)와 primaryKey 를 제공한다 → DAO(UserEntity)·DSL 공용.
 */
internal object UserTable : LongIdTable("users") {
    val nickname = varchar("nickname", 20)
    val handle = varchar("handle", 30).nullable()
    val bio = text("bio").nullable()
    val profileImageUrl = text("profile_image_url").nullable()

    // NOT NULL 이고 신규 유저는 0/ACTIVE 로 시작한다 — DB DEFAULT(V1) 와 같은 값을 Exposed 기본값으로도 둬서
    // DAO(.new{}) 삽입이 이 컬럼들을 생략해도 채워지게 한다(쓰기=DAO 컨벤션, CourseTable 선례).
    val followersCnt = integer("followers_cnt").default(0)
    val followingsCnt = integer("followings_cnt").default(0)

    // 공개범위별 발행 코스 개수 캐시(마이페이지가 매 조회 GROUP BY 대신 읽는다). CourseService 가 ±1 로 유지.
    val publicCoursesCnt = integer("public_courses_cnt").default(0)
    val followerCoursesCnt = integer("follower_courses_cnt").default(0)
    val privateCoursesCnt = integer("private_courses_cnt").default(0)
    val status = enumerationByName<UserStatus>("status", 32).default(UserStatus.ACTIVE)

    // SCRUM-466 계정 분리: users 는 identity 에 매달린다. identityId 는 expand 단계라 nullable(구버전이 안 채움) — NOT NULL 승격은 후속.
    // isPrimary 는 identity 의 기본(로그인) 프로필 여부. DAO(.new{})가 생략해도 채워지게 DB DEFAULT 와 같은 기본값을 둔다.
    val identityId = long("identity_id").nullable()
    val isPrimary = bool("is_primary").default(true)

    // 롤링 배포 호환용 레거시 컬럼(V5에서 드롭). 신규 경로는 자격증명을 oauth_credentials 에 쓰되,
    // 구버전 인스턴스의 findBySocial(users.social_*)이 새 계정을 찾도록 여기에도 dual-write 하고,
    // 구버전이 users.social_* 로만 만든 계정은 credential 조회 실패 시 이 컬럼으로 폴백해 읽는다.
    val socialProvider = varchar("social_provider", 20).nullable()
    val socialId = varchar("social_id", 255).nullable()
    val deletedAt = timestamp("deleted_at").nullable()
}

/**
 * users 테이블의 DAO 엔티티([UserTable] 과 한 쌍이라 같은 파일에 둔다). 같은 테이블을 DSL 로도 조회할 수 있다(DAO·DSL 공용).
 * 트랜잭션에 묶인 가변 영속 객체이므로 어댑터(outbound) 밖으로 내보내지 않고, 읽기 모델은 순수 도메인/DTO 로 변환해 반환한다.
 */
internal class UserEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<UserEntity>(UserTable)

    var nickname by UserTable.nickname
    var handle by UserTable.handle
    var bio by UserTable.bio
    var profileImageUrl by UserTable.profileImageUrl
    var followersCnt by UserTable.followersCnt
    var followingsCnt by UserTable.followingsCnt
    var publicCoursesCnt by UserTable.publicCoursesCnt
    var followerCoursesCnt by UserTable.followerCoursesCnt
    var privateCoursesCnt by UserTable.privateCoursesCnt
    var status by UserTable.status
    var identityId by UserTable.identityId
    var isPrimary by UserTable.isPrimary
    var deletedAt by UserTable.deletedAt

    /** DAO 엔티티를 도메인 [User] 로 변환한다. */
    fun toDomain(): User =
        User.reconstitute(
            id = id.value,
            nickname = nickname,
            handle = handle,
            bio = bio,
            profileImageUrl = profileImageUrl,
            status = status,
        )
}
