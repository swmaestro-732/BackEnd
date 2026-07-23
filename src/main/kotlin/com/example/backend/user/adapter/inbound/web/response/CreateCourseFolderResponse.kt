package com.example.backend.user.adapter.inbound.web.response

/** 코스 저장 폴더 생성 응답 — 생성된 폴더 id(코스 생성 `CreateCourseResponse` 와 동일 패턴). */
data class CreateCourseFolderResponse(
    val folderId: Long,
)
