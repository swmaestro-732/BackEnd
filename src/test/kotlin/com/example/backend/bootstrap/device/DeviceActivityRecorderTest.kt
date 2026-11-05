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
        // DB 연결 없이 transaction 을 열어 쓰기를 실패시킨다.
        var calls = 0
        val sync =
            TaskExecutor {
                calls++
                it.run()
            }
        val recorder = DeviceActivityRecorder(Clock.systemUTC(), sync)

        recorder.record("device-a", null)
        recorder.record("device-a", null)

        assertEquals(2, calls)
    }
}
