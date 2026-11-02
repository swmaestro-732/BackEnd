package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.appversion.AppPlatform
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode

/** 기기 등록 API 들이 함께 쓰는 선택 앱 헤더(X-App-Platform, X-App-Build) 파싱. */
internal object AppHeaders {
    fun platform(value: String?): String? =
        value?.let {
            AppPlatform.fromHeader(it)?.name ?: throw BusinessException(CommonErrorCode.INVALID_APP_HEADER)
        }

    fun build(value: String?): Int? =
        value?.let {
            it.toIntOrNull() ?: throw BusinessException(CommonErrorCode.INVALID_APP_HEADER)
        }
}
