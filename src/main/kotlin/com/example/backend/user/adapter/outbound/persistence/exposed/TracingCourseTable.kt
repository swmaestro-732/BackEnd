package com.example.backend.user.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock

// LongIdTable 이 id(EntityID<Long>, "id" 컬럼)·primaryKey 를 제공한다 → DSL insert 후 생성 id 획득에 쓴다.
// created_at 은 클라이언트 기본값을 둔다(SavedCourseTable 선례) — DB 전용 DEFAULT 를 못 태우는 경로 대비.
internal object TracingCourseTable : LongIdTable("tracing_courses") {
    val userId = long("user_id")
    val courseId = long("course_id") // cross-domain(course): FK 없음
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val completedAt = timestamp("completed_at").nullable() // 완주 시각, NULL = 진행중
}
