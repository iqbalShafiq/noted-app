package id.usecase

import io.ktor.server.application.*
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
    val noteSyncService by inject<NoteSyncService>()
    val authService by inject<AuthService>()

    configureSerialization()
    configureSecurity(jwtService = jwtService)
    configureRouting(
        noteSharingService = noteSharingService,
        noteSyncService = noteSyncService,
        authService = authService,
    )
}
