package programs

import grouping.Clusterer
import printInfo
import processors.TextProcessor
import structures.Language
import structures.Words
import java.io.File

class Loader {
    var docs: MutableList<Words> = mutableListOf()

    init {
        val processor = TextProcessor(Language.EN)
        File("data/samples/abcnews-date-text.csv").forEachLine {
            val words = processor.makeWords(it.split(',')[1])
            if(words.isNotEmpty()) {
                docs.add(words)
            }
        }
        printInfo("Loader", "Sample docs: ${docs.size}")
    }
}

fun main() {
    val loader = Loader()
    val afterLoad = System.currentTimeMillis()
    val docs = loader.docs

    val clusterer = Clusterer<Words>()
    val insertedDocs = mutableListOf<Words>()

    var i = 0
    while(i < docs.size){
        insertedDocs.add(docs[i])
        clusterer.add(docs[i])
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