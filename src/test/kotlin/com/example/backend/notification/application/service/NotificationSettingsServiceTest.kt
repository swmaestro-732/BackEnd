package com.example.backend.notification.application.service

import com.example.backend.notification.application.port.inbound.dto.UpdateNotificationSettingsCommand
import com.example.backend.notification.application.port.outbound.NotificationSettingsPersistencePort
import com.example.backend.notification.domain.model.NotificationSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class NotificationSettingsServiceTest {
    private val settings = mock(NotificationSettingsPersistencePort::class.java)
    private val service = NotificationSettingsService(settings)

    @Test
    fun `저장된 설정이 없으면 모든 알림을 허용한다`() {
        `when`(settings.findByUser(7L)).thenReturn(null)

        assertThat(service.get(7L)).isEqualTo(NotificationSettings(true, true, true, true, true))
        verify(settings).findByUser(7L)
    }

    @Test
    fun `저장된 설정이 있으면 그대로 반환한다`() {
        val saved = NotificationSettings(false, true, false, true, false)
        `when`(settings.findByUser(7L)).thenReturn(saved)

        assertThat(service.get(7L)).isEqualTo(saved)
        verify(settings).findByUser(7L)
    }

    @Test
    fun `부분 수정 명령과 사용자 식별자를 전달하고 저장 결과를 반환한다`() {
        val command = UpdateNotificationSettingsCommand(activityLike = false, activityReview = true)
        val saved = NotificationSettings(false, false, true, false, true)
        `when`(settings.update(7L, command)).thenReturn(saved)

        assertThat(service.update(7L, command)).isEqualTo(saved)
        verify(settings).update(7L, command)
    }
}
