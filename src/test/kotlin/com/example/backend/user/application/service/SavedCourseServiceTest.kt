package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.common.response.UserErrorCode
import com.example.backend.user.application.port.inbound.dto.SavedCoursesCommand
import com.example.backend.user.application.port.outbound.CourseAccessPort
import com.example.backend.user.application.port.outbound.CourseFolderCountRow
import com.example.backend.user.application.port.outbound.CourseFolderRow
import com.example.backend.user.application.port.outbound.SavedCoursePersistencePort
import com.example.backend.user.application.port.outbound.SavedCourseRow
import com.example.backend.user.application.port.outbound.UserPersistencePort
import com.example.backend.user.application.port.outbound.UserProfileRow
import com.example.backend.user.domain.model.CourseFolder
import com.example.backend.user.domain.model.SavedCourse
import com.example.backend.user.domain.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class SavedCourseServiceTest {
    private val fakeCourseAccess =
        object : CourseAccessPort {
            var existing: Set<Long> = emptySet()

            // 실제 갱신 행 수를 반환한다. 0 = 코스가 (동시 삭제 등으로) 비활성 → save 가 롤백해야 한다.
            var increaseReturn = 1
            val increasedCourseIds = mutableListOf<Long>()
            val decreasedCourseIds = mutableListOf<Long>()

            override fun existsCourse(courseId: Long): Boolean = courseId in existing

            override fun increaseSavesCount(courseId: Long): Int {
                increasedCourseIds += courseId
                return increaseReturn
            }

            override fun decreaseSavesCount(courseId: Long) {
                decreasedCourseIds += courseId
            }
        }

    private val fakePort =
        object : SavedCoursePersistencePort {
            var ownedFolders: Set<Pair<Long, Long>> = emptySet()
            var savedCourses: Set<Pair<Long, Long>> = emptySet()
            var pageRows: List<SavedCourseRow> = emptyList()
            var countReturn: Long = 0
            var folderNames: Set<Pair<Long, String>> = emptySet()
            var folderRows: List<CourseFolderRow> = emptyList()
            var folderCountRows: List<CourseFolderCountRow> = emptyList()
            var withoutFolderCount: Long = 0

            // 호출 캡처
            var insertArgs: Triple<Long, Long, Long?>? = null
            var insertFolderArgs: Pair<Long, String>? = null
            var deleteArgs: Pair<Long, Long>? = null
            var findPageArgs: FindPageArgs? = null
            var findFoldersArg: Long? = null

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

            override fun findAliveSavedCourseIds(userId: Long): List<Long> = emptyList()

            override fun deleteAllByUser(userId: Long) {}

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

            override fun existsFolderName(
                userId: Long,
                name: String,
            ): Boolean = (userId to name) in folderNames

            override fun insertFolder(
                userId: Long,
                name: String,
            ): CourseFolder {
                insertFolderArgs = userId to name
                return CourseFolder(id = 300L, userId = userId, name = name, orderNo = 0)
            }

            override fun findFolders(userId: Long): List<CourseFolderRow> {
                findFoldersArg = userId
                return folderRows
            }

            override fun listFolders(userId: Long): List<CourseFolderCountRow> = folderCountRows

            override fun countWithoutFolder(userId: Long): Long = withoutFolderCount
        }

    private val fakeUserPort =
        object : UserPersistencePort {
            // 기본은 전부 활성으로 취급(락 통과). 특정 테스트에서 탈퇴 유저를 흉내내려면 이 집합을 좁힌다.
            var activeUserIds: Set<Long>? = null

            override fun lockActive(userIds: List<Long>): Set<Long> =
                activeUserIds?.let { active -> userIds.filterTo(mutableSetOf()) { it in active } }
                    ?: userIds.toSet()

            override fun findAll(): List<User> = TODO()

            override fun findById(id: Long): User? = TODO()

            override fun findByHandle(handle: String): User? = TODO()

            override fun findProfile(userId: Long): UserProfileRow? = TODO()

            override fun findProfiles(userIds: List<Long>): List<UserProfileRow> = TODO()

            override fun save(user: User): User = TODO()

            override fun update(user: User) = TODO()

            override fun applyCourseCountDelta(
                userId: Long,
                publicDelta: Int,
                followerDelta: Int,
                privateDelta: Int,
            ) = TODO()

            override fun softDelete(user: User) = TODO()

            override fun existsByNickname(nickname: String): Boolean = TODO()

            override fun existsByHandle(handle: String): Boolean = TODO()

            override fun existsByNicknameExcludingUser(
                nickname: String,
                excludeUserId: Long,
            ): Boolean = TODO()

            override fun existsByHandleExcludingUser(
                handle: String,
                excludeUserId: Long,
            ): Boolean = TODO()

            override fun reactivate(user: User): User = TODO()
        }

    private val service = SavedCourseService(fakePort, fakeCourseAccess, fakeUserPort)

    private fun row(id: Long) = SavedCourseRow(id = id, folderId = null, courseId = id * 10, savedAt = Instant.EPOCH)

    // --- save ---

    @Test
    fun `탈퇴(비활성) 사용자면 USER_NOT_FOUND 를 던지고 저장하지 않는다`() {
        fakeUserPort.activeUserIds = emptySet() // 락 대상이 활성 행 없음 = 탈퇴/부재
        fakeCourseAccess.existing = setOf(42L)

        val ex = assertThrows<BusinessException> { service.save(userId = 1L, courseId = 42L, folderId = null) }

        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.errorCode)
        assertNull(fakePort.insertArgs)
    }

    @Test
    fun `저장 중 코스가 비활성화되면(saves_cnt 0행) COURSE_NOT_FOUND 로 롤백한다`() {
        fakeCourseAccess.existing = setOf(42L) // 존재 검증 시점엔 활성
        fakeCourseAccess.increaseReturn = 0 // 삽입 후 증가 시점엔 삭제됨(0행)

        val ex = assertThrows<BusinessException> { service.save(userId = 1L, courseId = 42L, folderId = null) }

        assertEquals(CourseErrorCode.COURSE_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `저장할 코스가 없으면 COURSE_NOT_FOUND 를 던진다`() {
        fakeCourseAccess.existing = emptySet()

        val ex = assertThrows<BusinessException> { service.save(userId = 1L, courseId = 42L, folderId = null) }

        assertEquals(CourseErrorCode.COURSE_NOT_FOUND, ex.errorCode)
        assertNull(fakePort.insertArgs)
    }

    @Test
    fun `folderId 가 소유 폴더가 아니면 INVALID_INPUT 을 던진다`() {
        fakeCourseAccess.existing = setOf(42L)
        fakePort.ownedFolders = emptySet()

        val ex = assertThrows<BusinessException> { service.save(userId = 1L, courseId = 42L, folderId = 7L) }

        assertEquals(CommonErrorCode.INVALID_INPUT, ex.errorCode)
        assertNull(fakePort.insertArgs)
    }

    @Test
    fun `이미 저장한 코스면 COURSE_ALREADY_SAVED 를 던진다`() {
        fakeCourseAccess.existing = setOf(42L)
        fakePort.savedCourses = setOf(1L to 42L)

        val ex = assertThrows<BusinessException> { service.save(userId = 1L, courseId = 42L, folderId = null) }

        assertEquals(UserErrorCode.COURSE_ALREADY_SAVED, ex.errorCode)
        assertNull(fakePort.insertArgs)
    }

    @Test
    fun `유효하면 폴더와 함께 저장하고 생성된 도메인을 반환한다`() {
        fakeCourseAccess.existing = setOf(42L)
        fakePort.ownedFolders = setOf(1L to 7L)

        val result = service.save(userId = 1L, courseId = 42L, folderId = 7L)

        assertEquals(Triple(1L, 42L, 7L), fakePort.insertArgs)
        assertEquals(42L, result.courseId)
        assertEquals(7L, result.folderId)
    }

    @Test
    fun `folderId 가 null 이면 폴더 검증 없이 미분류로 저장한다`() {
        fakeCourseAccess.existing = setOf(42L)
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

        assertEquals(CommonErrorCode.INVALID_INPUT, ex.errorCode)
    }

    // --- createFolder ---

    @Test
    fun `폴더 생성 시 탈퇴(비활성) 사용자면 USER_NOT_FOUND 를 던지고 만들지 않는다`() {
        fakeUserPort.activeUserIds = emptySet() // 락 대상이 활성 행 없음 = 탈퇴/부재

        val ex = assertThrows<BusinessException> { service.createFolder(userId = 1L, name = "가고싶다") }

        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.errorCode)
        assertNull(fakePort.insertFolderArgs)
    }

    @Test
    fun `같은 이름의 폴더가 이미 있으면 FOLDER_NAME_ALREADY_TAKEN 을 던진다`() {
        fakePort.folderNames = setOf(1L to "가고싶다")

        val ex = assertThrows<BusinessException> { service.createFolder(userId = 1L, name = "가고싶다") }

        assertEquals(UserErrorCode.FOLDER_NAME_ALREADY_TAKEN, ex.errorCode)
        assertNull(fakePort.insertFolderArgs)
    }

    @Test
    fun `이름 중복은 사용자별로 판정한다 - 타인이 쓰는 이름이면 그대로 만든다`() {
        fakePort.folderNames = setOf(2L to "가고싶다") // 같은 이름이지만 다른 사용자 소유

        val folder = service.createFolder(userId = 1L, name = "가고싶다")

        assertEquals(1L to "가고싶다", fakePort.insertFolderArgs)
        assertEquals("가고싶다", folder.name)
    }

    @Test
    fun `유효하면 폴더를 만들고 생성된 도메인을 반환한다`() {
        val folder = service.createFolder(userId = 1L, name = "데이트")

        assertEquals(1L to "데이트", fakePort.insertFolderArgs)
        assertEquals(300L, folder.id)
        assertEquals(1L, folder.userId)
        assertEquals("데이트", folder.name)
    }

    // --- getFolders ---

    @Test
    fun `폴더 목록은 포트가 준 순서 그대로 id·이름만 매핑한다`() {
        fakePort.folderRows =
            listOf(
                CourseFolderRow(id = 10L, name = "가고싶다"),
                CourseFolderRow(id = 20L, name = "데이트"),
            )

        val result = service.getFolders(userId = 1L)

        assertEquals(1L, fakePort.findFoldersArg)
        assertEquals(listOf(10L to "가고싶다", 20L to "데이트"), result.map { it.id to it.name })
    }

    @Test
    fun `폴더가 하나도 없으면 빈 목록을 반환한다`() {
        fakePort.folderRows = emptyList()

        assertTrue(service.getFolders(userId = 1L).isEmpty())
    }

    // --- getFolderCounts ---

    @Test
    fun `폴더별 개수 조회는 미분류 개수를 폴더 합이 아니라 따로 센다`() {
        fakePort.folderCountRows =
            listOf(
                CourseFolderCountRow(id = 10L, name = "가고싶다", count = 2),
                CourseFolderCountRow(id = 20L, name = "데이트", count = 0),
            )
        fakePort.withoutFolderCount = 5 // 폴더 합(2)과 무관한 값이라야 따로 세는 게 드러난다

        val result = service.getFolderCounts(userId = 1L)

        assertEquals(listOf(10L to 2, 20L to 0), result.folders.map { it.id to it.count })
        assertEquals(listOf("가고싶다", "데이트"), result.folders.map { it.name })
        assertEquals(5L, result.withoutFolderCount)
    }

    private data class FindPageArgs(
        val userId: Long,
        val folderId: Long?,
        val completed: Boolean?,
        val cursorId: Long?,
        val limit: Int,
    )
}
