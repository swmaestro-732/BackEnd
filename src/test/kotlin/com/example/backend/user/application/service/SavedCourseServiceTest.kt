package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.course.application.port.inbound.CourseCounterUseCase
import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.inbound.dto.AuthorCourseCursor
import com.example.backend.course.application.port.inbound.dto.CourseSummary
import com.example.backend.course.application.port.inbound.dto.CourseSummaryPage
import com.example.backend.user.application.port.inbound.dto.SavedCoursesCommand
import com.example.backend.user.application.port.outbound.CourseFolderCountRow
import com.example.backend.user.application.port.outbound.SavedCoursePersistencePort
import com.example.backend.user.application.port.outbound.SavedCourseRow
import com.example.backend.user.domain.model.SavedCourse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class SavedCourseServiceTest {
    private val fakeCourseQuery =
        object : CourseQueryUseCase {
            var existing: Set<Long> = emptySet()

            override fun existsById(courseId: Long): Boolean = courseId in existing

            override fun listByAuthor(
                authorId: Long,
                viewerId: Long?,
                cursor: AuthorCourseCursor?,
                size: Int,
            ): CourseSummaryPage = CourseSummaryPage(emptyList(), hasNext = false)

            override fun listPublic(limit: Int): List<CourseSummary> = emptyList()
        }

    private val fakePort =
        object : SavedCoursePersistencePort {
            var ownedFolders: Set<Pair<Long, Long>> = emptySet()
            var savedCourses: Set<Pair<Long, Long>> = emptySet()
            var pageRows: List<SavedCourseRow> = emptyList()
            var countReturn: Long = 0

            // 호출 캡처
            var insertArgs: Triple<Long, Long, Long?>? = null
            var deleteArgs: Pair<Long, Long>? = null
            var findPageArgs: FindPageArgs? = null

            override fun existsFolder(
                userId: Long,
                folderId: Long,
            ): Boolean = (userId to folderId) in ownedFolders

            override fun existsSavedCourse(
                userId: Long,
                courseId: Long,
            ): Boolean = (userId to courseId) in savedCourses

            override fun insert(
                userId: Long,
                courseId: Long,
                folderId: Long?,
            ): SavedCourse {
                insertArgs = Triple(userId, courseId, folderId)
                return SavedCourse(
                    id = 100L,
                    userId = userId,
                    courseId = courseId,
                    folderId = folderId,
                    savedAt = Instant.parse("2026-07-31T00:00:00Z"),
                )
            }

            override fun deleteByUserAndCourse(
                userId: Long,
                courseId: Long,
            ): Boolean {
                deleteArgs = userId to courseId
                return true
            }

            override fun count(
                userId: Long,
                folderId: Long?,
                completed: Boolean?,
            ): Long = countReturn

            override fun findPage(
                userId: Long,
                folderId: Long?,
                completed: Boolean?,
                cursorId: Long?,
                limit: Int,
            ): List<SavedCourseRow> {
                findPageArgs = FindPageArgs(userId, folderId, completed, cursorId, limit)
                return pageRows
            }

            override fun listFolders(userId: Long): List<CourseFolderCountRow> = emptyList()
        }

    private val fakeCourseCounter =
        object : CourseCounterUseCase {
            var increasedCourseIds: MutableList<Long> = mutableListOf()
            var decreasedCourseIds: MutableList<Long> = mutableListOf()

            override fun increaseSavesCount(courseId: Long) {
                increasedCourseIds += courseId
            }

            override fun decreaseSavesCount(courseId: Long) {
                decreasedCourseIds += courseId
            }
        }

    private val service = SavedCourseService(fakePort, fakeCourseQuery, fakeCourseCounter)

    private fun row(id: Long) = SavedCourseRow(id = id, folderId = null, courseId = id * 10, savedAt = Instant.EPOCH)

    // --- save ---

    @Test
    fun `저장할 코스가 없으면 COURSE_NOT_FOUND 를 던진다`() {
        fakeCourseQuery.existing = emptySet()

        val ex = assertThrows<BusinessException> { service.save(userId = 1L, courseId = 42L, folderId = null) }

        assertEquals(ErrorCode.COURSE_NOT_FOUND, ex.errorCode)
        assertNull(fakePort.insertArgs)
    }

    @Test
    fun `folderId 가 소유 폴더가 아니면 INVALID_INPUT 을 던진다`() {
        fakeCourseQuery.existing = setOf(42L)
        fakePort.ownedFolders = emptySet()

        val ex = assertThrows<BusinessException> { service.save(userId = 1L, courseId = 42L, folderId = 7L) }

        assertEquals(ErrorCode.INVALID_INPUT, ex.errorCode)
        assertNull(fakePort.insertArgs)
    }

    @Test
    fun `이미 저장한 코스면 COURSE_ALREADY_SAVED 를 던진다`() {
        fakeCourseQuery.existing = setOf(42L)
        fakePort.savedCourses = setOf(1L to 42L)

        val ex = assertThrows<BusinessException> { service.save(userId = 1L, courseId = 42L, folderId = null) }

        assertEquals(ErrorCode.COURSE_ALREADY_SAVED, ex.errorCode)
        assertNull(fakePort.insertArgs)
    }

    @Test
    fun `유효하면 폴더와 함께 저장하고 생성된 도메인을 반환한다`() {
        fakeCourseQuery.existing = setOf(42L)
        fakePort.ownedFolders = setOf(1L to 7L)

        val result = service.save(userId = 1L, courseId = 42L, folderId = 7L)

        assertEquals(Triple(1L, 42L, 7L), fakePort.insertArgs)
        assertEquals(42L, result.courseId)
        assertEquals(7L, result.folderId)
    }

    @Test
    fun `folderId 가 null 이면 폴더 검증 없이 미분류로 저장한다`() {
        fakeCourseQuery.existing = setOf(42L)
        // ownedFolders 비어 있어도 folderId=null 이면 existsFolder 를 타지 않아 통과해야 한다

        val result = service.save(userId = 1L, courseId = 42L, folderId = null)

        assertEquals(Triple(1L, 42L, null), fakePort.insertArgs)
        assertNull(result.folderId)
    }

    // --- unsave ---

    @Test
    fun `저장 취소는 삭제 포트에 위임한다 (멱등)`() {
        service.unsave(userId = 1L, courseId = 42L)

        assertEquals(1L to 42L, fakePort.deleteArgs)
    }

    // --- getSavedCourses ---

    @Test
    fun `size 보다 한 개 더 조회되면 hasNext true 이고 nextCursor 는 페이지 마지막 id 다`() {
        // size=2 인데 3개 반환 → hasNext, page 는 앞 2개, nextCursor 는 2번째 id
        fakePort.pageRows = listOf(row(30L), row(20L), row(10L))
        fakePort.countReturn = 5

        val result = service.getSavedCourses(SavedCoursesCommand(userId = 1L, folderId = null, cursor = null, size = 2))

        assertEquals(5L, result.totalCount)
        assertTrue(result.hasNext)
        assertEquals("20", result.nextCursor)
        assertEquals(listOf(30L, 20L), result.savedCourses.map { it.id })
        // hasNext 판정을 위해 size+1 로 조회하는지 확인
        assertEquals(3, fakePort.findPageArgs?.limit)
    }

    @Test
    fun `결과가 size 이하면 hasNext false 이고 nextCursor 는 null 이다`() {
        fakePort.pageRows = listOf(row(30L), row(20L))
        fakePort.countReturn = 2

        val result = service.getSavedCourses(SavedCoursesCommand(userId = 1L, folderId = null, cursor = null, size = 5))

        assertFalse(result.hasNext)
        assertNull(result.nextCursor)
        assertEquals(listOf(30L, 20L), result.savedCourses.map { it.id })
    }

    @Test
    fun `유효한 커서는 cursorId 로 파싱돼 findPage 에 전달된다`() {
        fakePort.pageRows = emptyList()

        service.getSavedCourses(SavedCoursesCommand(userId = 1L, folderId = 7L, cursor = "50", size = 10))

        assertEquals(50L, fakePort.findPageArgs?.cursorId)
        assertEquals(7L, fakePort.findPageArgs?.folderId)
    }

    @Test
    fun `잘못된 커서면 INVALID_INPUT 을 던진다`() {
        val ex =
            assertThrows<BusinessException> {
                service.getSavedCourses(SavedCoursesCommand(userId = 1L, folderId = null, cursor = "abc", size = 10))
            }

        assertEquals(ErrorCode.INVALID_INPUT, ex.errorCode)
    }

    private data class FindPageArgs(
        val userId: Long,
        val folderId: Long?,
        val completed: Boolean?,
        val cursorId: Long?,
        val limit: Int,
    )
}
