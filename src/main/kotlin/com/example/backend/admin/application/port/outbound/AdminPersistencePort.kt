package com.example.backend.admin.application.port.outbound

interface AdminPersistencePort {
    fun exists(email: String): Boolean
}
