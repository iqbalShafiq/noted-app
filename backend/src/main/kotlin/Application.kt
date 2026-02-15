package id.usecase

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module(
    appServices: AppServices = createConfiguredServices(),
) {
    configureSerialization()
    configureSecurity(jwtService = appServices.jwtService)
    configureRouting(
        noteSharingService = appServices.noteSharingService,
        noteSyncService = appServices.noteSyncService,
        authService = appServices.authService,
    )
}
