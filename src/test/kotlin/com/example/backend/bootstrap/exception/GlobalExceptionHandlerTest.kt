package com.example.backend.bootstrap.exception

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.common.response.UserErrorCode
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.HttpInputMessage
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

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

    @Test
    fun `BusinessException 은 ErrorCode 기준 상태코드와 code를 내려준다`() {
        val response = handler.handleBusiness(BusinessException(CommonErrorCode.NOT_FOUND))

        assertEquals(404, response.statusCode.value())
        assertEquals(CommonErrorCode.NOT_FOUND.code, response.body?.code)
        assertEquals(CommonErrorCode.NOT_FOUND.message, response.body?.message)
    }

    @Test
    fun `BusinessException 에 커스텀 message를 넘기면 해당 문구가 내려간다`() {
        val response = handler.handleBusiness(BusinessException(UserErrorCode.USER_NOT_FOUND, "커스텀 메시지"))

        assertEquals(404, response.statusCode.value())
        assertEquals("커스텀 메시지", response.body?.message)
    }

    @Test
    fun `BusinessException 으로 409 UserErrorCode 를 던지면 409 가 내려간다`() {
        val response = handler.handleBusiness(BusinessException(PlaceErrorCode.PLACE_ALREADY_SAVED))

        assertEquals(409, response.statusCode.value())
        assertEquals(PlaceErrorCode.PLACE_ALREADY_SAVED.code, response.body?.code)
    }

    @Test
    fun `요청 본문 파싱 실패는 400 INVALID_INPUT 고정 문구를 내려준다`() {
        val response =
            handler.handleUnreadableBody(
                HttpMessageNotReadableException("bad input", mock(HttpInputMessage::class.java)),
            )

        assertEquals(400, response.statusCode.value())
        assertEquals(CommonErrorCode.INVALID_INPUT.code, response.body?.code)
        assertEquals("요청 본문 형식이 올바르지 않습니다.", response.body?.message)
    }

    @Test
    fun `IllegalArgumentException 은 400 INVALID_INPUT 과 예외 메시지를 내려준다`() {
        val response = handler.handleBadRequest(IllegalArgumentException("유효하지 않은 커서입니다."))

        assertEquals(400, response.statusCode.value())
        assertEquals(CommonErrorCode.INVALID_INPUT.code, response.body?.code)
        assertEquals("유효하지 않은 커서입니다.", response.body?.message)
    }

    @Test
    fun `MethodArgumentTypeMismatchException 은 400 INVALID_INPUT 과 파라미터명을 내려준다`() {
        val e = mock(MethodArgumentTypeMismatchException::class.java)
        `when`(e.name).thenReturn("cursor")

        val response = handler.handleTypeMismatch(e)

        assertEquals(400, response.statusCode.value())
        assertEquals(CommonErrorCode.INVALID_INPUT.code, response.body?.code)
        assertEquals("요청 파라미터 형식이 올바르지 않습니다: cursor", response.body?.message)
    }

    @Test
    fun `처리되지 않은 예외는 500 INTERNAL_ERROR 를 내려준다`() {
        val response = handler.handleUnexpected(RuntimeException("unexpected"))

        assertEquals(500, response.statusCode.value())
        assertEquals(CommonErrorCode.INTERNAL_ERROR.code, response.body?.code)
    }

    @Test
    fun `handleUnexpected 에서 AuthenticationException 은 재던진다`() {
        val e = object : AuthenticationException("auth fail") {}

        assertThrows<AuthenticationException> { handler.handleUnexpected(e) }
    }

    @Test
    fun `handleUnexpected 에서 AccessDeniedException 은 재던진다`() {
        assertThrows<AccessDeniedException> { handler.handleUnexpected(AccessDeniedException("denied")) }
    }

    @Test
    fun `handleValidation — fieldErrors 를 400 VALIDATION_FAILED 응답으로 변환한다`() {
        val e = mock(MethodArgumentNotValidException::class.java)
        val bindingResult = mock(BindingResult::class.java)
        `when`(e.bindingResult).thenReturn(bindingResult)
        `when`(bindingResult.fieldErrors).thenReturn(
            listOf(FieldError("req", "title", "크기가 200 이하이어야 합니다")),
        )

        val response = handler.handleValidation(e)

        assertEquals(400, response.statusCode.value())
        assertEquals(CommonErrorCode.VALIDATION_FAILED.code, response.body?.code)
        assertEquals(1, response.body?.fieldErrors?.size)
        assertEquals(
            "title",
            response.body
                ?.fieldErrors
                ?.first()
                ?.field,
        )
        assertEquals(
            "크기가 200 이하이어야 합니다",
            response.body
                ?.fieldErrors
                ?.first()
                ?.reason,
        )
    }

    @Test
    fun `handleSqlException — UNIQUE 위반 handle 컬럼은 409 HANDLE_ALREADY_TAKEN 을 내려준다`() {
        val e = mock(ExposedSQLException::class.java)
        `when`(e.sqlState).thenReturn("23505")
        `when`(e.message).thenReturn("duplicate key value violates unique constraint \"users_handle_key\"")

        val response = handler.handleSqlException(e)

        assertEquals(409, response.statusCode.value())
        assertEquals(UserErrorCode.HANDLE_ALREADY_TAKEN.code, response.body?.code)
    }

    @Test
    fun `handleSqlException — UNIQUE 위반 nickname 컬럼은 409 NICKNAME_ALREADY_TAKEN 을 내려준다`() {
        val e = mock(ExposedSQLException::class.java)
        `when`(e.sqlState).thenReturn("23505")
        `when`(e.message).thenReturn("duplicate key value violates unique constraint \"users_nickname_key\"")

        val response = handler.handleSqlException(e)

        assertEquals(409, response.statusCode.value())
        assertEquals(UserErrorCode.NICKNAME_ALREADY_TAKEN.code, response.body?.code)
    }

    @Test
    fun `handleSqlException — UNIQUE 위반 saved_places 테이블은 409 PLACE_ALREADY_SAVED 를 내려준다`() {
        val e = mock(ExposedSQLException::class.java)
        `when`(e.sqlState).thenReturn("23505")
        `when`(e.message).thenReturn("duplicate key value violates unique constraint \"saved_places_pkey\"")

        val response = handler.handleSqlException(e)

        assertEquals(409, response.statusCode.value())
        assertEquals(PlaceErrorCode.PLACE_ALREADY_SAVED.code, response.body?.code)
    }

    @Test
    fun `handleSqlException — UNIQUE 위반 saved_course_folders 테이블은 409 FOLDER_NAME_ALREADY_TAKEN 을 내려준다`() {
        val e = mock(ExposedSQLException::class.java)
        `when`(e.sqlState).thenReturn("23505")
        `when`(e.message).thenReturn("duplicate key value violates unique constraint \"saved_course_folders_name_key\"")

        val response = handler.handleSqlException(e)

        assertEquals(409, response.statusCode.value())
        assertEquals(UserErrorCode.FOLDER_NAME_ALREADY_TAKEN.code, response.body?.code)
    }

    @Test
    fun `handleSqlException — UNIQUE 위반 saved_courses 테이블은 409 COURSE_ALREADY_SAVED 를 내려준다`() {
        val e = mock(ExposedSQLException::class.java)
        `when`(e.sqlState).thenReturn("23505")
        `when`(e.message).thenReturn("duplicate key value violates unique constraint \"saved_courses_pkey\"")

        val response = handler.handleSqlException(e)

        assertEquals(409, response.statusCode.value())
        assertEquals(UserErrorCode.COURSE_ALREADY_SAVED.code, response.body?.code)
    }

    @Test
    fun `handleSqlException — UNIQUE 위반이지만 알 수 없는 테이블은 500 을 내려준다`() {
        val e = mock(ExposedSQLException::class.java)
        `when`(e.sqlState).thenReturn("23505")
        `when`(e.message).thenReturn("duplicate key value violates unique constraint \"unknown_table_key\"")

        val response = handler.handleSqlException(e)

        assertEquals(500, response.statusCode.value())
    }

    @Test
    fun `handleSqlException — UNIQUE 위반이 아닌 SQL 예외는 500 을 내려준다`() {
        val e = mock(ExposedSQLException::class.java)
        `when`(e.sqlState).thenReturn("08001") // connection error, not UNIQUE

        val response = handler.handleSqlException(e)

        assertEquals(500, response.statusCode.value())
    }
}
