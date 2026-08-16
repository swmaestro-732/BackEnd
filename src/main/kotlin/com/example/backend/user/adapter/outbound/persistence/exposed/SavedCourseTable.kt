package com.example.backend.user.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock

// LongIdTable 이 id(EntityID<Long>, "id" 컬럼)·primaryKey 를 제공한다 → DAO(SavedCourseEntity)·DSL 공용.
// created_at 은 클라이언트 기본값을 둔다 — DAO(.new)는 batch insert 라 DB 전용 DEFAULT 를 못 태우기 때문.
internal object SavedCourseTable : LongIdTable("saved_courses") {
    val userId = long("user_id")
    val folderId = long("folder_id").nullable() // NULL = 폴더 미분류
    val courseId = long("course_id") // cross-domain(course): FK 없음
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val deletedAt = timestamp("deleted_at").nullable() // 소프트 삭제 스탬프, NULL = 살아있음
}
