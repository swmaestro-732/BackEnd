package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CourseSearchCommand
import com.example.backend.course.application.port.inbound.dto.CourseSearchResult

/**
 * 인바운드 포트 — 공개 코스 검색(`GET /api/v1/courses/search`). 키워드·필터·정렬로 발행 PUBLIC 코스를 찾는다.
 * 커서(문자열) 디코딩/인코딩과 검색 포트 호출을 서비스가 담당하고 컨트롤러는 Request→포트→Response 매핑만 한다.
 */
interface CourseSearchUseCase {
    fun search(command: CourseSearchCommand): CourseSearchResult
}
