package programs

import Configuration
import grouping.Clusterer
import printInfo
import processors.TextProcessor
import structures.Article
import structures.Language
import structures.Words
import java.sql.DriverManager

fun main() {
    val config = Configuration()
    val processor = TextProcessor(Language.DE)
    val connection = DriverManager.getConnection(config.postgresUrl(), config.postgresUser(), config.postgresPassword())
    assert(connection.isValid(0))

    val select = connection.prepareStatement("SELECT * FROM articles ORDER BY created ASC")
    val result = select.executeQuery()

    val articles = mutableListOf<Article>()
    while (result.next()){
        val article = Article(
            header = result.getString("head"),
            content = result.getString("content"),
            url = result.getString("url"),
            source = result.getString("source"),
            processor = processor)
        // TODO: Also add date/created?
        articles.add(article)
    }

    val clusterer = Clusterer<Article>()
    val insertedDocs = mutableListOf<Article>()

    var i = 0
    while(i < articles.size){
        insertedDocs.add(articles[i])
        clusterer.add(articles[i])
        ++i

        while(insertedDocs.size > 1000){
            clusterer.remove(insertedDocs.first())
            insertedDocs.removeFirst()
        }

        if(i % 10_000 == 0){
            printInfo("Clustering", "\n${clusterer.statistics()}")
        }
    }

    println("Print")
    clusterer.clusters().filter { it.docs.size > 2 }
                      .sortedBy { it.docs.size }
                      .forEach { cluster ->
                          cluster.docs.forEach { println(it.text) }
                          println()
                      }
}