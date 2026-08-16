package com.example.backend.user.adapter.inbound.web.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 코스 저장 폴더 생성 요청 — 웹 어댑터 DTO.
 * 노션 필드 명세 미작성 상태라 디자인(저장 → 새 폴더 만들기 시트)에서 도출한 필드.
 *
 * - name: 폴더 이름. 디자인 카운터("7/20") 기준 최대 20자. 같은 사용자 안에서 중복 불가(409).
 *
 * 디자인의 설명·비공개 토글은 이번 실구현 범위에서 제외했다(스키마는 name·order_no 만 유지).
 */
data class CreateCourseFolderRequest(
    @field:NotBlank
    @field:Size(max = 20)
    val name: String,
)
