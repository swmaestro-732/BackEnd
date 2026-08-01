package com.example.backend.user.adapter.inbound.web

import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.request.CreateCourseFolderRequest
import com.example.backend.user.adapter.inbound.web.response.CourseFolderListResponse
import com.example.backend.user.adapter.inbound.web.response.CreateCourseFolderResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스 저장 폴더(노션 명세 · User · user-course). **모킹 API**.
 *
 * - [create] 코스 저장 폴더 생성(`POST /api/v1/my/course-folders`): 노션 필드 명세 미작성 상태라
 *   디자인(저장 → 새 폴더 만들기 시트)에서 필드를 도출했다([CreateCourseFolderRequest]).
 * - [list] 코스 폴더 목록 조회(`GET /api/v1/my/course-folders`): 디자인(저장함 · 코스 탭 폴더 칩,
 *   코스 상세 → 저장 시트 "내 폴더 N")에서 도출 — 폴더별 저장 코스 개수를 order_no 순으로 내려준다.
 *
 * 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체한다. 모킹 에러(`?mockError=<code>`)는
 * 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 *
 * 경로: `/service/v1/course-folders` 가 정식이며 `/api/v1/my/course-folders` 는 deprecated 별칭(동일 핸들러).
 */
@RestController
@RequestMapping(value = ["/service/v1/course-folders", "/api/v1/my/course-folders"])
class CourseFolderController {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateCourseFolderRequest,
    ): ApiResponse<CreateCourseFolderResponse> =
        ApiResponse.success(CreateCourseFolderResponse(folderId = NEXT_FOLDER_ID), "폴더가 생성되었습니다.")

    @GetMapping
    fun list(): ApiResponse<CourseFolderListResponse> = ApiResponse.success(CourseFolderListResponse.mock())

    private companion object {
        /** 생성 모킹 고정 id — 목 폴더(1~3) 다음 번호. */
        const val NEXT_FOLDER_ID = 4L
    }
}
