package com.example.backend.course.application.port.inbound.dto

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.domain.model.CourseCategory
import com.example.backend.course.domain.model.CourseStatus
import java.time.Instant

/** 어드민 화면의 코스 상태. 저장 상태(status)와 발행 여부를 합쳐 보여 준다(활성 미발행은 DRAFT). */
enum class AdminCourseStatus {
    ACTIVE,
    DRAFT,
    HIDDEN,
    DELETED,
    ;

    companion object {
        fun of(
            status: CourseStatus,
            isPublished: Boolean,
        ): AdminCourseStatus =
            when (status) {
                CourseStatus.ACTIVE -> if (isPublished) ACTIVE else DRAFT
                CourseStatus.HIDDEN, CourseStatus.SUSPENDED -> HIDDEN
                CourseStatus.DELETED -> DELETED
            }
    }
}

/** 작성자 구분. 큐레이터는 설정값 `admin.curator-user-id` 계정이다. */
enum class AdminCourseAuthorType {
    CURATOR,
    USER,
}

/** 어드민 코스 목록 조건. [status] 가 없으면 삭제되지 않은 코스 전체. [page] 는 0부터. */
data class AdminCourseQuery(
    val status: AdminCourseStatus?,
    val author: AdminCourseAuthorType?,
    val areaCode: String?,
    val theme: CourseCategory?,
    val q: String?,
    val page: Int,
    val size: Int,
)

data class AdminCourseAuthor(
    val id: Long,
    /** 탈퇴했거나 없는 사용자면 null. */
    val nickname: String?,
)

data class AdminCourseItem(
    val id: Long,
    val title: String,
    val coverImageUrl: String?,
    val themes: List<String>,
    val author: AdminCourseAuthor,
    /** 큐레이터 코스면 CURATOR(어드민 편집 가능), 아니면 USER. */
    val authorType: AdminCourseAuthorType,
    val areaCode: String?,
    val areaName: String?,
    val placeCount: Int,
    val status: AdminCourseStatus,
    val isPublished: Boolean,
    val visibility: CourseVisibility,
    val likesCnt: Int,
    val savesCnt: Int,
    val pendingReportCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class AdminCoursePage(
    val items: List<AdminCourseItem>,
    val page: Int,
    val size: Int,
    val total: Long,
)

/** 편집용 장소. name 부터 address 는 장소 도메인 값이라 삭제된 장소면 null. */
data class AdminCoursePlace(
    val placeId: Long,
    val orderNo: Int,
    val name: String?,
    val category: String?,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val caption: String?,
    val imageUrls: List<String>,
    val walkingMinutes: Int?,
)

data class AdminCourseDetail(
    val id: Long,
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    val themes: List<String>,
    /** 작성자가 고른 동행 테마(DATE, FAMILY, SOLO 중). PATCH 의 companions 로 그대로 돌려보내면 된다. */
    val companions: List<String>,
    val tags: List<String>,
    val areaCode: String?,
    val areaName: String?,
    val status: AdminCourseStatus,
    val isPublished: Boolean,
    val visibility: CourseVisibility,
    val author: AdminCourseAuthor,
    /** 큐레이터 코스면 CURATOR(어드민 편집 가능), 아니면 USER. */
    val authorType: AdminCourseAuthorType,
    val places: List<AdminCoursePlace>,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Instant,
    /** PATCH 의 expectedUpdatedAt 으로 그대로 보낸다(다르면 409). */
    val updatedAt: Instant,
)

/** 어드민 코스 수정. 본문은 사용자 편집([EditCourseCommand])과 같고 [expectedUpdatedAt] 으로 낙관적 확인을 한다. */
data class AdminEditCourseCommand(
    val adminEmail: String,
    val courseId: Long,
    val expectedUpdatedAt: Instant,
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    val tags: List<String>,
    val visibility: CourseVisibility,
    val isPublished: Boolean,
    val companions: List<CourseCategory>?,
    val places: List<CreateCoursePlaceCommand>,
)

/**
 * 저장 전 미리보기 초안. 실제 코스 상세와 같은 규칙으로 테마와 지역을 계산하되 저장, 외부 도보 API 호출은 하지 않는다.
 * [authorId] 가 없으면 큐레이터, [courseId] 는 편집 중인 코스가 있으면 그 id(응답 표시용).
 */
data class CoursePreviewCommand(
    val courseId: Long?,
    val authorId: Long?,
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    /** [CourseCategory] 이름(DATE, FAMILY, SOLO). 모르는 이름이면 400. */
    val companions: List<String>,
    val tags: List<String>,
    val places: List<CreateCoursePlaceCommand>,
)
