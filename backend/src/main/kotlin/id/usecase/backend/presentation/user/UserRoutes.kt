package id.usecase.backend.presentation.user

import id.usecase.backend.domain.auth.AuthRepository
import id.usecase.backend.plugins.requireUserId
import id.usecase.noted.shared.user.GetUserProfileResponse
import id.usecase.noted.shared.user.UpdateProfileRequest
import id.usecase.noted.shared.user.UpdateProfileResponse
import id.usecase.noted.shared.user.UserProfileDto
import id.usecase.noted.shared.user.UserStatisticsDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.slf4j.LoggerFactory

fun Route.userRoutes(authRepository: AuthRepository) {
    val logger = LoggerFactory.getLogger("UserRoutes")

    route("/user") {
        authenticate("auth-jwt") {
            get("/profile") {
                try {
                    val userId = call.requireUserId()
                    logger.info("Fetching profile for userId={}", userId)

                    val user = authRepository.findById(userId)
                        ?: run {
                            logger.warn("User not found userId={}", userId)
                            call.respond(
                                HttpStatusCode.NotFound,
                                mapOf("error" to "User not found")
                            )
                            return@get
                        }

                    val statistics = authRepository.getUserStatistics(userId)
                    logger.info("Profile retrieved successfully userId={}", userId)

                    val response = GetUserProfileResponse(
                        profile = UserProfileDto(
                            userId = user.userId,
                            username = user.username,
                            displayName = user.displayName,
                            bio = user.bio,
                            profilePictureUrl = user.profilePictureUrl,
                            email = user.email,
                            createdAtEpochMillis = user.createdAtEpochMillis,
                            lastLoginAtEpochMillis = user.lastLoginAtEpochMillis,
                            updatedAtEpochMillis = user.updatedAtEpochMillis,
                        ),
                        statistics = UserStatisticsDto(
                            totalNotes = statistics.totalNotes,
                            notesShared = statistics.notesShared,
                            notesReceived = statistics.notesReceived,
                            lastSyncAtEpochMillis = statistics.lastSyncAtEpochMillis,
                        )
                    )

                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    logger.error("Error fetching profile", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Internal server error")
                    )
                }
            }

            put("/profile") {
                try {
                    val userId = call.requireUserId()
                    val request = call.receive<UpdateProfileRequest>()
                    logger.info("Updating profile for userId={}", userId)

                    val updatedUser = authRepository.updateProfile(
                        userId = userId,
                        displayName = request.displayName,
                        bio = request.bio,
                        profilePictureUrl = request.profilePictureUrl,
                        email = request.email,
                    )

                    logger.info("Profile updated successfully userId={}", userId)

                    val response = UpdateProfileResponse(
                        success = true,
                        message = "Profile updated successfully",
                        profile = UserProfileDto(
                            userId = updatedUser.userId,
                            username = updatedUser.username,
                            displayName = updatedUser.displayName,
                            bio = updatedUser.bio,
                            profilePictureUrl = updatedUser.profilePictureUrl,
                            email = updatedUser.email,
                            createdAtEpochMillis = updatedUser.createdAtEpochMillis,
                            lastLoginAtEpochMillis = updatedUser.lastLoginAtEpochMillis,
                            updatedAtEpochMillis = updatedUser.updatedAtEpochMillis,
                        )
                    )

                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    logger.error("Error updating profile", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Internal server error")
                    )
                }
            }
        }
    }
}
