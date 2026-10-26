package com.example.backend.bootstrap.persistence

import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.sql.SQLException
import javax.sql.DataSource

class CrawlImportsPersistenceTest
    @Autowired
    constructor(
        private val dataSource: DataSource,
    ) : IntegrationTestBase() {
        @Test
        fun `크롤러 뷰는 재시도와 이미지 보완 대상을 구분하고 활성 장소 중복을 막는다`() {
            dataSource.connection.use { connection ->
                connection.autoCommit = false
                try {
                    connection.createStatement().use { statement ->
                        statement.execute(
                            """
                            INSERT INTO public.crawl_imports (shortcode, run_id, status) VALUES
                                ('scrum-694-imported', 'scrum-694-test', 'IMPORTED'),
                                ('scrum-694-rejected', 'scrum-694-test', 'REJECTED'),
                                ('scrum-694-failed', 'scrum-694-test', 'FAILED')
                            """.trimIndent(),
                        )
                        statement.execute(
                            """
                            INSERT INTO public.places (id, name, category, location, address, kakao_place_id, image_url, deleted_at)
                            SELECT id, '크롤러 테스트 장소', 'CAFE', ST_GeogFromText('SRID=4326;POINT(127 37.5)'), '서울',
                                   kakao_place_id, image_url, deleted_at
                            FROM (VALUES
                                (-6941, 'scrum-694-image', 'https://example.com/image.jpg', NULL::timestamptz),
                                (-6942, 'scrum-694-empty', '', NULL::timestamptz),
                                (-6943, 'scrum-694-null', NULL, NULL::timestamptz),
                                (-6944, 'scrum-694-null', 'https://example.com/deleted.jpg', now()),
                                (-6945, NULL, NULL, NULL::timestamptz),
                                (-6946, NULL, NULL, NULL::timestamptz)
                            ) AS fixture(id, kakao_place_id, image_url, deleted_at)
                            """.trimIndent(),
                        )
                        statement.execute("SET LOCAL ROLE crawler_ro")
                        statement
                            .executeQuery(
                                "SELECT shortcode FROM public.crawler_known_posts WHERE shortcode LIKE 'scrum-694-%' ORDER BY shortcode",
                            ).use { rows ->
                                val shortcodes = buildList { while (rows.next()) add(rows.getString("shortcode")) }
                                assertEquals(listOf("scrum-694-imported", "scrum-694-rejected"), shortcodes)
                            }
                        statement
                            .executeQuery(
                                "SELECT kakao_place_id, has_image FROM public.crawler_known_places " +
                                    "WHERE kakao_place_id LIKE 'scrum-694-%' OR kakao_place_id IS NULL ORDER BY kakao_place_id",
                            ).use { rows ->
                                val places =
                                    buildList {
                                        while (rows.next()) {
                                            add(rows.getString("kakao_place_id") to rows.getObject("has_image"))
                                        }
                                    }
                                assertEquals(
                                    listOf(
                                        "scrum-694-empty" to false,
                                        "scrum-694-image" to true,
                                        "scrum-694-null" to false,
                                    ),
                                    places,
                                )
                            }
                        statement.execute("RESET ROLE")
                        val duplicate =
                            assertThrows(SQLException::class.java) {
                                statement.execute(
                                    """
                                    INSERT INTO public.places (id, name, category, location, address, kakao_place_id)
                                    SELECT -6947, name, category, location, address, kakao_place_id FROM public.places WHERE id = -6941
                                    """.trimIndent(),
                                )
                            }
                        assertEquals("23505", duplicate.sqlState)
                    }
                } finally {
                    connection.rollback()
                }
            }
        }

        @Test
        fun `V18 중복 정리는 가장 오래된 장소로 참조를 옮기고 나머지를 소프트 삭제한다`() {
            // 운영에 있던 활성 중복을 재현하려면 인덱스를 잠시 내려야 해서, 같은 트랜잭션 안에서 내리고 롤백한다.
            // 남는 쪽은 id 최소인 -6952, 정리 대상은 -6951 이다.
            val migration = javaClass.getResource("/db/migration/V18__crawl_imports.sql")!!.readText()
            val dedupSql =
                migration.substring(
                    migration.indexOf("CREATE TEMP TABLE place_dedup"),
                    migration.indexOf("-- 활성 장소만 카카오 ID로 중복 방지한다."),
                )
            dataSource.connection.use { connection ->
                connection.autoCommit = false
                try {
                    connection.createStatement().use { statement ->
                        statement.execute("DROP INDEX public.uq_places_kakao_place_id")
                        statement.execute(
                            """
                            INSERT INTO public.places (id, name, category, location, address, kakao_place_id)
                            SELECT id, '중복 장소', 'CAFE', ST_GeogFromText('SRID=4326;POINT(127 37.5)'), '서울', 'scrum-694-dup'
                            FROM (VALUES (-6951), (-6952)) AS fixture(id)
                            """.trimIndent(),
                        )
                        statement.execute(
                            """
                            INSERT INTO public.place_business_hours (id, place_id, day_of_week) VALUES
                                (-6951, -6951, 1), (-6952, -6951, 2), (-6953, -6952, 1)
                            """.trimIndent(),
                        )
                        statement.execute(dedupSql)
                        statement
                            .executeQuery(
                                "SELECT id, deleted_at IS NOT NULL FROM public.places WHERE id IN (-6951, -6952) ORDER BY id DESC",
                            ).use { rows ->
                                val deleted =
                                    buildList { while (rows.next()) add(rows.getLong(1) to rows.getBoolean(2)) }
                                assertEquals(listOf(-6951L to true, -6952L to false), deleted)
                            }
                        statement
                            .executeQuery(
                                "SELECT place_id, day_of_week FROM public.place_business_hours " +
                                    "WHERE id IN (-6951, -6952, -6953) ORDER BY day_of_week",
                            ).use { rows ->
                                val hours = buildList { while (rows.next()) add(rows.getLong(1) to rows.getInt(2)) }
                                assertEquals(listOf(-6952L to 1, -6952L to 2), hours)
                            }
                        val createIndex =
                            migration
                                .substring(migration.indexOf("CREATE UNIQUE INDEX uq_places_kakao_place_id"))
                                .substringBefore(";")
                        statement.execute(createIndex)
                    }
                } finally {
                    connection.rollback()
                }
            }
        }
    }
