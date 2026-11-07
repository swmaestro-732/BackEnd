package com.example.backend.course.domain.model

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.course.domain.model.CourseCategory.ACTIVITY
import com.example.backend.course.domain.model.CourseCategory.CAFETOUR
import com.example.backend.course.domain.model.CourseCategory.CULTURE
import com.example.backend.course.domain.model.CourseCategory.DATE
import com.example.backend.course.domain.model.CourseCategory.FAMILY
import com.example.backend.course.domain.model.CourseCategory.FOOD
import com.example.backend.course.domain.model.CourseCategory.HEALING
import com.example.backend.course.domain.model.CourseCategory.NATURE
import com.example.backend.course.domain.model.CourseCategory.NIGHTVIEW
import com.example.backend.course.domain.model.CourseCategory.SHOPPING
import com.example.backend.course.domain.model.CourseCategory.SOLO
import com.example.backend.course.domain.model.CourseCategory.TRADITION
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** 코스 카테고리 지정 규칙 — 동행 선택값 + 장소 기반 + 식당/카페 보충. */
class CourseCategoryTest {
    @Test
    fun `장소 카테고리에 대응하는 코스 카테고리를 모두 붙인다`() {
        val result =
            CourseCategory.resolve(
                emptyList(),
                listOf("CULTURE", "NATURE", "LANDMARK", "BAR", "SHOPPING", "EXPERIENCE", "ENTERTAINMENT", "WELLNESS"),
            )

        assertEquals(setOf(CULTURE, NATURE, HEALING, TRADITION, NIGHTVIEW, SHOPPING, ACTIVITY), result)
    }

    @Test
    fun `카테고리가 2개 이상이면 식당·카페를 보충하지 않는다`() {
        assertEquals(setOf(DATE, CULTURE), CourseCategory.resolve(listOf(DATE), listOf("CULTURE", "CAFE", "CAFE")))
    }

    @Test
    fun `카테고리가 1개 이하면 음식점·카페 중 많은 쪽을 보충한다`() {
        assertEquals(
            setOf(CULTURE, CAFETOUR),
            CourseCategory.resolve(emptyList(), listOf("CULTURE", "CAFE", "CAFE", "RESTAURANT")),
        )
        assertEquals(
            setOf(SOLO, FOOD),
            CourseCategory.resolve(listOf(SOLO), listOf("RESTAURANT", "RESTAURANT", "CAFE")),
        )
    }

    @Test
    fun `음식점·카페 수가 같으면 둘 다 보충하고 둘 다 없으면 보충하지 않는다`() {
        assertEquals(setOf(FOOD, CAFETOUR), CourseCategory.resolve(emptyList(), listOf("RESTAURANT", "CAFE")))
        assertEquals(emptySet<CourseCategory>(), CourseCategory.resolve(emptyList(), emptyList()))
    }

    @Test
    fun `발행 코스는 동행 선택값과 장소 기반 카테고리를 enum 순으로 담는다`() {
        val course = create(isPublished = true, companions = listOf(FAMILY), placeCategories = listOf("NATURE", "CAFE"))

        assertEquals(listOf(HEALING, NATURE, FAMILY), course.categories)
    }

    @Test
    fun `임시저장은 동행 선택값만 담는다`() {
        val course = create(isPublished = false, companions = listOf(DATE), placeCategories = listOf("NATURE", "CAFE"))

        assertEquals(listOf(DATE), course.categories)
    }

    @Test
    fun `동행 선택값이 아닌 카테고리를 고르면 거부한다`() {
        val exception = assertThrows<BusinessException> { create(isPublished = true, companions = listOf(FOOD)) }

        assertEquals(CommonErrorCode.INVALID_INPUT, exception.errorCode)
    }

    @Test
    fun `편집에서 동행 선택값이 없으면 기존 값을 유지하고 빈 목록이면 비운다`() {
        val existing = listOf(DATE, CULTURE)

        assertEquals(listOf(DATE, CAFETOUR), edit(companions = null, existingCategories = existing).categories)
        assertEquals(listOf(CAFETOUR), edit(companions = emptyList(), existingCategories = existing).categories)
    }

    private fun create(
        isPublished: Boolean,
        companions: List<CourseCategory>?,
        placeCategories: List<String> = listOf("CAFE", "CAFE"),
    ): Course =
        Course.create(
            userId = 1L,
            title = "코스",
            description = null,
            coverImageUrl = "cover",
            visibility = CourseVisibility.PUBLIC,
            isPublished = isPublished,
            duplicatedFromId = null,
            tags = emptyList(),
            places = places(placeCategories.size),
            companions = companions,
            placeCategoryByPlaceId = placeCategories.withIndex().associate { (i, name) -> i.toLong() + 1 to name },
            areaCode = null,
            area = null,
        )

    private fun edit(
        companions: List<CourseCategory>?,
        existingCategories: List<CourseCategory>,
    ): Course =
        Course.edit(
            id = 1L,
            userId = 1L,
            title = "코스",
            description = null,
            coverImageUrl = "cover",
            visibility = CourseVisibility.PUBLIC,
            isPublished = true,
            duplicatedFromId = null,
            tags = emptyList(),
            places = places(2),
            companions = companions,
            existingCategories = existingCategories,
            placeCategoryByPlaceId = mapOf(1L to "CAFE", 2L to "CAFE"),
            areaCode = null,
            area = null,
        )

    private fun places(count: Int): List<CoursePlace> =
        (1..count).map { CoursePlace(it.toLong(), it - 1, null, listOf("img"), null) }
}
