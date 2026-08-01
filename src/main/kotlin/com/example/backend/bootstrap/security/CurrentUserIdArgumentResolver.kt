package com.example.backend.bootstrap.security

import org.springframework.core.MethodParameter
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentUserIdArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentUserId::class.java) &&
            (
                parameter.parameterType == Long::class.javaObjectType ||
                    parameter.parameterType == Long::class.javaPrimitiveType
            )

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? {
        val authentication = SecurityContextHolder.getContext().authentication
        val userId =
            (authentication as? JwtAuthenticationToken)
                ?.let { it.principal as? Jwt }
                ?.subject
                ?.toLong()

        // 필수(non-null) 파라미터인데 인증이 없으면 500 대신 401 로 응답하도록 인증 예외를 던진다.
        // (경로 매칭 대신 @AccessTokenRequired 메서드 시큐리티로 보호하므로, 익명 요청이 컨트롤러까지 도달한다.)
        if (userId == null && !parameter.isOptional) {
            throw AuthenticationCredentialsNotFoundException("인증이 필요합니다.")
        }
        return userId
    }
}
