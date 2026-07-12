package com.example.backend.place.adapter.outbound.persistence

import com.example.backend.place.application.port.outbound.PlaceQueryPort
import kotlinx.datetime.toJavaLocalTime
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.springframework.stereotype.Repository
import kotlin.time.toJavaInstant

/**
 * 아웃바운드 어댑터 — [PlaceQueryPort] 를 Exposed 로 구현한다.
 * 좌표는 PostGIS geography 컬럼이라 Exposed 매핑 없이 raw SQL(ST_X/ST_Y)로 추출한다.
 */
@Repository
class PlaceQueryPersistenceAdapter : PlaceQueryPort {
    override fun findPlace(placeId: Long): PlaceQueryPort.PlaceRecord? {
        val row =
            PlaceTable
                .selectAll()
                .where { (PlaceTable.id eq placeId) and (PlaceTable.deletedAt.isNull()) }
                .singleOrNull() ?: return null
        val (latitude, longitude) = findCoordinates(placeId) ?: return null
        return PlaceQueryPort.PlaceRecord(
            id = row[PlaceTable.id],
            name = row[PlaceTable.name],
            category = row[PlaceTable.category],
            imageUrl = row[PlaceTable.imageUrl],
            address = row[PlaceTable.address],
            latitude = latitude,
            longitude = longitude,
            businessStatus = row[PlaceTable.businessStatus],
        )
    }

    private fun findCoordinates(placeId: Long): Pair<Double, Double>? =
        TransactionManager.current().exec(
            "SELECT ST_Y(location::geometry) AS lat, ST_X(location::geometry) AS lng FROM places WHERE id = $placeId",
        ) { rs ->
            if (rs.next()) rs.getDouble("lat") to rs.getDouble("lng") else null
        }

    override fun findBusinessHours(placeId: Long): List<PlaceQueryPort.BusinessHourRecord> =
        PlaceBusinessHourTable
            .selectAll()
            .where { PlaceBusinessHourTable.placeId eq placeId }
            .orderBy(PlaceBusinessHourTable.dayOfWeek to SortOrder.ASC)
            .map {
                PlaceQueryPort.BusinessHourRecord(
                    dayOfWeek = it[PlaceBusinessHourTable.dayOfWeek].toInt(),
                    openTime = it[PlaceBusinessHourTable.openTime]?.toJavaLocalTime(),
                    closeTime = it[PlaceBusinessHourTable.closeTime]?.toJavaLocalTime(),
                )
            }

    override fun findRecentReviews(
        placeId: Long,
        limit: Int,
    ): List<PlaceQueryPort.ReviewRecord> {
        val rows =
            PlaceReviewTable
                .selectAll()
                .where { (PlaceReviewTable.placeId eq placeId) and (PlaceReviewTable.deletedAt.isNull()) }
                .orderBy(PlaceReviewTable.createdAt to SortOrder.DESC)
                .limit(limit)
                .toList()
        val photosByReview =
            PlaceReviewPhotoTable
                .selectAll()
                .where { PlaceReviewPhotoTable.placeReviewId inList rows.map { it[PlaceReviewTable.id] } }
                .orderBy(PlaceReviewPhotoTable.orderNo to SortOrder.ASC)
                .groupBy({ it[PlaceReviewPhotoTable.placeReviewId] }, { it[PlaceReviewPhotoTable.imageUrl] })
        return rows.map {
            PlaceQueryPort.ReviewRecord(
                id = it[PlaceReviewTable.id],
                userId = it[PlaceReviewTable.userId],
                rating = it[PlaceReviewTable.rating].toInt(),
                content = it[PlaceReviewTable.content],
                createdAt = it[PlaceReviewTable.createdAt].toJavaInstant(),
                photoUrls = photosByReview[it[PlaceReviewTable.id]].orEmpty(),
            )
        }
    }

    override fun countReviews(placeId: Long): Int =
        PlaceReviewTable
            .selectAll()
            .where { (PlaceReviewTable.placeId eq placeId) and (PlaceReviewTable.deletedAt.isNull()) }
            .count()
            .toInt()

    override fun averageRating(placeId: Long): Double? =
        TransactionManager.current().exec(
            "SELECT AVG(rating)::float8 AS avg_rating FROM place_reviews WHERE place_id = $placeId AND deleted_at IS NULL",
        ) { rs ->
            if (rs.next()) rs.getObject("avg_rating") as Double? else null
        }
}
