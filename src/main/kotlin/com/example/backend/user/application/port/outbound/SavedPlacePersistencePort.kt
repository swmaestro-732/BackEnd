package com.example.backend.user.application.port.outbound

import com.example.backend.user.domain.model.SavedPlace
import com.example.backend.user.domain.model.SavedPlaceCategory
import java.time.Instant

/**
 * 아웃바운드 포트 — 저장 장소(saved_places) 접근.
 * 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 * 모든 조회·삭제는 살아있는(deleted_at IS NULL) 행만 대상으로 한다.
 */
interface SavedPlacePersistencePort {
    /** (user_id, place_id) 저장 레코드가 이미 있는지. 중복 저장 차단(409)에 쓴다. */
    fun existsSavedPlace(
        userId: Long,
        placeId: Long,
    ): Boolean

    /**
     * 저장 레코드를 삽입하고 생성값(id·created_at)까지 적재된 도메인 [SavedPlace] 를 반환한다. category 는 장소 카테고리 스냅샷.
     * 같은 (user, place) 의 소프트 삭제된 레코드가 남아 있으면 지우고 새로 발행한다 —
     * 저장·취소를 반복해도 장소당 행은 하나이고, id 는 항상 저장 시각 순으로 증가한다.
     */
    fun insert(
        userId: Long,
        placeId: Long,
        category: SavedPlaceCategory?,
    ): SavedPlace

    /** (user_id, place_id) 저장 레코드를 소프트 삭제한다. 실제 삭제된 행이 있으면 true(없으면 false). */
    fun deleteByUserAndPlace(
        userId: Long,
        placeId: Long,
    ): Boolean

    /** 방문 여부별 저장 장소 개수(미방문/방문 탭 배지). */
    fun countByVisited(
        userId: Long,
        visited: Boolean,
    ): Long

    /** 카테고리별 저장 장소 개수(카테고리 칩 배지). 카테고리 미분류(null)·0개 카테고리는 결과에서 빠진다. */
    fun countByCategory(userId: Long): List<SavedPlaceCategoryCountRow>

    /**
     * 저장 레코드를 id 내림차순(최신 저장순)으로 조회한다.
     * visited 로 미방문/방문 탭을, category(null 이면 전체)로 카테고리 칩을 필터링하고,
     * cursorId 가 있으면 그보다 작은 id 만(다음 페이지), limit 개까지 반환한다.
     */
    fun findPage(
        userId: Long,
        visited: Boolean,
        category: SavedPlaceCategory?,
        cursorId: Long?,
        limit: Int,
    ): List<SavedPlaceRow>
}

/** 저장 레코드 읽기 모델 — 조회 전용. */
data class SavedPlaceRow(
    val id: Long,
    val placeId: Long,
    val category: SavedPlaceCategory?,
    val visited: Boolean,
    val savedAt: Instant,
)

/** 카테고리별 저장 개수 읽기 모델(저장함 카테고리 칩). */
data class SavedPlaceCategoryCountRow(
    val category: SavedPlaceCategory,
    val count: Long,
)
