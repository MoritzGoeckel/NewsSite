package programs

import server.WebServer

fun main() {
    val server = WebServer()
    server.start()
    while (true) {
        Thread.sleep(1000)
    }
}