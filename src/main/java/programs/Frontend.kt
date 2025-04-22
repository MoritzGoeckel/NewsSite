package programs

import server.FrontendServer
import util.Configuration
import java.sql.DriverManager

fun main() {
    val config = Configuration()

    val connection = DriverManager.getConnection(config.postgresUrl(), config.postgresUser(), config.postgresPassword())
    assert(connection.isValid(0))

    val frontendServer = FrontendServer(connection, config.frontendPort())
        .start()

    waitForever()
}