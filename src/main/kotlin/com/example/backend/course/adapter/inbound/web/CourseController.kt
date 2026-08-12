package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.request.CreateCourseRequest
import com.example.backend.course.adapter.inbound.web.request.EditCourseRequest
import com.example.backend.course.adapter.inbound.web.request.ForkCourseRequest
import com.example.backend.course.adapter.inbound.web.response.CourseDetailResponse
import com.example.backend.course.adapter.inbound.web.response.CourseIdResponse
import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.inbound.CourseUseCase
import com.example.backend.course.application.port.inbound.dto.CourseSummary
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * 인바운드 어댑터 — 코스(노션 명세 · Course).
 *
 * - [getDetail] 코스 상세(`GET /api/v1/courses/{courseId}`): **실구현** — 인바운드 포트([CourseUseCase])로
 *   DB 조회한다. 시드 데이터가 없는 개발 환경을 위해 `?mock=true` 폴백([CourseDetailResponse.MOCK])을 유지한다.
 * - [listDrafts] 임시저장 코스 목록(`GET /api/v1/courses/drafts`): **실구현** — 로그인 작성자의
 *   미발행·활성·미삭제 코스를 최근 수정순으로 조회한다.
 * - [create] 코스 생성(`POST /api/v1/courses`): **실구현** — 인바운드 포트([CourseUseCase])로 저장한다.
 *   작성자 식별이 필요해 `@CurrentUserId`(JWT subject)로 userId 를 받는다 — 유효한 토큰이 있어야 동작하며,
 *   경로 자체의 인증 강제(SecurityConfig)는 후속 과제다. 시드/DB 없이 프론트가 붙어볼 수 있도록
 *   `?mock=true` 면 저장 없이 고정 목([CourseIdResponse.MOCK], 코스 상세 목과 이어짐)을 반환한다.
 * - [edit] 코스 편집(`PATCH /api/v1/courses/{courseId}`): **실구현** — 인바운드 포트([CourseUseCase])로
 *   전체 치환 갱신한다. 작성자 식별이 필요해 `@CurrentUserId`(JWT subject)로 userId 를 받으며, 소유자만 편집 가능하다.
 *   시드/DB 없이 프론트가 붙어볼 수 있도록 `?mock=true` 면 갱신 없이 고정 목([CourseIdResponse.MOCK])을 반환한다.
 * - [fork] 코스 포크(`POST /api/v1/courses/{courseId}/forks`): **실구현** — 인바운드 포트([CourseUseCase])로
 *   원본의 장소 구성 위에 포크하는 사람의 콘텐츠를 얹어 새 코스로 저장한다(courses.forked_from_id 로 출처 표시).
 *   원본을 볼 수 없으면 404 이며, 포크 주체 식별이 필요해 `@CurrentUserId`(JWT subject)로 userId 를 받는다.
 *   시드/DB 없이 프론트가 붙어볼 수 있도록 `?mock=true` 면 저장 없이 고정 목([CourseIdResponse.MOCK])을 반환한다.
 * - [delete] 코스 삭제(`DELETE /api/v1/courses/{courseId}`): **실구현** — 인바운드 포트([CourseUseCase])로
 *   소프트 삭제한다(deleted_at 스탬프·status=DELETED). 소유자만 삭제 가능하며(그 외 404), data 없이 안내 메시지만 내려준다.
 *   시드/DB 없이 프론트가 붙어볼 수 있도록 `?mock=true` 면 삭제 없이 고정 성공 메시지를 반환한다.
 *
 * 모킹 에러(`?mockError=<code>`)는 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 */
@RestController
@RequestMapping("/api/v1/courses")
class CourseController(
    private val courseUseCase: CourseUseCase,
    private val courseQueryUseCase: CourseQueryUseCase,
    private val mockGuard: MockGuard,
) {
    /**
     * 로그인 작성자의 임시저장 코스를 최근 수정순으로 조회한다. 공개범위는 본인 목록이라 적용하지 않는다.
     * `?mock=true` 이고 [MockGuard] 가 모킹을 허용할 때만 DB 조회 없이 고정 목록을 반환한다.
     */
    @GetMapping("/drafts")
    fun listDrafts(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<List<CourseSummary>> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(DRAFTS_MOCK)
        return ApiResponse.success(courseQueryUseCase.listDraftsByAuthor(userId))
    }

    /**
     * 코스 상세 조회. status=ACTIVE·미삭제 코스만 반환하며 PRIVATE 은 소유자만 조회 가능(그 외 404).
     * `?mock=true` 이고 [MockGuard] 가 모킹을 허용할 때만 DB 조회 없이 고정 목([CourseDetailResponse.MOCK])을
     * 반환하고, 그 외에는 정상 유스케이스 경로를 탄다.
     */
    @GetMapping("/{courseId}")
    fun getDetail(
        @PathVariable courseId: Long,
        @CurrentUserId viewerId: Long?,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CourseDetailResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(CourseDetailResponse.MOCK)
        return ApiResponse.success(CourseDetailResponse.from(courseQueryUseCase.getDetail(courseId, viewerId)))
    }

    /**
     * 코스 생성. 발행(isPublished=true)과 임시저장(false)을 함께 처리한다.
     *
     * 검증
     * - 필드 형식·범위(title·tags·places 등)는 Bean Validation([CreateCourseRequest]) → 400 VALIDATION_FAILED + fieldErrors.
     * - 교차 필드·비즈니스 규칙(장소 2곳 이상, orderNo 중복 금지)은 [CourseUseCase] 가 검증한다 → 400 INVALID_INPUT.
     *
     * `?mock=true` 이고 [MockGuard] 가 모킹을 허용할 때만 DB 저장 없이 고정 목([CourseIdResponse.MOCK])을
     * 반환하고, 그 외에는 정상 유스케이스 경로를 탄다.
     */
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: CreateCourseRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CourseIdResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(CourseIdResponse.MOCK)
        val course = courseUseCase.create(request.toCommand(userId))
        return ApiResponse.success(CourseIdResponse(courseId = requireNotNull(course.id)))
    }

    /**
     * 코스 편집. 코스 만들기와 같은 빌더 화면을 재사용하며, 편집한 코스 전체 상태를
     * 되돌려 보내는 전체 치환 계약이다([EditCourseRequest]) — 보낸 필드로 코스·장소·태그를 덮어쓴다.
     * 소유자만 편집할 수 있고(그 외 404), 발행 전환 시 검증은 생성과 동일하게 도메인이 수행한다.
     * 응답은 courseId 만 반환하며, 프론트는 편집 후 코스 상세 API 재조회로 화면을 구성한다.
     * `?mock=true` 이고 [MockGuard] 가 모킹을 허용할 때만 DB 갱신 없이 고정 목([CourseIdResponse.MOCK])을
     * 반환하고, 그 외에는 정상 유스케이스 경로를 탄다.
     */
    @PatchMapping("/{courseId}")
    fun edit(
        @CurrentUserId userId: Long,
        @PathVariable courseId: Long,
        @Valid @RequestBody request: EditCourseRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CourseIdResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(CourseIdResponse.MOCK)
        val course = courseUseCase.edit(request.toCommand(userId, courseId))
        return ApiResponse.success(CourseIdResponse(courseId = requireNotNull(course.id)))
    }

    /**
     * 코스 포크(노션 명세 · Course · course-fork). 인바운드 포트([CourseUseCase])로 새 코스를 저장한다.
     *
     * 노션 "코스 포크" 페이지가 본문 미작성이라 계약을 기능 정의서(7.2 포크하기 → **7.2.1 게시물 만들기**)와
     * 코스 생성 계약에서 도출했다. 포크는 원본에서 **장소 구성(어디를 어떤 순서로)만** 가져오고,
     * 장소별 caption·사진을 비롯한 콘텐츠는 포크하는 사람이 코스 만들기와 같은 빌더 화면에서 새로 입력해
     * 게시한다([ForkCourseRequest]) — 그래서 요청 모양이 코스 생성과 같고, 원본 id 만 바디가 아니라 경로로 받는다.
     * (디자인 밴드 F 는 포크 버튼 → 완료 화면만 그려 두어 중간 빌더 단계가 생략돼 있다 — 디자인 갱신 필요.)
     *
     * 경로 `{courseId}` 는 **원본** 코스 id, 응답 `courseId` 는 새로 만들어진 **내** 코스 id다.
     * 원본을 볼 수 없으면(없음·삭제·비활성·PRIVATE 타인·FOLLOWER 비팔로워) 코스 상세와 동일하게
     * 404 COURSE_NOT_FOUND 로 응답한다(존재를 드러내지 않는 은닉 규칙).
     * 원본 장소를 규칙만큼 담지 않으면 400 FORK_PLACES_NOT_KEPT 다 — 원본이 4곳 이하면 전부,
     * 5곳 이상이면 절반 이상을 그대로 담아야 하고 장소 추가는 자유롭다. 포크 주체 식별이 필요해
     * `@CurrentUserId`(JWT subject)로 userId 를 받으므로 유효한 토큰이 필요하다.
     * `?mock=true` 이고 [MockGuard] 가 모킹을 허용할 때만 DB 저장 없이 고정 목([CourseIdResponse.MOCK])을
     * 반환하고, 그 외에는 정상 유스케이스 경로를 탄다.
     */
    @PostMapping("/{courseId}/forks")
    @ResponseStatus(HttpStatus.CREATED)
    fun fork(
        @CurrentUserId userId: Long,
        @PathVariable courseId: Long,
        @Valid @RequestBody request: ForkCourseRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CourseIdResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(CourseIdResponse.MOCK)
        val course = courseUseCase.fork(request.toCommand(userId, courseId))
        return ApiResponse.success(CourseIdResponse(courseId = requireNotNull(course.id)))
    }

    /**
     * 코스 삭제(소프트 삭제). 인바운드 포트([CourseUseCase])로 deleted_at 을 찍고 status 를 DELETED 로 전이한다.
     * 소유자만 삭제할 수 있고(없음·비활성·타인 소유는 존재를 드러내지 않도록 404 COURSE_NOT_FOUND), 성공 시 data 없이
     * 안내 메시지만 내려준다. 소유자 식별을 위해 `@CurrentUserId`(JWT subject)로 userId 를 받으므로 유효한 토큰이 필요하다.
     * `?mock=true` 이고 [MockGuard] 가 모킹을 허용할 때만 삭제 없이 고정 성공 메시지를 반환하고,
     * 그 외에는 정상 유스케이스 경로를 탄다.
     */
    @DeleteMapping("/{courseId}")
    fun delete(
        @CurrentUserId userId: Long,
        @PathVariable courseId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.ok("코스가 삭제되었습니다.")
        courseUseCase.delete(userId, courseId)
        return ApiResponse.ok("코스가 삭제되었습니다.")
    }

    private companion object {
        val DRAFTS_MOCK: List<CourseSummary> =
            listOf(
                CourseSummary(
                    id = 2,
                    authorId = 1,
                    title = "비 오는 날 성수 감성 카페 코스",
                    coverImageUrl = "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=600",
                    theme = null,
                    likesCnt = 0,
                    savesCnt = 0,
                    createdAt = Instant.parse("2026-07-20T02:30:00Z"),
                ),
            )
    }
}
