package id.usecase.backend.service.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.config.ApplicationConfig
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

data class JwtConfig(
    val issuer: String,
    val audience: String,
    val realm: String,
    val secret: String,
    val expiresInSeconds: Long,
)

class JwtService(
    private val config: JwtConfig,
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    private val algorithm: Algorithm = Algorithm.HMAC256(config.secret)
    val expiresInSeconds: Long = config.expiresInSeconds

    fun issueToken(userId: String, username: String): String {
        val issuedAt = nowProvider()
        val expiresAt = issuedAt.plus(config.expiresInSeconds, ChronoUnit.SECONDS)

        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId)
            .withClaim("username", username)
            .withIssuedAt(Date.from(issuedAt))
            .withExpiresAt(Date.from(expiresAt))
            .sign(algorithm)
    }

    fun verifier(): JWTVerifier {
        return JWT.require(algorithm)
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .build()
    }

    fun realm(): String = config.realm
}

fun ApplicationConfig.toJwtConfig(): JwtConfig {
    return JwtConfig(
        issuer = readString("auth.jwt.issuer", "JWT_ISSUER", "noted-backend"),
        audience = readString("auth.jwt.audience", "JWT_AUDIENCE", "noted-app"),
        realm = readString("auth.jwt.realm", "JWT_REALM", "noted"),
        secret = readString("auth.jwt.secret", "JWT_SECRET", "noted-dev-super-secret-key-change-me"),
        expiresInSeconds = readLong("auth.jwt.expiresInSeconds", "JWT_EXPIRES_IN_SECONDS", 86_400L),
    )
}

private fun ApplicationConfig.readString(path: String, envKey: String, fallback: String): String {
    val envValue = System.getenv(envKey)?.trim().orEmpty()
    if (envValue.isNotBlank()) {
        return envValue
    }

    val configValue = propertyOrNull(path)?.getString()?.trim().orEmpty()
    if (configValue.isNotBlank()) {
        return configValue
    }

    return fallback
}

private fun ApplicationConfig.readLong(path: String, envKey: String, fallback: Long): Long {
    val envValue = System.getenv(envKey)?.trim()?.toLongOrNull()
    if (envValue != null) {
        return envValue
    }

    val configValue = propertyOrNull(path)?.getString()?.trim()?.toLongOrNull()
    if (configValue != null) {
        return configValue
    }

    return fallback
}
