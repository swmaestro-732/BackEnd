package com.example.backend.mobile.user.application.port.inbound.dto

import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.user.application.port.inbound.dto.SavedCourseFolderCount
import com.example.backend.user.application.port.inbound.dto.UserProfileResult
import java.time.Instant

/**
 * 저장함 코스 탭 화면 조합 결과(BFF 애플리케이션 계층 DTO).
 * 도메인 인바운드 포트들의 결과를 묶기만 한다 — 표시 로직(소요 시간 텍스트·내 코스 여부·장소 핀 조립 등)은 web 계층 매퍼가 입힌다.
 *
 * - [totalCount] 폴더 필터 안의 전체 저장 코스 개수(완주 필터와 무관, "전체 N" 칩).
 * - [completedCount] 폴더 필터 안의 완주 코스 개수("완주 N" 칩, 안 가봄 개수는 total - completed).
 * - [folders] 폴더 칩(order_no 순, 폴더별 저장 개수).
 * - [withoutFolderCount] 폴더 없이 저장한 코스 개수("폴더 없음" 칩) — 폴더별 개수의 합에 포함되지 않는다.
 * - [viewerId] 조회 주체 — 각 코스의 작성자와 비교해 "내가 만든 코스"(isMine)를 판정한다.
 * - [items] 페이지의 저장 코스들(완주/폴더 필터·커서 적용). 삭제·비공개로 볼 수 없게 된 코스는 서비스에서 제외한다.
 */
data class SavedCourseScreenResult(
    val totalCount: Long,
    val completedCount: Long,
    val folders: List<SavedCourseFolderCount>,
    val withoutFolderCount: Long,
    val nextCursor: String?,
    val hasNext: Boolean,
    val viewerId: Long,
    val items: List<Item>,
) {
    /**
     * 저장 코스 한 건 = 저장 레코드 + 코스 상세 + 작성자 프로필 + 완주 시각 + 장소 좌표 조회 결과.
     * - [completedAt] 완주 시각(따라간 적 없으면 null).
     * - [placeById] 이 코스에 담긴 장소들의 좌표(지도 핀). 삭제된 장소는 빠질 수 있어 map 으로 둔다.
     */
    data class Item(
        val savedId: Long,
        val folderId: Long?,
        val savedAt: Instant,
        val completedAt: Instant?,
        val course: CourseDetailResult,
        val author: UserProfileResult,
        val placeById: Map<Long, PlaceSummary>,
    )
}
