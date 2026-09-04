package com.example.backend.bootstrap.exception

import com.example.backend.common.response.CommonErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.bind.MissingServletRequestParameterException

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    fun `NoSuchElementException은 표준 라이브러리 영어 메시지를 노출하지 않는다`() {
        val stdlibMessage = "Collection contains no element matching the predicate."

        val response = handler.handleNotFound(NoSuchElementException(stdlibMessage))

        assertEquals(404, response.statusCode.value())
        assertEquals(CommonErrorCode.NOT_FOUND.code, response.body?.code)
        assertEquals(CommonErrorCode.NOT_FOUND.message, response.body?.message)
    }

    @Test
    fun `필수 요청 파라미터 누락은 400 INVALID_INPUT과 파라미터명을 내려준다`() {
        val response = handler.handleMissingParam(MissingServletRequestParameterException("placeId", "Long"))

        assertEquals(400, response.statusCode.value())
        assertEquals(CommonErrorCode.INVALID_INPUT.code, response.body?.code)
        assertEquals("필수 요청 파라미터가 누락되었습니다: placeId", response.body?.message)
    }
}
