package com.example.backend.user.domain.model

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 도메인 단위 테스트 — Spring 컨텍스트/DB 없이 순수하게 불변식을 검증한다.
 * (헥사고날에서 도메인이 프레임워크에 의존하지 않는 이점.)
 */
class UserTest {
    @Test
    fun `create 는 신규 User 를 id 없이 생성한다`() {
        val user = User.create("hello")

        assertNull(user.id)
        assertEquals("hello", user.nickname)
        assertNull(user.handle)
        assertNull(user.profileImageUrl)
        assertNull(user.bio)
        assertEquals(UserStatus.ACTIVE, user.status)
    }

    @Test
    fun `create 는 닉네임이 비면 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { User.create(" ") }
    }

    @Test
    fun `create 는 닉네임이 최대 길이를 넘으면 예외를 던진다`() {
        val tooLong = "a".repeat(User.MAX_NICKNAME_LENGTH + 1)

        assertThrows<IllegalArgumentException> { User.create(tooLong) }
    }

    @Test
    fun `create 는 핸들과 프로필을 포함해 생성한다`() {
        val user =
            User.create(
                nickname = "hello",
                handle = "hello_handle",
                profileImageUrl = "https://example.com/profile.jpg",
            )

        assertNull(user.id)
        assertEquals("hello", user.nickname)
        assertEquals("hello_handle", user.handle)
        assertEquals("https://example.com/profile.jpg", user.profileImageUrl)
        assertEquals(UserStatus.ACTIVE, user.status)
    }

    @Test
    fun `create 는 핸들이 비면 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { User.create("hello", handle = " ") }
    }

    @Test
    fun `create 는 핸들이 최대 길이를 넘으면 예외를 던진다`() {
        val tooLong = "a".repeat(User.MAX_HANDLE_LENGTH + 1)

        assertThrows<IllegalArgumentException> { User.create("hello", handle = tooLong) }
    }

    @Test
    fun `withdraw 는 활성 사용자를 탈퇴 상태로 전이한다`() {
        val withdrawn = existingUser().withdraw()

        assertEquals(UserStatus.WITHDRAWN, withdrawn.status)
    }

    @Test
    fun `withdraw 는 이미 탈퇴한 사용자면 예외를 던진다`() {
        val user =
            User.reconstitute(
                id = 1,
                nickname = "탈퇴사용자",
                status = UserStatus.WITHDRAWN,
            )

        assertThrows<IllegalArgumentException> { user.withdraw() }
    }

    @Test
    fun `updateProfile 은 닉네임만 변경하고 핸들과 프로필 이미지를 유지한다`() {
        val user = existingUser()

        val updated = user.updateProfile(nickname = "새닉네임")

        assertEquals("새닉네임", updated.nickname)
        assertEquals(user.handle, updated.handle)
        assertEquals(user.profileImageUrl, updated.profileImageUrl)
    }

    @Test
    fun `updateProfile 은 핸들만 변경한다`() {
        val user = existingUser()

        val updated = user.updateProfile(handle = "new_handle")

        assertEquals(user.nickname, updated.nickname)
        assertEquals("new_handle", updated.handle)
        assertEquals(user.profileImageUrl, updated.profileImageUrl)
    }

    @Test
    fun `updateProfile 은 프로필 이미지만 변경한다`() {
        val user = existingUser()

        val updated = user.updateProfile(profileImageUrl = "https://example.com/new.jpg")

        assertEquals(user.nickname, updated.nickname)
        assertEquals(user.handle, updated.handle)
        assertEquals("https://example.com/new.jpg", updated.profileImageUrl)
    }

    @Test
    fun `updateProfile 은 bio만 변경한다`() {
        val user = existingUser()

        val updated = user.updateProfile(bio = "새 자기소개")

        assertEquals(user.nickname, updated.nickname)
        assertEquals(user.handle, updated.handle)
        assertEquals(user.profileImageUrl, updated.profileImageUrl)
        assertEquals("새 자기소개", updated.bio)
    }

    @Test
    fun `updateProfile 은 모든 값이 null 이면 원본을 유지한다`() {
        val user = existingUser()

        assertEquals(user, user.updateProfile())
    }

    @Test
    fun `updateProfile 은 갱신 후 닉네임이 비면 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { existingUser().updateProfile(nickname = " ") }
    }

    @Test
    fun `updateProfile 은 갱신 후 닉네임이 최대 길이를 넘으면 예외를 던진다`() {
        val tooLong = "a".repeat(User.MAX_NICKNAME_LENGTH + 1)

        assertThrows<IllegalArgumentException> { existingUser().updateProfile(nickname = tooLong) }
    }

    @Test
    fun `updateProfile 은 갱신 후 핸들이 비면 예외를 던진다`() {
        assertThrows<IllegalArgumentException> { existingUser().updateProfile(handle = " ") }
    }

    @Test
    fun `updateProfile 은 갱신 후 핸들이 최대 길이를 넘으면 예외를 던진다`() {
        val tooLong = "a".repeat(User.MAX_HANDLE_LENGTH + 1)

        assertThrows<IllegalArgumentException> { existingUser().updateProfile(handle = tooLong) }
    }

    private fun existingUser() =
        User.reconstitute(
            id = 1,
            nickname = "기존닉네임",
            handle = "original_handle",
            profileImageUrl = "https://example.com/original.jpg",
            bio = "기존 자기소개",
        )
}
