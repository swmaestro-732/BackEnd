package com.example.backend.mobile.course.application.service

import com.example.backend.course.application.port.inbound.CourseUseCase
import com.example.backend.mobile.course.application.port.inbound.CourseMobileUseCase
import com.example.backend.mobile.course.application.port.inbound.dto.CourseDetailScreenResult
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.user.application.port.inbound.UserUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 상세 화면 조합 서비스 (BFF). 도메인 인바운드 포트 3개를 순서대로 호출해 한 화면 응답 재료를 만든다 —
 * 코스 상세([CourseUseCase]) → 작성자 프로필([UserUseCase]) → 코스에 담긴 장소 요약([PlaceQueryUseCase]).
 * 조합 한 번을 하나의 읽기 트랜잭션으로 묶어 일관된 스냅샷을 본다.
 */
@Service
@Transactional(readOnly = true)
class CourseMobileService(
    private val courseUseCase: CourseUseCase,
    private val userUseCase: UserUseCase,
    private val placeQueryUseCase: PlaceQueryUseCase,
) : CourseMobileUseCase {
    override fun getScreen(
        courseId: Long,
        viewerId: Long?,
    ): CourseDetailScreenResult {
        val course = courseUseCase.getDetail(courseId, viewerId)
        val author = userUseCase.getProfile(course.authorId, viewerId)
        val places = placeQueryUseCase.findPlacesById(course.places.map { it.placeId })
        return CourseDetailScreenResult(course = course, author = author, places = places)
    }
}
