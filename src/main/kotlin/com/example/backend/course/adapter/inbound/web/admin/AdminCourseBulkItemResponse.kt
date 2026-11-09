package com.example.backend.course.adapter.inbound.web.admin

/** 일괄 조치의 건별 결과. 실패하면 [code] 와 [message] 에 그 건의 에러를 싣는다. */
data class AdminCourseBulkItemResponse(
    val id: Long,
    val success: Boolean,
    val code: Int? = null,
    val message: String? = null,
)
