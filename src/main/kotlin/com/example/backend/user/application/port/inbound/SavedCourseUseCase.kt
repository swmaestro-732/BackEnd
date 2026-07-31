package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.port.inbound.dto.SavedCourseFolderCount
import com.example.backend.user.application.port.inbound.dto.SavedCoursesCommand
import com.example.backend.user.application.port.inbound.dto.SavedCoursesResult
import com.example.backend.user.domain.model.SavedCourse

/**
 * 인바운드 포트 — 코스 저장(saved_courses)·저장 코스 조회(공개 API).
 *
 * 저장 레코드는 user 도메인 소유 데이터다. 저장 시 폴더 소유권을 검증하고, 이미 저장한 코스면 중복 저장으로 막는다(409).
 * 코스당 저장 레코드는 1개이며 폴더 이동은 별도 API 소관이다. 조회는 저장 레코드(ID 위주)만 반환한다.
 * 저장 취소(unsave)는 저장과 대칭인 멱등 연산이다 — 저장돼 있지 않아도 성공으로 수렴한다(unfollow 선례와 동일).
 */
interface SavedCourseUseCase {
    fun save(
        userId: Long,
        courseId: Long,
        folderId: Long?,
    ): SavedCourse

    /** (userId, courseId) 저장을 취소한다. 저장 레코드가 없어도 오류 없이 성공한다(멱등). */
    fun unsave(
        userId: Long,
        courseId: Long,
    )

    fun getSavedCourses(command: SavedCoursesCommand): SavedCoursesResult

    /** 사용자의 저장 폴더를 폴더별 저장 코스 개수와 함께 order_no 순으로 반환한다(저장함 코스 탭 폴더 칩). */
    fun getFolders(userId: Long): List<SavedCourseFolderCount>
}
