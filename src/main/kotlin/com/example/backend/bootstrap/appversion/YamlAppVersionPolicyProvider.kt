package com.example.backend.bootstrap.appversion

import org.springframework.stereotype.Component

@Component
class YamlAppVersionPolicyProvider(
    private val properties: AppVersionProperties,
) : AppVersionPolicyProvider {
    override fun minBuild(
        feature: String,
        platform: AppPlatform,
    ): Int? {
        val policy = properties.features[feature] ?: return null
        return when (platform) {
            AppPlatform.ANDROID -> policy.androidVersion
            AppPlatform.IOS -> policy.iosVersion
        }
    }
}
