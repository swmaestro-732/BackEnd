package com.example.backend.user.application.service

import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * 오늘 이미 쓰기를 넘긴 기기 활동을 인스턴스 메모리에 기억한다(SCRUM-783). [DeviceActivityService] 안에서만 쓴다.
 *
 * 쓰기 횟수를 줄이는 캐시일 뿐이고 정확성은 PK 와 ON CONFLICT 가 지킨다. 인스턴스가 여러 대면 대마다 한 번씩 쓰지만 한 행으로 합쳐진다.
 * 날짜가 바뀌면 새로 시작한다. 헤더는 비로그인 요청도 보내 무작위 ID 로 끝없이 커질 수 있어 [MAX_SIZE] 를 넘으면 비운다
 * (비운 뒤에는 이미 기록한 기기도 한 번 더 쓰지만 ON CONFLICT 로 끝난다).
 */
internal class RecordedDeviceActivities {
    // 여러 요청 스레드가 바꾸므로 @Volatile 로 새 Day 를 바로 보게 한다. 처음엔 LocalDate.MIN 이라 첫 요청에서 오늘로 바뀐다.
    @Volatile
    private var day = Day(LocalDate.MIN)

    /** 처음 보는 활동이면 기억하고 true. */
    fun markNew(activity: DeviceActivity): Boolean = keysOn(activity.date).add(activity.key)

    /** 쓰지 못한 활동을 잊어 다음 요청에 다시 쓰게 한다. 그새 날짜가 바뀌었으면 이미 새 날이라 할 일이 없다. */
    fun forget(activity: DeviceActivity) {
        day.takeIf { it.date == activity.date }?.keys?.remove(activity.key)
    }

    private fun keysOn(date: LocalDate): MutableSet<DeviceActivity.Key> {
        val current = day
        if (current.date == date && current.keys.size < MAX_SIZE) return current.keys
        return Day(date).also { day = it }.keys
    }

    private class Day(
        val date: LocalDate,
    ) {
        val keys: MutableSet<DeviceActivity.Key> = ConcurrentHashMap.newKeySet()
    }

    private companion object {
        const val MAX_SIZE = 100_000
    }
}
