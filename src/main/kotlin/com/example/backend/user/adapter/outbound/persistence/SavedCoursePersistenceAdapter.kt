package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.SavedCourseFolderRepository
import com.example.backend.user.adapter.outbound.persistence.exposed.repository.SavedCourseRepository
import com.example.backend.user.application.port.outbound.CourseFolderCountRow
import com.example.backend.user.application.port.outbound.SavedCoursePersistencePort
import com.example.backend.user.application.port.outbound.SavedCourseRow
import com.example.backend.user.domain.model.SavedCourse
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [SavedCoursePersistencePort] 를 구현한다.
 * saved_courses(저장 레코드)는 [SavedCourseRepository], saved_course_folders(폴더 소유권·개수)는
 * [SavedCourseFolderRepository] 에 위임한다.
 */
@Component
class SavedCoursePersistenceAdapter(
    private val savedCourseRepository: SavedCourseRepository,
    private val savedCourseFolderRepository: SavedCourseFolderRepository,
) : SavedCoursePersistencePort {
    override fun existsFolder(
        userId: Long,
        folderId: Long,
    ): Boolean = savedCourseFolderRepository.exists(userId, folderId)

    override fun existsSavedCourse(
        userId: Long,
        courseId: Long,
    ): Boolean = savedCourseRepository.existsSavedCourse(userId, courseId)

    override fun insert(
        userId: Long,
        courseId: Long,
        folderId: Long?,
    ): SavedCourse = savedCourseRepository.insert(userId, courseId, folderId)

    override fun deleteByUserAndCourse(
        userId: Long,
        courseId: Long,
    ): Boolean = savedCourseRepository.deleteByUserAndCourse(userId, courseId)

    override fun count(
        userId: Long,
        folderId: Long?,
        completed: Boolean?,
    ): Long = savedCourseRepository.count(userId, folderId, completed)

    override fun findPage(
        userId: Long,
        folderId: Long?,
        completed: Boolean?,
        cursorId: Long?,
        limit: Int,
    ): List<SavedCourseRow> = savedCourseRepository.findPage(userId, folderId, completed, cursorId, limit)

    override fun listFolders(userId: Long): List<CourseFolderCountRow> = savedCourseFolderRepository.listFolders(userId)
}
