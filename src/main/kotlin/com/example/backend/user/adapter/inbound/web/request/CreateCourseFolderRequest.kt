package com.example.backend.user.adapter.inbound.web.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 코스 저장 폴더 생성 요청(모킹 API) — 웹 어댑터 DTO.
 * 노션 필드 명세 미작성 상태라 디자인(저장 → 새 폴더 만들기 시트)에서 도출한 필드.
 *
 * - name: 폴더 이름. 디자인 카운터("7/20") 기준 최대 20자.
 * - description: 설명(선택) — 디자인 "설명 (선택)" 입력.
 * - isPrivate: 비공개 폴더 여부 — 디자인 "비공개 폴더 · 나만 볼 수 있어요" 토글. 생략 시 공개(false).
 *
 * description·isPrivate 는 saved_course_folders 스키마 미반영 필드(name·order_no 만 존재) —
 * 실구현 전 마이그레이션 필요(유저 handle·profileImageUrl 선례).
 */
data class CreateCourseFolderRequest(
    @field:NotBlank
    @field:Size(max = 20)
    val name: String,
    val description: String?,
    val isPrivate: Boolean = false,
)
