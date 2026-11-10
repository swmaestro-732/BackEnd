package com.example.backend.course.adapter.outbound.admin

import com.example.backend.admin.application.port.inbound.AdminActionUseCase
import com.example.backend.course.application.port.outbound.AdminActionPort
import org.springframework.stereotype.Component

/** ACL 어댑터: 코스 조치 기록을 admin 도메인 인바운드 포트([AdminActionUseCase])로 넘긴다. */
@Component
class AdminActionAdapter(
    private val adminActionUseCase: AdminActionUseCase,
) : AdminActionPort {
    override fun record(
        adminEmail: String,
        courseId: Long,
        action: String,
    ) = adminActionUseCase.record(adminEmail, TARGET_TYPE, courseId, action)

    private companion object {
        const val TARGET_TYPE = "COURSE"
    }
}
