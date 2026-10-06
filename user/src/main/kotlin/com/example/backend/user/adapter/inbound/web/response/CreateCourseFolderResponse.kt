package com.example.backend.user.adapter.inbound.web.response

/** 코스 저장 폴더 생성 응답 — 생성된 폴더 id(코스 생성 `CreateCourseResponse` 와 동일 패턴). */
data class CreateCourseFolderResponse(
    val folderId: Long,
) {
    companion object {
        /**
         * 목 생성 응답 — 폴더 목록 모킹([CourseFolderListResponse.mock])의 목 폴더(1~3) 다음 번호로 고정한다.
         */
        fun mock(): CreateCourseFolderResponse = CreateCourseFolderResponse(folderId = MOCK_FOLDER_ID)

        private const val MOCK_FOLDER_ID = 4L
    }
}
