package com.example.backend.place.adapter.outbound.persistence.exposed

import com.example.backend.common.persistence.postgis.geographyPoint
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceStatus
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock

// LongIdTable 이 id(EntityID<Long>, "id" 컬럼)와 primaryKey 를 제공한다 → DAO(PlaceEntity)·DSL 공용.
// created_at·updated_at 에 클라이언트 기본값을 둔다 — DAO(.new)는 batch insert 라 DB 전용 DEFAULT 를
// 지원하지 않기 때문(없으면 BatchDataInconsistentException). 타임스탬프는 이 정의상 앱 생성이 된다.
internal object PlaceTable : LongIdTable("places") {
    val status = enumerationByName<PlaceStatus>("status", 32)
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
    val deletedAt = timestamp("deleted_at").nullable()
    val name = varchar("name", 200)
    val description = text("description").nullable()
    val category = enumerationByName<PlaceCategory>("category", 50)
    val location = geographyPoint("location")
    val address = varchar("address", 255)

    // 법정동코드 10자리(V12). sigungu_code 는 DB 파생 컬럼(앞 5자리 자동 계산)이라 매핑하지 않는다.
    val areaCode = varchar("area_code", 10).nullable()
    val imageUrl = text("image_url").nullable()
    val businessStatus = enumerationByName<PlaceBusinessStatus>("business_status", 32)
    val kakaoPlaceId = varchar("kakao_place_id", 64).nullable()

    // 별점 비정규화 카운터(V6) — 리뷰 작성 +rating/+1, 소프트 삭제 -rating/-1. 평균 = rating_sum / rating_cnt.
    val ratingSum = long("rating_sum").default(0)
    val ratingCnt = integer("rating_cnt").default(0)

    // 화면 통계 카운터(V13) — 살아있는 리뷰의 사진 수와 별점별 리뷰 수. 리뷰 작성 +, 소프트 삭제 -. 별점별 합 = rating_cnt.
    val reviewPhotoCnt = integer("review_photo_cnt").default(0)
    val rating1Cnt = integer("rating_1_cnt").default(0)
    val rating2Cnt = integer("rating_2_cnt").default(0)
    val rating3Cnt = integer("rating_3_cnt").default(0)
    val rating4Cnt = integer("rating_4_cnt").default(0)
    val rating5Cnt = integer("rating_5_cnt").default(0)

    /** 별점(1~5)에 해당하는 카운터 컬럼. */
    fun ratingCountColumn(rating: Int): Column<Int> =
        when (rating) {
            1 -> rating1Cnt
            2 -> rating2Cnt
            3 -> rating3Cnt
            4 -> rating4Cnt
            5 -> rating5Cnt
            else -> error("별점은 1~5 여야 한다: $rating")
        }
}

/**
 * places 테이블의 DAO 엔티티([PlaceTable] 과 한 쌍이라 같은 파일에 둔다). 같은 테이블을 DSL 로도 조회할 수 있다(DAO·DSL 공용).
 * 트랜잭션에 묶인 가변 영속 객체이므로 어댑터(outbound) 밖으로 내보내지 않고, 읽기 모델은 순수 DTO 로 변환해 반환한다.
 * created_at·updated_at 등 클라이언트 기본값 컬럼은 new 시 미지정으로 두면 기본값이 채운다.
 */
internal class PlaceEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<PlaceEntity>(PlaceTable)

    var status by PlaceTable.status
    var createdAt by PlaceTable.createdAt
    var updatedAt by PlaceTable.updatedAt
    var deletedAt by PlaceTable.deletedAt
    var name by PlaceTable.name
    var description by PlaceTable.description
    var category by PlaceTable.category
    var location by PlaceTable.location
    var address by PlaceTable.address
    var areaCode by PlaceTable.areaCode
    var imageUrl by PlaceTable.imageUrl
    var businessStatus by PlaceTable.businessStatus
    var kakaoPlaceId by PlaceTable.kakaoPlaceId
    var ratingSum by PlaceTable.ratingSum
    var ratingCnt by PlaceTable.ratingCnt

    /** DAO 엔티티를 도메인 [Place] 로 변환한다(생성된 id·DB 생성값 포함). */
    fun toDomain(): Place =
        Place.reconstitute(
            id = id.value,
            status = status,
            name = name,
            description = description,
            category = category,
            location = location,
            address = address,
            areaCode = areaCode,
            imageUrl = imageUrl,
            businessStatus = businessStatus,
            kakaoPlaceId = kakaoPlaceId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            ratingSum = ratingSum,
            ratingCnt = ratingCnt,
        )
}
