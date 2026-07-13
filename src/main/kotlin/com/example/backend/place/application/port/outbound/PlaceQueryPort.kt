package com.example.backend.place.application.port.outbound

import com.example.backend.place.domain.model.PlaceBusinessStatus
import java.time.Instant
import java.time.LocalTime

/** 아웃바운드 포트 — 장소 상세 조회에 필요한 영속성 질의. */
interface PlaceQueryPort {
    /** 삭제되지 않은 장소를 조회한다. 좌표는 PostGIS location에서 추출. */
    fun findPlace(placeId: Long): PlaceRecord?

    fun findBusinessHours(placeId: Long): List<BusinessHourRecord>

    /** 최신순 리뷰 [limit]개 (사진 URL 포함). */
    fun findRecentReviews(
        placeId: Long,
        limit: Int,
    ): List<ReviewRecord>

    fun countReviews(placeId: Long): Int

    /** 리뷰가 없으면 null. */
    fun averageRating(placeId: Long): Double?

    data class PlaceRecord(
        val id: Long,
        val name: String,
        val category: String,
        val imageUrl: String?,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val businessStatus: PlaceBusinessStatus,
    )

    data class BusinessHourRecord(
        val dayOfWeek: Int,
        val openTime: LocalTime?,
        val closeTime: LocalTime?,
    )

    data class ReviewRecord(
        val id: Long,
        val userId: Long,
        val rating: Int,
        val content: String?,
        val createdAt: Instant,
        val photoUrls: List<String>,
    )
}
