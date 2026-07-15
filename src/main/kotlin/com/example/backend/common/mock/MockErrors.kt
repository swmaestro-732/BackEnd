package com.example.backend.common.mock

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode

/**
 * 모킹 API 전용 에러 주입 지원.
 * 클라이언트가 `mockError` 파라미터로 에러 코드를 넘기면 해당 모킹 에러를 강제 반환한다
 * — 프론트가 에러 화면/핸들링을 실제 응답 형태로 작업할 수 있게 하기 위함(위키 api-design.md 모킹 프로세스).
 * 해당 API가 실제 구현으로 전환되면 호출부와 함께 제거한다.
 */
object MockErrors {
    fun throwIfRequested(mockError: Int?) {
        if (mockError == null) return
        val errorCode = ErrorCode.entries.firstOrNull { it.code == mockError } ?: ErrorCode.INVALID_INPUT
        throw BusinessException(errorCode)
    }
}
