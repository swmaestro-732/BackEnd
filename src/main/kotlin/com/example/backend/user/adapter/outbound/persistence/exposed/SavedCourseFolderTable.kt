package com.example.backend.user.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

// LongIdTable 이 id(EntityID<Long>, "id" 컬럼)·primaryKey 를 제공한다 → DAO(SavedCourseFolderEntity)·DSL 공용.
internal object SavedCourseFolderTable : LongIdTable("saved_course_folders") {
    val userId = long("user_id")
    val name = varchar("name", 200)
    val orderNo = short("order_no")
}

/**
 * saved_course_folders 테이블의 DAO 엔티티([SavedCourseFolderTable] 과 한 쌍이라 같은 파일에 둔다).
 * 같은 테이블을 DSL 로도 조회할 수 있다(DAO·DSL 공용). 트랜잭션에 묶인 가변 영속 객체이므로
 * 어댑터(outbound) 밖으로 내보내지 않고, 읽기 모델은 순수 DTO 로 변환해 반환한다.
 */
internal class SavedCourseFolderEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<SavedCourseFolderEntity>(SavedCourseFolderTable)

    var userId by SavedCourseFolderTable.userId
    var name by SavedCourseFolderTable.name
    var orderNo by SavedCourseFolderTable.orderNo
}
