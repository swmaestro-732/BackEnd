package com.example.backend.admin.application.port.outbound

interface AdminPersistencePort {
    fun exists(email: String): Boolean

    /**
     * [email] 어드민을 구글 계정 [googleSub] 에 묶는다. 아직 묶이지 않았으면 지금 묶고, 이미 같은 계정이면 그대로 둔다.
     * 어드민이 아니거나 다른 구글 계정에 묶여 있으면 false. 한 문장 UPDATE 라 동시 첫 로그인도 한 계정만 묶인다.
     */
    fun bindGoogleSub(
        email: String,
        googleSub: String,
    ): Boolean
}
