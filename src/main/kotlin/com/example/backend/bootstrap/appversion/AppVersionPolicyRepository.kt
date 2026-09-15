package com.example.backend.bootstrap.appversion

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class AppVersionPolicyRepository {
    private val log = KotlinLogging.logger {}

    @Transactional(readOnly = true)
    fun loadAll(): Map<Pair<AppFeature, AppPlatform>, Int> =
        AppVersionPolicyTable
            .selectAll()
            .mapNotNull { row ->
                val feature = AppFeature.fromKey(row[AppVersionPolicyTable.feature])
                val platform = AppPlatform.fromHeader(row[AppVersionPolicyTable.platform])
                if (feature == null || platform == null) {
                    log.warn {
                        "미정의 앱 버전 정책 행 무시: feature=${row[AppVersionPolicyTable.feature]}, " +
                            "platform=${row[AppVersionPolicyTable.platform]}"
                    }
                    return@mapNotNull null
                }
                (feature to platform) to row[AppVersionPolicyTable.minBuild]
            }.toMap()
}
