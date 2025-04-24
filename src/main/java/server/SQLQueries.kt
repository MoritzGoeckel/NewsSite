package server

import structures.Original
import java.sql.Connection
import structures.Article
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant


fun selectArticle(connection: Connection, id: Int): Article {
    val selectStmt =
        connection.prepareStatement("SELECT * FROM articles WHERE id = ?")
    selectStmt.setInt(1, id)

    val result = selectStmt.executeQuery()
    if (result.next()) {
        return Article(result)
    } else {
        throw Exception("Article with id $id not found")
    }
}

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

fun selectArticlesFor(connection: Connection, original: Original): List<Article> {
    if(original.id == -1 || original.id == 1) {
        throw Exception("Original with id ${original.id} does not make sense")
    }

    val selectStmt =
        connection.prepareStatement("SELECT * FROM articles WHERE original_id = ?")
    selectStmt.setInt(1, original.id)

    val result = selectStmt.executeQuery()
    val localArticles = mutableListOf<Article>()
    while (result.next()) {
        localArticles.add(Article(result))
    }
    return localArticles
}

fun selectOriginal(connection: Connection, id: Int): Original {
    val selectStmt =
        connection.prepareStatement("SELECT * FROM originals WHERE id = ?")
    selectStmt.setInt(1, id)

    val result = selectStmt.executeQuery()
    if (result.next()) {
        return Original(result)
    } else {
        throw Exception("Original with id $id not found")
    }
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

fun selectOriginals(connection: Connection, limit: Int): List<Original> {
    val selectStmt =
        connection.prepareStatement("SELECT * FROM originals ORDER BY created_at DESC LIMIT ?")
    selectStmt.setInt(1, limit)

    val result = selectStmt.executeQuery()
    val localOriginals = mutableListOf<Original>()
    while (result.next()) {
        localOriginals.add(Original(result))
    }
    return localOriginals
}
