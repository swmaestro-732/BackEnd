package com.example.backend.common.appversion

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class RequiresAppFeature(
    val value: AppFeature,
)
