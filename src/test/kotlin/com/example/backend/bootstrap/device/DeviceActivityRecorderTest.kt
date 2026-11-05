package com.example.backend.bootstrap.device

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import java.time.Clock

class DeviceActivityRecorderTest {
    @Test
    fun `큐가 차서 거절되면 다음 요청에 같은 기기를 다시 기록하려 한다`() {
        var calls = 0
        val rejecting =
            TaskExecutor {
                calls++
                throw TaskRejectedException("full")
            }
        val recorder = DeviceActivityRecorder(Clock.systemUTC(), rejecting)

        recorder.record("device-a", null)
        recorder.record("device-a", null)

        assertEquals(2, calls)
    }

    @Test
    fun `쓰기가 실패하면 다음 요청에 같은 기기를 다시 기록하려 한다`() {
        // 컬럼 길이(128)를 넘는 값은 Exposed 가 쓰기 전에 거절한다. DB 연결 여부와 상관없이 실패한다.
        val tooLong = "x".repeat(129)
        var calls = 0
        val sync =
            TaskExecutor {
                calls++
                it.run()
            }
        val recorder = DeviceActivityRecorder(Clock.systemUTC(), sync)

        recorder.record(tooLong, null)
        recorder.record(tooLong, null)

        assertEquals(2, calls)
    }
}
