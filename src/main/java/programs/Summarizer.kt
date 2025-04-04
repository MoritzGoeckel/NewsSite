package programs

import structures.Language
import summarizer.Summarizer
import java.io.File

fun main() {
    val content = File("data/samples/article_content.txt").readText()
    val summarizer = Summarizer(Language.DE, 300)
    val summary = summarizer.summarize(content, content)
    println("Summary: $summary")
}
