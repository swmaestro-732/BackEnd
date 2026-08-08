package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseCategory
import com.example.backend.course.domain.model.CoursePlace
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseVisibility
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock

// LongIdTable 이 id(EntityID<Long>, "id" 컬럼)와 primaryKey 를 제공한다 → DAO(CourseEntity)·DSL 공용.
// created_at·updated_at·카운터에 클라이언트 기본값을 둔다 — DAO(.new)는 batch insert 라 DB 전용 DEFAULT 를
// 지원하지 않기 때문(없으면 BatchDataInconsistentException). 타임스탬프는 이 정의상 앱 생성이 된다.
internal object CourseTable : LongIdTable("courses") {
    val status = enumerationByName<CourseStatus>("status", 32)
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
    val deletedAt = timestamp("deleted_at").nullable()
    val userId = long("user_id") // cross-domain(user): FK 없음
    val title = varchar("title", 200)
    val coverImageUrl = text("cover_image_url").nullable()
    val description = text("description").nullable()
    val category = enumerationByName<CourseCategory>("category", 50).nullable()
    val area = varchar("area", 100).nullable()

    // 법정동코드 10자리(V12). sigungu_code 는 DB 파생 컬럼(앞 5자리 자동 계산)이라 매핑하지 않는다.
    val areaCode = varchar("area_code", 10).nullable()
    val visitDate = date("visit_date").nullable()
    val isPublished = bool("is_published")
    val visibility = enumerationByName<CourseVisibility>("visibility", 32)
    val likesCnt = integer("likes_cnt").default(0)
    val commentsCnt = integer("comments_cnt").default(0)
    val savesCnt = integer("saves_cnt").default(0)
    val tracingsCnt = integer("tracings_cnt").default(0)
    val forkedFromId = long("forked_from_id").nullable() // 포크 원본 course (같은 도메인)
}

/**
 * courses 테이블의 DAO 엔티티([CourseTable] 과 한 쌍이라 같은 파일에 둔다). 같은 테이블을 DSL 로도 조회할 수 있다(DAO·DSL 공용).
 * 트랜잭션에 묶인 가변 영속 객체이므로 어댑터(outbound) 밖으로 내보내지 않고, 읽기 모델은 순수 DTO 로 변환해 반환한다.
 * 컬럼 타입은 위임(`by`)으로 추론한다 — created_at·updated_at·카운터 등 DB DEFAULT 컬럼은 new 시 미지정으로 두면 DB 기본값이 채운다.
 */
internal class CourseEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<CourseEntity>(CourseTable)

    var status by CourseTable.status
    var createdAt by CourseTable.createdAt
    var updatedAt by CourseTable.updatedAt
    var deletedAt by CourseTable.deletedAt
    var userId by CourseTable.userId
    var title by CourseTable.title
    var coverImageUrl by CourseTable.coverImageUrl
    var description by CourseTable.description
    var category by CourseTable.category
    var area by CourseTable.area
    var areaCode by CourseTable.areaCode
    var visitDate by CourseTable.visitDate
    var isPublished by CourseTable.isPublished
    var visibility by CourseTable.visibility
    var likesCnt by CourseTable.likesCnt
    var commentsCnt by CourseTable.commentsCnt
    var savesCnt by CourseTable.savesCnt
    var tracingsCnt by CourseTable.tracingsCnt
    var forkedFromId by CourseTable.forkedFromId

    /**
     * DAO 엔티티를 도메인 [Course] 로 변환한다. courses 컬럼(생성된 id·DB 생성값 포함)은 엔티티가,
     * 자식(tags·places)은 인자로 받아 조립한다 — 도메인은 자식의 생성 id 를 담지 않아 입력 애그리거트를 재사용한다.
     * DB 생성값(created_at·updated_at·카운터)은 insert 후 [refresh] 로 적재된 뒤라야 정확하다.
     */
    fun toDomain(
        tags: List<String>,
        places: List<CoursePlace>,
    ): Course =
        Course.reconstitute(
            id = id.value,
            userId = userId,
            status = status,
            title = title,
            description = description,
            coverImageUrl = coverImageUrl,
            category = category,
            area = area,
            areaCode = areaCode,
            visitDate = visitDate,
            visibility = visibility,
            isPublished = isPublished,
            likesCnt = likesCnt,
            commentsCnt = commentsCnt,
            savesCnt = savesCnt,
            tracingsCnt = tracingsCnt,
            forkedFromId = forkedFromId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            tags = tags,
            places = places,
        )
}
