package com.example.backend.course.application.port.inbound

import java.time.Instant

/**
 * 공개 이벤트 계약: 어드민이 사용자 코스를 숨기거나 삭제했음을 알린다(큐레이터 코스, 숨김 해제, 복원은 알리지 않는다).
 * 알림 도메인이 커밋 후 작성자 수신함에 남긴다. 코스는 이미 사용자에게 안 보이므로 표시용 제목과 커버를 함께 싣는다.
 */
interface CourseModeratedByAdmin {
    val authorId: Long
    val courseId: Long
    val courseTitle: String
    val coverImageUrl: String?
    val action: CourseModerationAction
    val occurredAt: Instant
}

enum class CourseModerationAction {
    HIDDEN,
    DELETED,
}
