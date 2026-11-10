package com.example.backend.notification.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

class NotificationSettingsTest {
    @Test
    fun `기본 설정은 네 활동을 모두 허용한다`() {
        assertThat(NotificationSettings.DEFAULT).isEqualTo(NotificationSettings(true, true, true, true))
        assertThat(NotificationType.entries.filter(NotificationSettings.DEFAULT::allows))
            .containsExactly(*NotificationType.entries.toTypedArray())
    }

    @ParameterizedTest
    @CsvSource(
        "COURSE_LIKED, LIKE",
        "COURSE_COMMENTED, COMMENT",
        "USER_FOLLOWED, FOLLOW",
        "COURSE_REVIEWED, REVIEW",
    )
    fun `활동 종류는 정해진 알림 설정에 연결된다`(
        type: NotificationType,
        category: NotificationCategory,
    ) {
        assertThat(type.category).isEqualTo(category)
    }

    @Test
    fun `운영 알림은 활동 알림을 모두 꺼도 보낸다`() {
        val allOff = NotificationSettings(false, false, false, false)

        assertThat(NotificationType.entries.filter(allOff::allows))
            .containsExactly(NotificationType.COURSE_HIDDEN_BY_ADMIN, NotificationType.COURSE_DELETED_BY_ADMIN)
    }

    // 운영 알림(SYSTEM)은 끄는 설정이 없어 제외한다(위 테스트가 담당).
    @ParameterizedTest
    @EnumSource(NotificationCategory::class, names = ["SYSTEM"], mode = EnumSource.Mode.EXCLUDE)
    fun `끄는 설정에 속한 활동만 차단한다`(category: NotificationCategory) {
        val settings =
            NotificationSettings(
                activityLike = category != NotificationCategory.LIKE,
                activityComment = category != NotificationCategory.COMMENT,
                activityFollow = category != NotificationCategory.FOLLOW,
                activityReview = category != NotificationCategory.REVIEW,
            )

        assertThat(NotificationType.entries.filter(settings::allows))
            .containsExactlyElementsOf(NotificationType.entries.filter { it.category != category })
    }
}
