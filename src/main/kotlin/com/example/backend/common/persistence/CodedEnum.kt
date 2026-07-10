package com.example.backend.common.persistence

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import kotlin.enums.enumEntries

/**
 * SMALLINT 코드로 저장되는 enum 규약 (V1__init.sql: 앱 레벨 enum ↔ SMALLINT).
 * 코드 값은 DB 에 저장되는 계약이므로 enum 순서 변경과 무관하게 유지되어야 한다.
 */
interface CodedEnum {
    val code: Short
}

/** SMALLINT 컬럼을 [CodedEnum] 구현 enum 으로 읽고 쓰는 컬럼 정의. */
internal inline fun <reified E> Table.codeEnum(name: String): Column<E> where E : Enum<E>, E : CodedEnum =
    short(name).transform(
        wrap = { code ->
            enumEntries<E>().firstOrNull { it.code == code }
                ?: throw IllegalStateException("${E::class.simpleName} 에 정의되지 않은 코드: $code")
        },
        unwrap = { it.code },
    )
