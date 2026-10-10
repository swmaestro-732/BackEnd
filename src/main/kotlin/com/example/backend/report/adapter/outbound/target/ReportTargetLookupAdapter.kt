package com.example.backend.report.adapter.outbound.target

import com.example.backend.course.application.port.inbound.CourseCommentQueryUseCase
import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.report.application.port.outbound.ReportTargetLookupPort
import com.example.backend.report.domain.model.ReportTargetType
import com.example.backend.user.application.port.inbound.UserUseCase
import org.springframework.stereotype.Component

/**
 * ACL 어댑터 — [ReportTargetLookupPort] 를 각 도메인의 인바운드 포트로 위임한다.
 * 코스는 신고자 기준 공개범위를 통과한 것만 대상으로 본다(볼 수 없는 코스는 COURSE_NOT_FOUND). 댓글도 부모 코스에 같은 검사를 적용한다.
 */
@Component
class ReportTargetLookupAdapter(
    private val userUseCase: UserUseCase,
    private val courseQueryUseCase: CourseQueryUseCase,
    private val courseCommentQueryUseCase: CourseCommentQueryUseCase,
) : ReportTargetLookupPort {
    override fun getOwnerId(
        type: ReportTargetType,
        targetId: Long,
        viewerId: Long,
    ): Long =
        when (type) {
            ReportTargetType.USER -> {
                userUseCase.getProfile(targetId, viewerId).id
            }

            ReportTargetType.COURSE -> {
                courseQueryUseCase.getDetail(targetId, viewerId).authorId
            }

            ReportTargetType.COURSE_COMMENT -> {
                val comment = courseCommentQueryUseCase.getRef(targetId)
                // 부모 코스가 삭제됐거나 신고자가 볼 수 없으면 getDetail 이 COURSE_NOT_FOUND 를 던진다.
                courseQueryUseCase.getDetail(comment.courseId, viewerId)
                comment.authorId
            }
        }
}
