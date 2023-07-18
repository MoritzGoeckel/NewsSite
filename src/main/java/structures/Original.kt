package structures

import com.google.gson.JsonArray
import printInfo
import java.sql.Connection

data class Original(val header: String, val content: String, val images: List<String>, val url: String, val rawIn: String, val rawOut: String) {
    fun insertInto(connection: Connection): Boolean {
        val preparedStatement = connection.prepareStatement("INSERT INTO originals (url, head, content, media, raw_in, raw_out) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;")

        printInfo("Original", "Inserted original: $this")

        val mediaJson = JsonArray()
        images.forEach{ mediaJson.add(it) }

        preparedStatement.setString(1, url)
        preparedStatement.setString(2, header)
        preparedStatement.setString(3, content)
        preparedStatement.setString(4, mediaJson.toString())
        preparedStatement.setString(5, rawIn)
        preparedStatement.setString(6, rawOut)
        return preparedStatement.execute()
    }

}