package com.example.backend.course.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock

// 코스 좋아요 토글 레코드 — LongIdTable 이 id·primaryKey 를 제공하고, created_at 은 클라이언트 기본값을 둔다.
// 취소는 하드 삭제라 소프트 삭제 컬럼(deleted_at)을 두지 않는다.
internal object CourseLikeTable : LongIdTable("course_likes") {
    val userId = long("user_id") // cross-domain(user): FK 없음
    val courseId = long("course_id")
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
}
