package com.example.backend.support

import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.GlobalStatementInterceptor
import org.jetbrains.exposed.v1.core.statements.StatementContext

/**
 * 테스트가 실행하는 동안 Exposed 가 DB 로 보내는 SQL 과 바인딩 값을 모은다.
 * [SqlCaptureInterceptor] 가 META-INF/services 로 전역 등록되고, [record] 블록 안에서만 기록한다.
 * 버퍼는 호출 스레드에 묶여 있어 테스트가 병렬로 돌아도 서로 섞이지 않는다.
 */
object SqlCapture {
    data class Statement(
        val sql: String,
        val args: List<String>,
    )

    private val buffer = ThreadLocal<MutableList<Statement>>()

    fun <T> record(block: () -> T): Pair<T, List<Statement>> {
        val statements = mutableListOf<Statement>()
        buffer.set(statements)
        try {
            return block() to statements.toList()
        } finally {
            buffer.remove()
        }
    }

    internal fun add(statement: Statement) {
        buffer.get()?.add(statement)
    }
}

class SqlCaptureInterceptor : GlobalStatementInterceptor {
    override fun beforeExecution(
        transaction: Transaction,
        context: StatementContext,
    ) {
        @Suppress("UNCHECKED_CAST")
        val args = context.args.map { (type, value) -> (type as IColumnType<Any?>).valueToString(value) }
        SqlCapture.add(SqlCapture.Statement(context.sql(transaction), args))
    }
}
