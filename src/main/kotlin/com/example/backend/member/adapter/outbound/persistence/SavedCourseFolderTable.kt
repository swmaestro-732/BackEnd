package com.example.backend.member.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table

internal object SavedCourseFolderTable : Table("saved_course_folders") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val name = varchar("name", 200)
    val orderNo = short("order_no")
    override val primaryKey = PrimaryKey(id)
}
