package com.example.backend.mobile.user.adapter.inbound.web.response

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SavedCourseScreenResponseMockTest {
    @Test
    fun `mock() 은 non-null 응답을 반환한다`() {
        val response = SavedCourseScreenResponse.mock()

        assertThat(response).isNotNull
    }

    @Test
    fun `mock() totalCount 는 savedCourses 개수와 일치한다`() {
        val response = SavedCourseScreenResponse.mock()

        assertThat(response.totalCount).isEqualTo(response.savedCourses.size)
    }

    @Test
    fun `mock() completedCount + uncompletedCount 는 totalCount 와 일치한다`() {
        val response = SavedCourseScreenResponse.mock()

        assertThat(response.completedCount + response.uncompletedCount).isEqualTo(response.totalCount)
    }

    @Test
    fun `mock() hasNext 는 false 이고 nextCursor 는 null 이다`() {
        val response = SavedCourseScreenResponse.mock()

        assertThat(response.hasNext).isFalse
        assertThat(response.nextCursor).isNull()
    }

    @Test
    fun `mock() 각 저장 코스는 id·courseId·course 가 채워져 있다`() {
        val response = SavedCourseScreenResponse.mock()

        assertThat(response.savedCourses).isNotEmpty
        response.savedCourses.forEach { item ->
            assertThat(item.id).isPositive
            assertThat(item.courseId).isPositive
            assertThat(item.course.title).isNotBlank
            assertThat(item.course.places).isNotEmpty
        }
    }

    @Test
    fun `mock() folders 목록은 비어 있지 않다`() {
        val response = SavedCourseScreenResponse.mock()

        assertThat(response.folders).isNotEmpty
    }
}

class SavedPlaceScreenResponseMockTest {
    @Test
    fun `mock() 은 non-null 응답을 반환한다`() {
        val response = SavedPlaceScreenResponse.mock()

        assertThat(response).isNotNull
    }

    @Test
    fun `mock() totalCount 는 savedPlaces 개수와 일치한다`() {
        val response = SavedPlaceScreenResponse.mock()

        assertThat(response.totalCount).isEqualTo(response.savedPlaces.size.toLong())
    }

    @Test
    fun `mock() visitedCount + unvisitedCount 는 totalCount 와 일치한다`() {
        val response = SavedPlaceScreenResponse.mock()

        assertThat(response.visitedCount + response.unvisitedCount).isEqualTo(response.totalCount)
    }

    @Test
    fun `mock() hasNext 는 false 이고 nextCursor 는 null 이다`() {
        val response = SavedPlaceScreenResponse.mock()

        assertThat(response.hasNext).isFalse
        assertThat(response.nextCursor).isNull()
    }

    @Test
    fun `mock() 각 저장 장소는 id·placeId·place 정보가 채워져 있다`() {
        val response = SavedPlaceScreenResponse.mock()

        assertThat(response.savedPlaces).isNotEmpty
        response.savedPlaces.forEach { item ->
            assertThat(item.id).isPositive
            assertThat(item.placeId).isPositive
            assertThat(item.place.name).isNotBlank
        }
    }

    @Test
    fun `mock() categoryCounts 는 비어 있지 않다`() {
        val response = SavedPlaceScreenResponse.mock()

        assertThat(response.categoryCounts).isNotEmpty
    }
}
