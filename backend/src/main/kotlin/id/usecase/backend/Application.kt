package id.usecase.backend

import id.usecase.backend.auth.security.JwtService
import id.usecase.backend.di.configureDependencyInjection
import id.usecase.backend.domain.auth.AuthRepository
import id.usecase.backend.plugins.configureRouting
import id.usecase.backend.plugins.configureSecurity
import id.usecase.backend.plugins.configureSerialization
import id.usecase.backend.service.auth.AuthService
import id.usecase.backend.service.note.NoteHistoryService
import id.usecase.backend.service.note.NoteSharingService
import id.usecase.backend.service.sync.NoteSyncService
import io.ktor.server.application.Application
import org.koin.ktor.ext.inject

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module(
    storageModeOverride: String? = null,
) {
    configureDependencyInjection(storageModeOverride = storageModeOverride)

    val jwtService by inject<JwtService>()
    val noteSharingService by inject<NoteSharingService>()
    val noteHistoryService by inject<NoteHistoryService>()
    val noteSyncService by inject<NoteSyncService>()
    val authService by inject<AuthService>()
    val authRepository by inject<AuthRepository>()

    configureSerialization()
    configureSecurity(jwtService = jwtService)
    configureRouting(
        noteSharingService = noteSharingService,
        noteHistoryService = noteHistoryService,
        noteSyncService = noteSyncService,
        authService = authService,
        authRepository = authRepository,
    )
}
