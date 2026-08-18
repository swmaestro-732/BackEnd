package com.example.backend.place.adapter.inbound.web.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** 리뷰 사진 최대 개수 — 디자인(장소 리뷰 작성 "사진 추가 1/6"). */
private const val MAX_PHOTOS = 6

/** 태그 코드 최대 길이 — `.ai/taxonomy.md` 키워드(coffee, nowait 등) 기준. */
private const val MAX_TAG_CODE_LENGTH = 20

/** 한 리뷰에 선택할 수 있는 태그 최대 개수 — 디자인 태그 칩 그룹(업종별 6 + 공통) 기준 상한. */
private const val MAX_TAGS = 10

/** 한마디 남기기 최대 길이 — place_reviews.content(TEXT)에 여유를 둔 입력 상한. */
private const val MAX_CONTENT_LENGTH = 1000

/**
 * 장소 리뷰 생성 요청(**모킹 API**) — 웹 어댑터 DTO.
 * 노션 명세서(Place · place-review · "장소 리뷰 생성") 본문이 비어 있어
 * 디자인(밴드 I · 장소 리뷰 작성: 별점 → 사진 추가 → 태그 칩 → 한마디 남기기)과
 * `place_reviews`·`place_review_photos`·`place_review_tag_links` 스키마에서 도출했다.
 *
 * - rating: 별점 1~5(디자인 ★ 5개). place_reviews.rating CHECK(1~5)와 동일 범위.
 * - content: "한마디 남기기" 자유 입력. 별점만 남길 수 있어 선택 값(컬럼도 nullable).
 * - photoUrls: 업로드 presign(`POST /api/v1/uploads/presigned-urls`)으로 올린 이미지 URL 목록. 순서가 곧 노출 순서(order_no).
 * - tagCodes: 선택한 태그의 **키워드 코드**(`.ai/taxonomy.md` 장소 리뷰 태그 — coffee, view, nowait …).
 *   `place_review_tags` 마스터가 아직 시드 전이고 태그 목록 조회 API도 없어, id 대신 코드로 받기로 합의했다
 *   (실구현 시 코드 → 마스터 id 변환. 마스터에 code 컬럼 추가가 선행돼야 한다).
 *
 * 작성자(user_id)는 요청 바디가 아니라 `@CurrentUserId`(JWT subject)로 받는다.
 */
data class CreatePlaceReviewRequest(
    @field:Min(1)
    @field:Max(5)
    val rating: Int,
    @field:Size(max = MAX_CONTENT_LENGTH)
    val content: String? = null,
    @field:Size(max = MAX_PHOTOS)
    val photoUrls: List<
        @NotBlank
        String,
    > = emptyList(),
    @field:Size(max = MAX_TAGS)
    val tagCodes: List<
        @NotBlank
        @Size(max = MAX_TAG_CODE_LENGTH)
        String,
    > = emptyList(),
)
