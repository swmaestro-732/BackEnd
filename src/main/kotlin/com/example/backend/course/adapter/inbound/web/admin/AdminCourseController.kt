package com.example.backend.course.adapter.inbound.web.admin

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ApiResponse
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.course.application.port.inbound.AdminCourseUseCase
import com.example.backend.course.application.port.inbound.dto.AdminCourseAuthorType
import com.example.backend.course.application.port.inbound.dto.AdminCourseDetail
import com.example.backend.course.application.port.inbound.dto.AdminCoursePage
import com.example.backend.course.application.port.inbound.dto.AdminCourseQuery
import com.example.backend.course.application.port.inbound.dto.AdminCourseStatus
import com.example.backend.course.domain.model.CourseCategory
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터: 어드민 코스 관리(SCRUM-752).
 *
 * /admin/v1 경로라 purpose=admin 토큰이면서 지금도 admins 에 있는 계정만 통과한다(SecurityConfig).
 * 목록과 상세는 포트 결과를 그대로 응답 계약으로 쓴다(화면 전용 가공이 없어 응답 DTO 를 따로 두지 않는다).
 */
@RestController
@RequestMapping("/admin/v1/courses")
class AdminCourseController(
    private val adminCourseUseCase: AdminCourseUseCase,
) {
    private val log = KotlinLogging.logger {}

    /** 목록. status 가 없으면 삭제되지 않은 전체, page 는 0부터, size 는 1~100. q 는 제목, 작성자 닉네임, 코스 id. */
    @GetMapping
    fun list(
        @RequestParam(required = false) status: AdminCourseStatus?,
        @RequestParam(required = false) author: AdminCourseAuthorType?,
        @RequestParam(required = false) areaCode: String?,
        @RequestParam(required = false) theme: CourseCategory?,
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<AdminCoursePage> =
        ApiResponse.success(
            adminCourseUseCase.list(
                AdminCourseQuery(
                    status = status,
                    author = author,
                    areaCode = areaCode,
                    theme = theme,
                    q = q,
                    page = page.coerceAtLeast(0),
                    size = size.coerceIn(1, MAX_PAGE_SIZE),
                ),
            ),
        )

    @GetMapping("/{courseId}")
    fun getDetail(
        @PathVariable courseId: Long,
    ): ApiResponse<AdminCourseDetail> = ApiResponse.success(adminCourseUseCase.getDetail(courseId))

    @PatchMapping("/{courseId}")
    fun edit(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable courseId: Long,
        @Valid @RequestBody request: AdminEditCourseRequest,
    ): ApiResponse<AdminCourseDetail> =
        ApiResponse.success(adminCourseUseCase.edit(request.toCommand(adminEmail(jwt), courseId)))

    @PostMapping("/{courseId}/hide")
    fun hide(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable courseId: Long,
    ): ApiResponse<Nothing?> {
        adminCourseUseCase.hide(adminEmail(jwt), courseId)
        return ApiResponse.ok("코스를 숨겼습니다.")
    }

    @PostMapping("/{courseId}/unhide")
    fun unhide(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable courseId: Long,
    ): ApiResponse<Nothing?> {
        adminCourseUseCase.unhide(adminEmail(jwt), courseId)
        return ApiResponse.ok("코스 숨김을 해제했습니다.")
    }

    @DeleteMapping("/{courseId}")
    fun delete(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable courseId: Long,
    ): ApiResponse<Nothing?> {
        adminCourseUseCase.delete(adminEmail(jwt), courseId)
        return ApiResponse.ok("코스를 삭제했습니다.")
    }

    /** 삭제된 코스를 숨김 상태로 복원한다. 공개하려면 unhide 를 따로 부른다. */
    @PostMapping("/{courseId}/restore")
    fun restore(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable courseId: Long,
    ): ApiResponse<Nothing?> {
        adminCourseUseCase.restore(adminEmail(jwt), courseId)
        return ApiResponse.ok("코스를 숨김 상태로 복원했습니다. 공개하려면 숨김을 해제하세요.")
    }

    /** 일괄 조치: 건마다 별도 트랜잭션이라 일부만 성공할 수 있고, 건별 결과를 요청 순서대로 돌려준다. */
    @PostMapping("/bulk")
    fun bulk(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: AdminCourseBulkRequest,
    ): ApiResponse<List<AdminCourseBulkItemResponse>> {
        val adminEmail = adminEmail(jwt)
        return ApiResponse.success(
            request.ids.distinct().map { id ->
                try {
                    when (request.action) {
                        AdminCourseBulkAction.HIDE -> adminCourseUseCase.hide(adminEmail, id)
                        AdminCourseBulkAction.UNHIDE -> adminCourseUseCase.unhide(adminEmail, id)
                        AdminCourseBulkAction.DELETE -> adminCourseUseCase.delete(adminEmail, id)
                    }
                    AdminCourseBulkItemResponse(id, success = true)
                } catch (e: BusinessException) {
                    AdminCourseBulkItemResponse(id, success = false, code = e.errorCode.code, message = e.message)
                } catch (e: Exception) {
                    // 한 건의 예기치 못한 실패가 나머지 건을 막지 않게 건별 실패로 담는다.
                    log.error(e) { "어드민 코스 일괄 조치 실패: id=$id, action=${request.action}" }
                    AdminCourseBulkItemResponse(
                        id,
                        success = false,
                        code = CommonErrorCode.INTERNAL_ERROR.code,
                        message = CommonErrorCode.INTERNAL_ERROR.message,
                    )
                }
            },
        )
    }

    private fun adminEmail(jwt: Jwt) = checkNotNull(jwt.subject)

    private companion object {
        const val MAX_PAGE_SIZE = 100
    }
}
