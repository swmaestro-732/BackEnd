package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.domain.model.SocialProvider
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlMergeMode
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 코스 저장 폴더 컨트롤러(`POST /api/v1/folders` · `GET /api/v1/folders`) 통합 테스트.
 * 저장 코스와 픽스처(saved-course-fixture.sql)를 공유한다 — 폴더는 1·2 가 나(1) 소유(order_no 0·1), 3 이 타인(2) 소유다.
 *
 * 이름 유일성은 애플리케이션 선검사(409)와 유니크 인덱스(V2 마이그레이션)가 이중으로 막으므로 둘 다 확인하고,
 * order_no 상한(SMALLINT)은 픽스처 폴더의 order_no 를 상한까지 올려 두고 검증한다.
 */
@AutoConfigureMockMvc
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts = ["/sql/saved-course-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseFolderControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jdbcTemplate: JdbcTemplate,
    ) : IntegrationTestBase() {
        // ─────────────────────────── 폴더 생성 (POST /api/v1/folders) ───────────────────────────

        @Test
        fun `폴더를 만들면 201과 생성된 folderId 를 내려주고 목록 맨 뒤에 붙는다`() {
            mockMvc
                .perform(createFolderRequest("주말나들이"))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.code").value(2000))
                // 픽스처 폴더 id 는 1~3 — 새 폴더는 그보다 큰 id 를 받는다.
                .andExpect(jsonPath("$.data.folderId").value(greaterThan(FIXTURE_MAX_FOLDER_ID)))

            // order_no 는 기존 최대(1) + 1 이라 폴더 칩 맨 뒤에 붙는다.
            mockMvc
                .perform(listFoldersRequest(USER_ID))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.folderCount").value(3))
                .andExpect(jsonPath("$.data.folders[2].name").value("주말나들이"))
        }

        @Test
        fun `이름 앞뒤 공백은 잘라내고 저장한다`() {
            mockMvc
                .perform(createFolderRequest("  주말나들이  "))
                .andExpect(status().isCreated)

            mockMvc
                .perform(listFoldersRequest(USER_ID))
                .andExpect(jsonPath("$.data.folders[2].name").value("주말나들이"))
        }

        @Test
        fun `같은 이름의 폴더를 또 만들면 4095를 내려준다`() {
            mockMvc
                .perform(createFolderRequest(MY_FOLDER_NAME))
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4095))

            // 생성이 막혔으니 폴더 수는 그대로 2다.
            mockMvc
                .perform(listFoldersRequest(USER_ID))
                .andExpect(jsonPath("$.data.folderCount").value(2))
        }

        @Test
        fun `타인이 쓰는 이름이어도 내 폴더로는 만들 수 있다`() {
            mockMvc
                .perform(createFolderRequest(OTHERS_FOLDER_NAME))
                .andExpect(status().isCreated)

            mockMvc
                .perform(listFoldersRequest(USER_ID))
                .andExpect(jsonPath("$.data.folderCount").value(3))
                .andExpect(jsonPath("$.data.folders[2].name").value(OTHERS_FOLDER_NAME))
        }

        @Test
        fun `공백뿐인 이름은 검증 실패로 4002를 내려준다`() {
            mockMvc
                .perform(createFolderRequest("   "))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        @Test
        fun `10자를 넘는 이름은 검증 실패로 4002를 내려준다`() {
            mockMvc
                .perform(createFolderRequest("가".repeat(11)))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                // 실패 필드명은 요청 필드 그대로 name 이다(내부 rawName 이 새어나가지 않는다).
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
        }

        @Test
        fun `가운데 공백도 글자수에 센다 - 공백 포함 11자면 4002`() {
            // "가"*5 + " " + "가"*5 = 11자. 가운데 공백은 트림 대상이 아니라 길이에 포함된다.
            mockMvc
                .perform(createFolderRequest("가".repeat(5) + " " + "가".repeat(5)))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        @Test
        fun `길이 제한은 앞뒤 공백을 뺀 값 기준이다 - 공백 포함 12자여도 통과한다`() {
            // 화면 글자수 카운터가 트림 기준이라, 원본 길이로 재면 경계값에서 서버와 어긋난다.
            val name = "가".repeat(10)

            mockMvc
                .perform(createFolderRequest(" $name "))
                .andExpect(status().isCreated)

            mockMvc
                .perform(listFoldersRequest(USER_ID))
                .andExpect(jsonPath("$.data.folders[2].name").value(name))
        }

        @Test
        fun `가운데 띄어쓰기는 막지 않는다`() {
            mockMvc
                .perform(createFolderRequest("데이트 코스"))
                .andExpect(status().isCreated)

            mockMvc
                .perform(listFoldersRequest(USER_ID))
                .andExpect(jsonPath("$.data.folders[2].name").value("데이트 코스"))
        }

        @Test
        fun `폴더 생성은 토큰이 없으면 401을 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"name":"주말나들이"}"""),
                ).andExpect(status().isUnauthorized)
        }

        @Test
        fun `회원가입 토큰으로는 폴더를 만들 수 없다`() {
            val registrationToken = jwtTokenProvider.issueRegistrationToken(SocialProvider.KAKAO, "social-1")

            mockMvc
                .perform(
                    post("/api/v1/folders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $registrationToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"name":"주말나들이"}"""),
                ).andExpect(status().isForbidden)
        }

        @Test
        fun `생성 mock=true면 DB 저장 없이 목 folderId 를 내려준다`() {
            mockMvc
                .perform(createFolderRequest("주말나들이").param("mock", "true"))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.folderId").value(4))

            // 실제 생성은 일어나지 않아 폴더 수는 그대로 2다.
            mockMvc
                .perform(listFoldersRequest(USER_ID))
                .andExpect(jsonPath("$.data.folderCount").value(2))
        }

        /**
         * order_no 는 SMALLINT 라 다음 값이 [Short.MAX_VALUE] 를 넘으면 toShort() 가 음수로 래핑돼
         * 새 폴더가 폴더 칩 맨 앞으로 가버린다 — 정렬이 조용히 깨지는 대신 400 으로 드러나야 한다.
         */
        @Test
        @Sql(
            statements = ["UPDATE saved_course_folders SET order_no = 32767 WHERE user_id = 1 AND id = 2"],
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        )
        fun `폴더 순번이 SMALLINT 상한이면 400으로 막고 만들지 않는다`() {
            mockMvc
                .perform(createFolderRequest("주말나들이"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))

            mockMvc
                .perform(listFoldersRequest(USER_ID))
                .andExpect(jsonPath("$.data.folderCount").value(2))
        }

        // ─────────────────────────── 폴더 목록 조회 (GET /api/v1/folders) ───────────────────────────

        @Test
        fun `폴더 목록을 order_no 순으로 내려주고 타인 폴더는 빼놓는다`() {
            mockMvc
                .perform(listFoldersRequest(USER_ID))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.folderCount").value(2))
                .andExpect(jsonPath("$.data.folders.length()").value(2))
                .andExpect(jsonPath("$.data.folders[0].id").value(1))
                .andExpect(jsonPath("$.data.folders[0].name").value(MY_FOLDER_NAME))
                .andExpect(jsonPath("$.data.folders[1].id").value(2))

            // 타인(2)에게는 자기 폴더 1건만 보인다.
            mockMvc
                .perform(listFoldersRequest(OTHER_USER_ID))
                .andExpect(jsonPath("$.data.folderCount").value(1))
                .andExpect(jsonPath("$.data.folders[0].name").value(OTHERS_FOLDER_NAME))
        }

        @Test
        @Sql(
            statements = [
                "UPDATE saved_courses SET folder_id = NULL WHERE user_id = 1",
                "DELETE FROM saved_course_folders WHERE user_id = 1",
            ],
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        )
        fun `폴더가 하나도 없으면 빈 목록과 0을 내려준다`() {
            mockMvc
                .perform(listFoldersRequest(USER_ID))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.folderCount").value(0))
                .andExpect(jsonPath("$.data.folders.length()").value(0))
        }

        @Test
        fun `폴더 목록은 토큰이 없으면 401을 내려준다`() {
            mockMvc
                .perform(get("/api/v1/folders"))
                .andExpect(status().isUnauthorized)
        }

        @Test
        fun `목록 mock=true면 DB와 무관하게 목 폴더를 내려준다`() {
            mockMvc
                .perform(listFoldersRequest(USER_ID).param("mock", "true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.folderCount").value(3))
        }

        // ─────────────────────────── 이름 유일성 인덱스 (V2 마이그레이션) ───────────────────────────

        @Test
        fun `유니크 인덱스가 같은 사용자·같은 이름 폴더를 DB 단에서 막는다`() {
            // 애플리케이션 선검사를 건너뛴 경로(동시 생성 경합)를 흉내낸다 — 최종 방어선은 인덱스다.
            assertThrows<DuplicateKeyException> { insertFolderDirectly(USER_ID, MY_FOLDER_NAME) }
        }

        @Test
        fun `유니크 인덱스는 사용자별이라 다른 사용자의 같은 이름은 허용한다`() {
            insertFolderDirectly(OTHER_USER_ID, MY_FOLDER_NAME)

            mockMvc
                .perform(listFoldersRequest(OTHER_USER_ID))
                .andExpect(jsonPath("$.data.folderCount").value(2))
        }

        private fun insertFolderDirectly(
            userId: Long,
            name: String,
        ) = jdbcTemplate.update(
            "INSERT INTO saved_course_folders (user_id, name, order_no) VALUES (?, ?, ?)",
            userId,
            name,
            9,
        )

        private fun createFolderRequest(name: String) =
            post("/api/v1/folders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"$name"}""")

        private fun listFoldersRequest(userId: Long) =
            get("/api/v1/folders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(userId)}")

        private fun tokenFor(userId: Long) = jwtTokenProvider.issueAccessToken(userId)

        private companion object {
            const val USER_ID = 1L
            const val OTHER_USER_ID = 2L

            // 픽스처: 폴더 1·2 = USER_ID 소유(order_no 0·1), 폴더 3 = 타인 소유
            const val MY_FOLDER_NAME = "가고싶다"
            const val OTHERS_FOLDER_NAME = "타인폴더"
            const val FIXTURE_MAX_FOLDER_ID = 3
        }
    }
