package com.example.backend.user.application.service

import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.application.port.inbound.UserUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.jdbc.Sql

/**
 * 회원 탈퇴 시 유저 소유 데이터가 전부 정리되고 상대방 카운터까지 보정되는지 검증한다.
 * 재가입(reactivate)이 "처음 계정처럼" 시작할 수 있는 상태가 되는 게 목표다.
 *
 * 시드: A(1, 탈퇴 대상)·B(2)·C(3).
 *  - A→B, B→A 상호 팔로우.  - A 가 공개 코스 2개 작성(course 1·2).  - C 가 코스 작성(course 3), A 가 이를 저장.
 *  - A 는 관심 테마 2개·관심 지역 1개·bio·비영(非零) 카운터를 갖는다.
 */
@Sql(
    statements = [
        "TRUNCATE TABLE follows, saved_courses, saved_course_folders, user_like_categories, " +
            "user_areas, courses, users RESTART IDENTITY CASCADE",
        // A(1) — 탈퇴 대상. 카운터를 실제 관계/코스 수와 맞춰 preset.
        "INSERT INTO users (nickname, handle, bio, profile_image_url, status, social_provider, social_id, " +
            "followers_cnt, followings_cnt, public_courses_cnt, follower_courses_cnt, private_courses_cnt) " +
            "VALUES ('탈퇴대상', 'a_handle', '내 소개입니다', 'https://cdn/a.jpg', 0, 'KAKAO', 'a-social', 1, 1, 2, 0, 0)",
        // B(2) — A 와 상호 팔로우.
        "INSERT INTO users (nickname, handle, status, followers_cnt, followings_cnt) " +
            "VALUES ('유저비', 'b_handle', 0, 1, 1)",
        // C(3) — A 가 저장한 코스의 작성자.
        "INSERT INTO users (nickname, handle, status) VALUES ('유저씨', 'c_handle', 0)",
        "INSERT INTO follows (follower_id, following_id) VALUES (1, 2), (2, 1)",
        "INSERT INTO courses (user_id, title, is_published, visibility, created_at) " +
            "VALUES (1, 'A코스1', true, 'PUBLIC', now())",
        "INSERT INTO courses (user_id, title, is_published, visibility, created_at) " +
            "VALUES (1, 'A코스2', true, 'PUBLIC', now())",
        "INSERT INTO courses (user_id, title, is_published, visibility, saves_cnt, created_at) " +
            "VALUES (3, 'C코스', true, 'PUBLIC', 1, now())",
        "INSERT INTO saved_courses (user_id, course_id, created_at) VALUES (1, 3, now())",
        "INSERT INTO user_like_categories (user_id, category) VALUES (1, 'CAFETOUR'), (1, 'DATE')",
        "INSERT INTO user_areas (user_id, area_code, updated_at) VALUES (1, '1168010100', now())",
    ],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
class WithdrawPurgeIntegrationTest
    @Autowired
    constructor(
        private val userUseCase: UserUseCase,
        private val jdbc: JdbcTemplate,
    ) : IntegrationTestBase() {
        @Test
        fun `탈퇴하면 소유 데이터가 전부 정리되고 상대방 카운터가 보정된다`() {
            userUseCase.withdraw(ME)

            // 1) 팔로우 — A 가 얽힌 양방향 관계가 전부 사라진다(여기선 그게 전부라 0행).
            assertEquals(0L, count("SELECT count(*) FROM follows"))
            // 상대(B) 카운터 보정: A 가 B 를 언팔(B.followers −1), B→A 삭제(B.followings −1).
            assertEquals(0, intOf("SELECT followers_cnt FROM users WHERE id = 2"))
            assertEquals(0, intOf("SELECT followings_cnt FROM users WHERE id = 2"))

            // 2) 작성 코스 — A 코스는 전부 소프트 삭제, 남의 코스(C)는 그대로.
            assertEquals(0L, count("SELECT count(*) FROM courses WHERE user_id = 1 AND deleted_at IS NULL"))
            assertNull(jdbc.queryForObject("SELECT deleted_at FROM courses WHERE id = 3", Any::class.java))

            // 3) 저장 코스 — A 의 저장이 사라지고 원저자(C) 코스 saves_cnt 가 보정된다.
            assertEquals(0L, count("SELECT count(*) FROM saved_courses WHERE user_id = 1"))
            assertEquals(0, intOf("SELECT saves_cnt FROM courses WHERE id = 3"))

            // 4) 개인화 — 관심 테마·지역이 사라진다.
            assertEquals(0L, count("SELECT count(*) FROM user_like_categories WHERE user_id = 1"))
            assertEquals(0L, count("SELECT count(*) FROM user_areas WHERE user_id = 1"))

            // 5) users 행 — 탈퇴 스탬프 + 핸들 해제 + bio·카운터 리셋.
            assertEquals(3, intOf("SELECT status FROM users WHERE id = 1")) // WITHDRAWN
            assertEquals(1L, count("SELECT count(*) FROM users WHERE id = 1 AND deleted_at IS NOT NULL"))
            assertNull(jdbc.queryForObject("SELECT bio FROM users WHERE id = 1", String::class.java))
            assertNull(jdbc.queryForObject("SELECT profile_image_url FROM users WHERE id = 1", String::class.java))
            assertNull(jdbc.queryForObject("SELECT handle FROM users WHERE id = 1", String::class.java))
            assertEquals(0, intOf("SELECT followers_cnt FROM users WHERE id = 1"))
            assertEquals(0, intOf("SELECT followings_cnt FROM users WHERE id = 1"))
            assertEquals(0, intOf("SELECT public_courses_cnt FROM users WHERE id = 1"))
            assertEquals(0, intOf("SELECT follower_courses_cnt FROM users WHERE id = 1"))
            assertEquals(0, intOf("SELECT private_courses_cnt FROM users WHERE id = 1"))
        }

        private fun count(sql: String): Long = jdbc.queryForObject(sql, Long::class.java) ?: 0L

        private fun intOf(sql: String): Int = jdbc.queryForObject(sql, Int::class.java) ?: -1

        private companion object {
            const val ME = 1L
        }
    }
