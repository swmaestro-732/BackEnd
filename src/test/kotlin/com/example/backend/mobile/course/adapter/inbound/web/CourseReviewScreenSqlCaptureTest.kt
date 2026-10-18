package com.example.backend.mobile.course.adapter.inbound.web

import com.example.backend.support.IntegrationTestBase
import com.example.backend.support.SqlCapture
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import java.io.File

/**
 * 코스 후기 전체보기 BFF 가 실제 DB 로 보내는 SQL·바인딩 값을 수집하고(SCRUM-499 인덱스 설계 근거),
 * 공개(PUBLISHED)·숨김(HIDDEN)·삭제(DELETED) 리뷰가 목록·통계에 어떻게 반영되는지 확인한다.
 * 수집한 SQL 은 build/sql-capture/course-review-screen.md 에 남긴다.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/course-review-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseReviewScreenSqlCaptureTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jdbcTemplate: JdbcTemplate,
        private val jsonMapper: JsonMapper,
    ) : IntegrationTestBase() {
        @AfterEach
        fun cleanUpAuthors() {
            jdbcTemplate.update("DELETE FROM users WHERE id BETWEEN 9701 AND 9706")
        }

        @Test
        fun `최신순·평점순 첫 페이지와 커서 다음 페이지가 보내는 SQL 을 수집한다`() {
            seedReviews()
            val report = StringBuilder("# 코스 후기 전체보기 — 실제 실행 SQL (size=2)\n")

            val (latestFirst, latestFirstSql) = capture("sort=LATEST&order=DESC&size=2")
            assertEquals(listOf(4, 3), latestFirst.ids())
            assertScreenStatements(latestFirstSql, orderBy = "created_at DESC, course_reviews.id DESC")
            assertTrue(latestFirstSql[0].sql.contains("course_reviews.created_at < ?").not())
            report.section("최신순 첫 페이지", latestFirstSql)

            val (latestNext, latestNextSql) =
                capture(
                    "sort=LATEST&order=DESC&size=2&cursor=${latestFirst.nextCursor()}",
                )
            assertEquals(listOf(2, 1), latestNext.ids())
            assertScreenStatements(latestNextSql, orderBy = "created_at DESC, course_reviews.id DESC")
            assertTrue(
                latestNextSql[0].sql.contains(
                    "(course_reviews.created_at < ?) OR ((course_reviews.created_at = ?) AND (course_reviews.id < ?))",
                ),
            )
            assertTrue(latestNextSql[0].args.contains("3")) // 커서 = 첫 페이지 마지막 행(id 3)
            report.section("최신순 다음 페이지 (커서)", latestNextSql)

            val (ratingFirst, ratingFirstSql) = capture("sort=RATING&order=DESC&size=2")
            assertEquals(listOf(4, 1), ratingFirst.ids()) // 5점 두 건, 최신(4) → 오래된(1)
            assertScreenStatements(
                ratingFirstSql,
                orderBy = "rating DESC, course_reviews.created_at DESC, course_reviews.id DESC",
            )
            report.section("평점순 첫 페이지", ratingFirstSql)

            val (ratingNext, ratingNextSql) =
                capture(
                    "sort=RATING&order=DESC&size=2&cursor=${ratingFirst.nextCursor()}",
                )
            assertEquals(listOf(3, 2), ratingNext.ids()) // 4점(3) → 3점(2)
            assertScreenStatements(
                ratingNextSql,
                orderBy = "rating DESC, course_reviews.created_at DESC, course_reviews.id DESC",
            )
            assertTrue(ratingNextSql[0].sql.contains("(course_reviews.rating < ?) OR ((course_reviews.rating = ?) AND"))
            report.section("평점순 다음 페이지 (커서)", ratingNextSql)

            File("build/sql-capture").mkdirs()
            File("build/sql-capture/course-review-screen.md").writeText(report.toString())
        }

        @Test
        fun `통계(총 개수·평균·사진 수·별점 분포)는 집계하지 않고 courses 카운터를 읽는다`() {
            seedReviews()

            val (page, sql) = capture("size=10")

            // 목록: PUBLISHED 4건만, HIDDEN(5)·DELETED(6) 제외
            assertEquals(listOf(4, 3, 2, 1), page.ids())

            // 통계는 전부 courses 카운터 단건 조회에서 온다 — 집계(GROUP BY·JOIN COUNT) SQL 이 없다.
            // 카운터는 작성(+)·소프트 삭제(−)만 반영하므로 HIDDEN(id5, 2점, 사진 1장)은 남아 있다 → 총 5건, 19/5=3.8, 사진 4장, 2점 1건.
            assertEquals(5, page["totalCount"])
            assertEquals(3.8, page["averageRating"])
            assertEquals(4, page["photoCount"])
            assertEquals(mapOf(5 to 2, 4 to 1, 3 to 1, 2 to 1, 1 to 0), page.distribution())
            val counters = sql.single { it.sql.contains("courses.rating_sum") }
            assertTrue(
                counters.sql.contains("courses.review_photo_cnt") && counters.sql.contains("courses.rating_5_cnt"),
            )
            assertTrue(sql.none { it.sql.contains("GROUP BY") || it.sql.contains("COUNT(") })
        }

        // ── 검증 헬퍼 ──

        /** 화면 한 번 = 목록·사진·태그·카운터·작성자 5문. 목록 정렬은 [orderBy] 와 일치해야 한다. */
        private fun assertScreenStatements(
            sql: List<SqlCapture.Statement>,
            orderBy: String,
        ) {
            assertEquals(5, sql.size, sql.joinToString("\n") { it.sql })
            val list = sql[0]
            assertTrue(list.sql.startsWith("SELECT") && list.sql.contains("FROM course_reviews"))
            assertTrue(list.sql.contains("ORDER BY course_reviews.$orderBy LIMIT 3"), list.sql) // size 2 + hasNext 1
            assertTrue(list.sql.contains("course_reviews.deleted_at IS NULL"))
            assertEquals(listOf("701", "'PUBLISHED'"), list.args.take(2), list.args.toString()) // 커서 페이지는 뒤에 커서 값이 붙는다
            assertTrue(sql[1].sql.contains("FROM course_review_photos WHERE course_review_photos.course_review_id IN"))
            assertTrue(sql[1].sql.contains("ORDER BY course_review_photos.order_no ASC"))
            assertTrue(
                sql[2].sql.contains("FROM course_review_tag_links WHERE course_review_tag_links.course_review_id IN"),
            )
            assertTrue(sql[3].sql.contains("courses.rating_sum") && sql[3].sql.contains("courses.rating_5_cnt"))
            assertTrue(sql[4].sql.contains("FROM users WHERE"))
        }

        /** 단언보다 먼저 파일에 남겨, 실패해도 실제 SQL 을 볼 수 있게 한다. */
        private fun capture(query: String): Pair<Map<String, Any?>, List<SqlCapture.Statement>> {
            val captured =
                SqlCapture.record {
                    val body =
                        mockMvc
                            .perform(get("/service/v1/courses/$COURSE_ID/reviews?$query"))
                            .andExpect(status().isOk)
                            .andReturn()
                            .response
                            .contentAsString
                    @Suppress("UNCHECKED_CAST")
                    jsonMapper.readValue(body, Map::class.java)["data"] as Map<String, Any?>
                }
            dump.section("GET …/reviews?$query", captured.second)
            File("build/sql-capture").mkdirs()
            File("build/sql-capture/course-review-screen-raw.md").writeText(dump.toString())
            return captured
        }

        private val dump = StringBuilder("# 코스 후기 전체보기 — 실제 실행 SQL (원본 덤프)\n")

        private fun StringBuilder.section(
            title: String,
            sql: List<SqlCapture.Statement>,
        ) {
            append("\n## $title\n")
            sql.forEachIndexed { i, s ->
                append("\n${i + 1}. \n```sql\n${s.sql}\n```\n바인딩: ${s.args}\n")
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun Map<String, Any?>.ids(): List<Int> =
            (this["reviews"] as List<Map<String, Any?>>).map { (it["id"] as Number).toInt() }

        @Suppress("UNCHECKED_CAST")
        private fun Map<String, Any?>.distribution(): Map<Int, Int> =
            (this["ratingDistribution"] as List<Map<String, Any?>>).associate {
                (it["rating"] as Number).toInt() to (it["count"] as Number).toInt()
            }

        private fun Map<String, Any?>.nextCursor(): String = requireNotNull(this["nextCursor"] as String?)

        // ── 픽스처 ──

        /**
         * 코스 701 에 리뷰 6건. 작성일은 id 순으로 증가.
         *  id1 5점 PUBLISHED 사진2·태그2 / id2 3점 PUBLISHED 사진1 / id3 4점 PUBLISHED / id4 5점 PUBLISHED
         *  id5 2점 HIDDEN 사진1 (deleted_at NULL) / id6 1점 DELETED 사진1 (deleted_at 있음)
         * 카운터는 쓰기 경로가 남기는 값 그대로: 작성 6건 +20/+6/+5장, id6(1점) 소프트 삭제 −1/−1/−1장 → 19/5/4장,
         * 별점별 5점 2·4점 1·3점 1·2점 1·1점 0 (HIDDEN id5 는 차감되지 않아 2점 1건과 사진 1장이 남는다).
         */
        private fun seedReviews() {
            jdbcTemplate.update(
                "INSERT INTO users (id, nickname) VALUES (9701, 'sqlcap-A'), (9702, 'sqlcap-B'), (9703, 'sqlcap-C'), (9704, 'sqlcap-D'), (9705, 'sqlcap-E'), (9706, 'sqlcap-F') ON CONFLICT DO NOTHING",
            )
            jdbcTemplate.update(
                """
                INSERT INTO course_reviews (id, course_id, user_id, rating, content, status, created_at, updated_at, deleted_at) VALUES
                  (1, $COURSE_ID, 9701, 5, '최고예요', 'PUBLISHED', '2026-01-01T10:00:00Z', '2026-01-01T10:00:00Z', NULL),
                  (2, $COURSE_ID, 9702, 3, NULL,       'PUBLISHED', '2026-01-02T10:00:00Z', '2026-01-02T10:00:00Z', NULL),
                  (3, $COURSE_ID, 9703, 4, '좋아요',   'PUBLISHED', '2026-01-03T10:00:00Z', '2026-01-03T10:00:00Z', NULL),
                  (4, $COURSE_ID, 9704, 5, '또 올래요', 'PUBLISHED', '2026-01-04T10:00:00Z', '2026-01-04T10:00:00Z', NULL),
                  (5, $COURSE_ID, 9705, 2, '숨김',     'HIDDEN',    '2026-01-05T10:00:00Z', '2026-01-05T10:00:00Z', NULL),
                  (6, $COURSE_ID, 9706, 1, '삭제됨',   'DELETED',   '2026-01-06T10:00:00Z', '2026-01-06T10:00:00Z', '2026-01-07T10:00:00Z')
                """.trimIndent(),
            )
            jdbcTemplate.update(
                """
                INSERT INTO course_review_photos (course_review_id, image_url, order_no) VALUES
                  (1, 'https://cdn/p1.jpg', 0), (1, 'https://cdn/p2.jpg', 1), (2, 'https://cdn/p3.jpg', 0),
                  (5, 'https://cdn/hidden.jpg', 0), (6, 'https://cdn/deleted.jpg', 0)
                """.trimIndent(),
            )
            jdbcTemplate.update(
                "INSERT INTO course_review_tag_links (course_review_id, tag) VALUES (1, 'PACKED'), (1, 'SMOOTH')",
            )
            jdbcTemplate.update(
                "UPDATE courses SET rating_sum = 19, rating_cnt = 5, review_photo_cnt = 4, rating_5_cnt = 2, rating_4_cnt = 1, rating_3_cnt = 1, rating_2_cnt = 1 WHERE id = $COURSE_ID",
            )
        }

        private companion object {
            const val COURSE_ID = 701L
        }
    }
