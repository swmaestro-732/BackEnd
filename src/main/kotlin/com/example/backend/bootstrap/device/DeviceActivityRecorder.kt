package com.example.backend.bootstrap.device

import kotlinx.datetime.toKotlinLocalDate
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

/**
 * 기기의 그날(KST) 활동을 한 번만 기록한다. 이미 기록한 (기기, 날짜) 는 메모리에서 걸러 요청마다 DB 에 쓰지 않는다.
 * 인스턴스가 여러 대면 인스턴스마다 한 번씩 쓰지만 PK 가 중복을 막는다.
 */
@Component
class DeviceActivityRecorder(
    private val clock: Clock,
) {
    // 헤더는 비로그인 요청도 보내므로 무작위 ID 를 뿌리면 집합이 끝없이 커진다. [MAX_SEEN] 을 넘으면 비우고 다시 채운다
    // (비운 뒤에는 이미 기록한 기기도 한 번 더 insert 하지만 ON CONFLICT 로 끝난다).
    @Volatile
    private var seen = DaySeen(LocalDate.MIN)

    /** 이미 본 기기는 커넥션을 잡지 않도록 중복 검사를 트랜잭션 밖에서 한다. */
    fun record(
        deviceId: String,
        userId: Long?,
    ) {
        val today = LocalDate.now(clock.withZone(KST))
        val current = seen.takeIf { it.date == today && it.ids.size < MAX_SEEN } ?: DaySeen(today).also { seen = it }
        if (deviceId in current.ids) return

        transaction {
            DeviceActivityTable.insertIgnore {
                it[DeviceActivityTable.deviceId] = deviceId
                it[activeDate] = today.toKotlinLocalDate()
                it[DeviceActivityTable.userId] = userId
            }
        }
        current.ids.add(deviceId)
    }

    private class DaySeen(
        val date: LocalDate,
    ) {
        val ids: MutableSet<String> = ConcurrentHashMap.newKeySet()
    }

    private companion object {
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
        const val MAX_SEEN = 100_000
    }
}
