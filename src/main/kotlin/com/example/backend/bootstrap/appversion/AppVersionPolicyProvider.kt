package com.example.backend.bootstrap.appversion

interface AppVersionPolicyProvider {
    fun minBuild(
        feature: String,
        platform: AppPlatform,
    ): Int?
}
