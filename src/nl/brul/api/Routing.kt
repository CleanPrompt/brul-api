// src/nl/brul/api/Routing.kt
package nl.brul.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import nl.brul.api.features.auth.AuthService
import nl.brul.api.features.auth.authRoutes
import nl.brul.api.features.help.HelpService
import nl.brul.api.features.help.helpRoutes
import nl.brul.api.features.report.ReportService
import nl.brul.api.features.report.reportRoutes
import nl.brul.api.features.user.userRoutes
import nl.brul.api.features.vote.VoteService
import nl.brul.api.features.vote.voteRoutes

fun Application.configureRouting(
    authService: AuthService,
    reportService: ReportService,
    voteService: VoteService,
    helpService: HelpService
) {
    routing {
        route("/api/v1") {
            get("/health") {
                call.respond(
                    HttpStatusCode.OK,
                    mapOf("status" to "ok")
                )
            }

            authRoutes(authService)
            userRoutes()
            reportRoutes(reportService)
            voteRoutes(voteService)
            helpRoutes(helpService)
        }
    }
}
