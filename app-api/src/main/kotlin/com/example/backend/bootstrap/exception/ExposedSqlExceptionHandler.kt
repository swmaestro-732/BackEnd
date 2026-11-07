package com.example.backend.bootstrap.exception

import com.example.backend.common.response.ApiResponse
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.common.response.ReportErrorCode
import com.example.backend.common.response.UserErrorCode
import com.example.backend.web.exception.GlobalExceptionHandler
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * DB(Exposed) 예외 변환. core-web 의 [GlobalExceptionHandler] 가 Exposed 를 모르게 영속성을 아는 이 모듈에 둔다.
 * [GlobalExceptionHandler] 의 Exception 처리보다 먼저 잡히도록 우선순위를 가장 높게 둔다.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ExposedSqlExceptionHandler(
    private val globalExceptionHandler: GlobalExceptionHandler,
) {
    /** DB UNIQUE 위반 중 제약 대상을 식별할 수 있는 사용자 중복만 409로 변환한다. 나머지는 500 처리로 넘긴다. */
    @ExceptionHandler(ExposedSQLException::class)
    fun handleSqlException(e: ExposedSQLException): ResponseEntity<ApiResponse<Nothing?>> {
        if (e.sqlState == UNIQUE_VIOLATION_SQL_STATE) {
            val message = e.message?.lowercase().orEmpty()
            // 폴더(saved_course_folders)를 저장 코스(saved_courses)보다 먼저 본다. 테이블 이름이 서로 닮아 있다.
            // 리뷰 1인 1개 제한은 테이블명 대신 인덱스명으로 본다. place_reviews 는 사진, 태그 링크 인덱스와도 겹친다.
            val errorCode =
                when {
                    "handle" in message -> UserErrorCode.HANDLE_ALREADY_TAKEN
                    "nickname" in message -> UserErrorCode.NICKNAME_ALREADY_TAKEN
                    "saved_course_folders" in message -> UserErrorCode.FOLDER_NAME_ALREADY_TAKEN
                    "saved_courses" in message -> UserErrorCode.COURSE_ALREADY_SAVED
                    "course_likes" in message -> CourseErrorCode.COURSE_ALREADY_LIKED
                    "saved_places" in message -> PlaceErrorCode.PLACE_ALREADY_SAVED
                    "uq_place_reviews_user_place" in message -> PlaceErrorCode.PLACE_REVIEW_ALREADY_EXISTS
                    "uq_course_reviews_user_course" in message -> CourseErrorCode.COURSE_REVIEW_ALREADY_EXISTS
                    "uq_reports_reporter_target" in message -> ReportErrorCode.REPORT_ALREADY_EXISTS
                    else -> null
                }
            if (errorCode != null) {
                return ResponseEntity.status(errorCode.status).body(ApiResponse.error(errorCode))
            }
        }
        return globalExceptionHandler.handleUnexpected(e)
    }

    private companion object {
        const val UNIQUE_VIOLATION_SQL_STATE = "23505"
    }
}
