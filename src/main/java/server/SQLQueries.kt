package server

import structures.Original
import java.sql.Connection
import structures.Article
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant

fun selectArticles(connection: Connection): List<Article> {
    val selectStmt =
        connection.prepareStatement("SELECT * FROM articles WHERE created_at > ? AND created_at <= ? ORDER BY created_at DESC")
    selectStmt.setTimestamp(1, Timestamp.from(Instant.now().minus(Duration.ofHours(24))))
    selectStmt.setTimestamp(2, Timestamp.from(Instant.now()))

    val result = selectStmt.executeQuery()
    val localArticles = mutableListOf<Article>()
    while (result.next()) {
        localArticles.add(Article(result))
    }
    return localArticles
}

fun selectOriginals(connection: Connection): List<Original> {
    val selectStmt =
        connection.prepareStatement("SELECT * FROM originals WHERE created_at > ? AND created_at <= ? ORDER BY created_at DESC")
    selectStmt.setTimestamp(1, Timestamp.from(Instant.now().minus(Duration.ofHours(24))))
    selectStmt.setTimestamp(2, Timestamp.from(Instant.now()))

    val result = selectStmt.executeQuery()
    val localOriginals = mutableListOf<Original>()
    while (result.next()) {
        localOriginals.add(Original(result))
    }
    return localOriginals
}