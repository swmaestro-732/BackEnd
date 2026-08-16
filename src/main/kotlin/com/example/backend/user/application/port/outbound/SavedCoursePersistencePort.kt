package com.example.backend.user.application.port.outbound

import com.example.backend.user.domain.model.CourseFolder
import com.example.backend.user.domain.model.SavedCourse
import java.time.Instant

/**
 * 아웃바운드 포트 — 저장 코스(saved_courses)·저장 폴더(saved_course_folders) 접근.
 * 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 */
interface SavedCoursePersistencePort {
    /** 폴더가 존재하고 해당 사용자 소유인지. 저장 시 folderId 검증에 쓴다. */
    fun existsFolder(
        userId: Long,
        folderId: Long,
    ): Boolean

    /** (user_id, course_id) 저장 레코드가 이미 있는지. 중복 저장 차단(409)에 쓴다. */
    fun existsSavedCourse(
        userId: Long,
        courseId: Long,
    ): Boolean

    /** 저장 레코드를 삽입하고 생성값(id·created_at)까지 적재된 도메인 [SavedCourse] 를 반환한다. */
    fun insert(
        userId: Long,
        courseId: Long,
        folderId: Long?,
    ): SavedCourse

    /** (user_id, course_id) 저장 레코드를 삭제한다. 실제 삭제된 행이 있으면 true(없으면 false). */
    fun deleteByUserAndCourse(
        userId: Long,
        courseId: Long,
    ): Boolean

    /** 사용자가 지금 살아있게 저장 중인(deleted_at IS NULL) 코스 id 목록 — 탈퇴 정리 시 원저자 saves_cnt 보정용. */
    fun findAliveSavedCourseIds(userId: Long): List<Long>

    /** 사용자의 저장 코스·저장 폴더를 전부 하드 삭제한다(회원 탈퇴 정리). saves_cnt 보정은 호출부 책임. */
    fun deleteAllByUser(userId: Long)

    /**
     * 필터에 해당하는 저장 코스 개수.
     * - folderId: null 이면 전체(폴더 미분류 포함).
     * - completed: null 이면 완주 여부 무관, true 면 따라간(=완주) 코스만, false 면 아직 안 따라간 코스만.
     *   완주 판정은 tracing_courses(user_id, course_id) 존재 여부다([com.example.backend.user.adapter.outbound.persistence.TracingCourseTable]).
     */
    fun count(
        userId: Long,
        folderId: Long?,
        completed: Boolean?,
    ): Long

    /**
     * 저장 레코드를 id 내림차순(최신 저장순)으로 조회한다.
     * cursorId 가 있으면 그보다 작은 id 만(다음 페이지), limit 개까지 반환한다.
     * completed 필터는 [count] 와 동일한 의미다(tracing_courses 존재 여부 기준).
     */
    fun findPage(
        userId: Long,
        folderId: Long?,
        completed: Boolean?,
        cursorId: Long?,
        limit: Int,
    ): List<SavedCourseRow>

    /** 같은 사용자에게 같은 이름의 폴더가 이미 있는지. 폴더 생성 시 중복 이름 차단(409)에 쓴다. */
    fun existsFolderName(
        userId: Long,
        name: String,
    ): Boolean

    /** 폴더를 삽입하고 생성 id 까지 적재된 도메인 [CourseFolder] 를 반환한다(order_no 는 어댑터가 맨 뒤로 채운다). */
    fun insertFolder(
        userId: Long,
        name: String,
    ): CourseFolder

    /** 사용자의 저장 폴더를 order_no 순으로 조회한다 — 개수 없이 id·이름만(폴더 목록 조회·저장 시트). */
    fun findFolders(userId: Long): List<CourseFolderRow>

    /** 사용자의 저장 폴더를 order_no 순으로, 폴더별 저장 코스 개수와 함께 조회한다(저장함 폴더 칩). */
    fun listFolders(userId: Long): List<CourseFolderCountRow>

    /** 폴더에 넣지 않고(folder_id IS NULL) 저장한 코스 개수 — 저장함 "폴더 없음" 칩. */
    fun countWithoutFolder(userId: Long): Long
}

/** 저장 레코드 읽기 모델 — 조회 전용. */
data class SavedCourseRow(
    val id: Long,
    val folderId: Long?,
    val courseId: Long,
    val savedAt: Instant,
)

/** 저장 폴더 읽기 모델 — 개수 없이 폴더 자체만(폴더 목록 조회). */
data class CourseFolderRow(
    val id: Long,
    val name: String,
)

/** 저장 폴더 + 폴더별 저장 코스 개수 읽기 모델(저장함 폴더 칩). */
data class CourseFolderCountRow(
    val id: Long,
    val name: String,
    val count: Int,
)
