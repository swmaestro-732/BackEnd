package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.CourseCategoryQueryUseCase
import com.example.backend.course.domain.model.CourseCategory
import org.springframework.stereotype.Service

/** 코스 카테고리 목록 유스케이스 — 정본이 enum 이라 DB 조회 없이 도메인에서 바로 읽는다(아웃바운드 포트 불필요). */
@Service
class CourseCategoryQueryService : CourseCategoryQueryUseCase {
    override fun listCategoryNames(): List<String> = CATEGORY_NAMES

    private companion object {
        val CATEGORY_NAMES: List<String> = CourseCategory.entries.map { it.name }
    }
}
