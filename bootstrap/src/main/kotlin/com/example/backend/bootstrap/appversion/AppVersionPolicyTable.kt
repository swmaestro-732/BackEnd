package com.example.backend.bootstrap.appversion

import org.jetbrains.exposed.v1.core.Table

internal object AppVersionPolicyTable : Table("app_version_policies") {
    val id = long("id").autoIncrement()
    val feature = varchar("feature", 50)
    val platform = varchar("platform", 10)
    val minBuild = integer("min_build")

    override val primaryKey = PrimaryKey(id)
}
