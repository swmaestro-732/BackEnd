package com.example.backend.mobile.user.application.port.inbound.dto

import java.time.Instant

/**
 * 마이페이지 코스 카드 재료 (BFF 로컬 DTO) — course 도메인에 의존하지 않도록 mobile 패키지 안에 둔다.
 * course 도메인 담당자의 작성자별 코스 조회(listByAuthor) 구현 후 채운다(담당자 이관). theme 은 카테고리 이름.
 */
data class MyPageCourseInfo(
    val id: Long,
    val title: String,
    val coverImageUrl: String?,
    val theme: String?,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Instant,
)
