package com.example.backend.bootstrap.appversion

enum class AppPlatform {
    ANDROID,
    IOS,
    ;

    companion object {
        fun fromUserAgent(userAgent: String?): AppPlatform {
            val ua = userAgent?.lowercase() ?: return ANDROID
            return if ("iphone" in ua || "ipad" in ua) IOS else ANDROID
        }
    }
}
