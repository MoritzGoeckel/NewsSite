package structures

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import printInfo
import processors.TextProcessor
import processors.getBaseUrl
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.LocalDateTime

class ArticleLink(val header: String, val url: String, val source: String) {
}
