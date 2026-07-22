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
        assertNull(user.socialProvider)
        assertNull(user.socialId)
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
    fun `createWithSocial 은 핸들을 포함한 소셜 User 를 생성한다`() {
        val user =
            User.createWithSocial(
                nickname = "hello",
                profileImageUrl = "https://example.com/profile.jpg",
                socialProvider = SocialProvider.KAKAO,
                socialId = "social-id",
                handle = "hello_handle",
            )

        assertNull(user.id)
        assertEquals("hello", user.nickname)
        assertEquals("hello_handle", user.handle)
        assertEquals("https://example.com/profile.jpg", user.profileImageUrl)
        assertEquals(SocialProvider.KAKAO, user.socialProvider)
        assertEquals("social-id", user.socialId)
    }

    @Test
    fun `createWithSocial 은 핸들 없이 소셜 User 를 생성한다`() {
        val user =
            User.createWithSocial(
                nickname = "hello",
                profileImageUrl = null,
                socialProvider = SocialProvider.APPLE,
                socialId = "social-id",
            )

        assertNull(user.handle)
        assertEquals(SocialProvider.APPLE, user.socialProvider)
        assertEquals("social-id", user.socialId)
    }

    @Test
    fun `createWithSocial 은 닉네임이 비면 예외를 던진다`() {
        assertThrows<IllegalArgumentException> {
            User.createWithSocial(" ", null, SocialProvider.KAKAO, "social-id")
        }
    }

    @Test
    fun `createWithSocial 은 닉네임이 최대 길이를 넘으면 예외를 던진다`() {
        val tooLong = "a".repeat(User.MAX_NICKNAME_LENGTH + 1)

        assertThrows<IllegalArgumentException> {
            User.createWithSocial(tooLong, null, SocialProvider.KAKAO, "social-id")
        }
    }

    @Test
    fun `createWithSocial 은 핸들이 비면 예외를 던진다`() {
        assertThrows<IllegalArgumentException> {
            User.createWithSocial("hello", null, SocialProvider.KAKAO, "social-id", " ")
        }
    }

    @Test
    fun `createWithSocial 은 핸들이 최대 길이를 넘으면 예외를 던진다`() {
        val tooLong = "a".repeat(User.MAX_HANDLE_LENGTH + 1)

        assertThrows<IllegalArgumentException> {
            User.createWithSocial("hello", null, SocialProvider.KAKAO, "social-id", tooLong)
        }
    }

    @Test
    fun `createWithSocial 은 소셜 식별자가 비면 예외를 던진다`() {
        assertThrows<IllegalArgumentException> {
            User.createWithSocial("hello", null, SocialProvider.KAKAO, " ")
        }
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

    @Test
    fun `reconstitute 는 소셜 제공자만 있으면 예외를 던진다`() {
        assertThrows<IllegalArgumentException> {
            User.reconstitute(id = 1, nickname = "hello", socialProvider = SocialProvider.GOOGLE)
        }
    }

    @Test
    fun `reconstitute 는 소셜 식별자만 있으면 예외를 던진다`() {
        assertThrows<IllegalArgumentException> {
            User.reconstitute(id = 1, nickname = "hello", socialId = "social-id")
        }
    }

    @Test
    fun `reconstitute 는 소셜 정보가 모두 null 이면 복원한다`() {
        val user = User.reconstitute(id = 1, nickname = "hello")

        assertNull(user.socialProvider)
        assertNull(user.socialId)
    }

    @Test
    fun `reconstitute 는 소셜 정보가 모두 있으면 복원한다`() {
        val user =
            User.reconstitute(
                id = 1,
                nickname = "hello",
                socialProvider = SocialProvider.GOOGLE,
                socialId = "social-id",
            )

        assertEquals(SocialProvider.GOOGLE, user.socialProvider)
        assertEquals("social-id", user.socialId)
    }

    private fun existingUser() =
        User.reconstitute(
            id = 1,
            nickname = "기존닉네임",
            handle = "original_handle",
            profileImageUrl = "https://example.com/original.jpg",
        )
}
