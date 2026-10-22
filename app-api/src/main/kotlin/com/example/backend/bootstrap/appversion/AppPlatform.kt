package com.example.backend.bootstrap.appversion

enum class AppPlatform {
    ANDROID,
    IOS,
    ;

    companion object {
        fun fromHeader(value: String): AppPlatform? = entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}
