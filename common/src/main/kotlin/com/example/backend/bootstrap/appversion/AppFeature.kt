package com.example.backend.bootstrap.appversion

enum class AppFeature(
    val key: String,
) {
    AUTH("auth"),
    USER_PROFILE("user-profile"),
    USER_COURSE("user-course"),
    USER_PLACE("user-place"),
    COURSE_DETAIL("course-detail"),
    COURSE_CREATE("course-create"),
    COURSE_REVIEW("course-review"),
    PLAN("plan"),
    PLACE_DETAIL("place-detail"),
    PLACE_SEARCH("place-search"),
    PLACE_REVIEW("place-review"),
    MEDIA("media"),
    AREA("area"),
    ;

    companion object {
        private val byKey = entries.associateBy { it.key }

        fun fromKey(key: String): AppFeature? = byKey[key]
    }
}
