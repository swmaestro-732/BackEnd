package com.example.backend.bootstrap.appversion

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class RequiresAppFeature(
    val value: String,
)
