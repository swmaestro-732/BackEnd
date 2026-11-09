package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.AdminCourseDetail
import com.example.backend.course.application.port.inbound.dto.AdminCoursePage
import com.example.backend.course.application.port.inbound.dto.AdminCourseQuery
import com.example.backend.course.application.port.inbound.dto.AdminEditCourseCommand
import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.application.port.inbound.dto.CoursePreviewCommand

/**
 * 인바운드 포트: 어드민 코스 관리. 상태와 공개 여부에 관계없이 다루고, 조치마다 admin_actions 에 기록한다.
 * 숨김, 해제, 삭제, 복원은 기존 저장, 삭제 이벤트를 그대로 발행해 검색 색인과 작성자 코스 개수가 함께 움직인다.
 */
interface AdminCourseUseCase {
    fun list(query: AdminCourseQuery): AdminCoursePage

    /** 삭제된 코스도 반환한다. */
    fun getDetail(courseId: Long): AdminCourseDetail

    /** 큐레이터 코스만(아니면 4101), updatedAt 이 다르면 4102. */
    fun edit(command: AdminEditCourseCommand): AdminCourseDetail

    /** 활성 코스만 숨긴다(아니면 4103). */
    fun hide(
        adminEmail: String,
        courseId: Long,
    )

    fun unhide(
        adminEmail: String,
        courseId: Long,
    )

    /** 활성, 숨김 코스를 소프트 삭제한다. */
    fun delete(
        adminEmail: String,
        courseId: Long,
    )

    /** 삭제된 코스를 활성으로 되돌린다(삭제 전 숨김이었어도 활성). */
    fun restore(
        adminEmail: String,
        courseId: Long,
    )

    /** 초안을 저장하지 않고 앱 코스 상세와 같은 결과로 계산한다(부수효과 없음). */
    fun preview(command: CoursePreviewCommand): CourseDetailResult
}
