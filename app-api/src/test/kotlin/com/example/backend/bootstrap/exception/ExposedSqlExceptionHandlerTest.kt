package com.example.backend.bootstrap.exception

import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.common.response.ReportErrorCode
import com.example.backend.common.response.UserErrorCode
import com.example.backend.web.exception.GlobalExceptionHandler
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ExposedSqlExceptionHandlerTest {
    private val handler = ExposedSqlExceptionHandler(GlobalExceptionHandler())

    @Test
    fun `handleSqlException, UNIQUE 위반 handle 컬럼은 409 HANDLE_ALREADY_TAKEN 을 내려준다`() {
        val e = mock(ExposedSQLException::class.java)
        `when`(e.sqlState).thenReturn("23505")
        `when`(e.message).thenReturn("duplicate key value violates unique constraint \"users_handle_key\"")

        val response = handler.handleSqlException(e)

        assertEquals(409, response.statusCode.value())
        assertEquals(UserErrorCode.HANDLE_ALREADY_TAKEN.code, response.body?.code)
    }

    @Test
    fun `handleSqlException, UNIQUE 위반 nickname 컬럼은 409 NICKNAME_ALREADY_TAKEN 을 내려준다`() {
        val e = mock(ExposedSQLException::class.java)
        `when`(e.sqlState).thenReturn("23505")
        `when`(e.message).thenReturn("duplicate key value violates unique constraint \"users_nickname_key\"")

        val response = handler.handleSqlException(e)

        assertEquals(409, response.statusCode.value())
        assertEquals(UserErrorCode.NICKNAME_ALREADY_TAKEN.code, response.body?.code)
    }

    @Test
    fun `handleSqlException, UNIQUE 위반 saved_places 테이블은 409 PLACE_ALREADY_SAVED 를 내려준다`() {
        val e = mock(ExposedSQLException::class.java)
        `when`(e.sqlState).thenReturn("23505")
        `when`(e.message).thenReturn("duplicate key value violates unique constraint \"saved_places_pkey\"")

        val response = handler.handleSqlException(e)

        assertEquals(409, response.statusCode.value())
        assertEquals(PlaceErrorCode.PLACE_ALREADY_SAVED.code, response.body?.code)
    }

    @Test
    fun `handleSqlException, UNIQUE 위반 saved_course_folders 테이블은 409 FOLDER_NAME_ALREADY_TAKEN 을 내려준다`() {
        val e = mock(ExposedSQLException::class.java)
        `when`(e.sqlState).thenReturn("23505")
        `when`(e.message).thenReturn("duplicate key value violates unique constraint \"saved_course_folders_name_key\"")

        val response = handler.handleSqlException(e)

        assertEquals(409, response.statusCode.value())
        assertEquals(UserErrorCode.FOLDER_NAME_ALREADY_TAKEN.code, response.body?.code)
    }

    @Test
    fun `handleSqlException, UNIQUE 위반 uq_reports_reporter_target 인덱스는 409 REPORT_ALREADY_EXISTS 를 내려준다`() {
        val e = mock(ExposedSQLException::class.java)
        `when`(e.sqlState).thenReturn("23505")
        `when`(e.message).thenReturn("duplicate key value violates unique constraint \"uq_reports_reporter_target\"")

        val response = handler.handleSqlException(e)

        assertEquals(409, response.statusCode.value())
        assertEquals(ReportErrorCode.REPORT_ALREADY_EXISTS.code, response.body?.code)
    }

    @Test
    fun `handleSqlException, UNIQUE 위반 saved_courses 테이블은 409 COURSE_ALREADY_SAVED 를 내려준다`() {
        val e = mock(ExposedSQLException::class.java)
        `when`(e.sqlState).thenReturn("23505")
        `when`(e.message).thenReturn("duplicate key value violates unique constraint \"saved_courses_pkey\"")

        val response = handler.handleSqlException(e)

        assertEquals(409, response.statusCode.value())
        assertEquals(UserErrorCode.COURSE_ALREADY_SAVED.code, response.body?.code)
    }

    @Test
    fun `handleSqlException, UNIQUE 위반이지만 알 수 없는 테이블은 500 을 내려준다`() {
        val e = mock(ExposedSQLException::class.java)
        `when`(e.sqlState).thenReturn("23505")
        `when`(e.message).thenReturn("duplicate key value violates unique constraint \"unknown_table_key\"")

        val response = handler.handleSqlException(e)

        assertEquals(500, response.statusCode.value())
    }

    @Test
    fun `handleSqlException, UNIQUE 위반이 아닌 SQL 예외는 500 을 내려준다`() {
        val e = mock(ExposedSQLException::class.java)
        `when`(e.sqlState).thenReturn("08001") // connection error, not UNIQUE

        val response = handler.handleSqlException(e)

        assertEquals(500, response.statusCode.value())
    }
}
