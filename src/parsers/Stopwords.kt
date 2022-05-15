package parsers

import structures.Language
import java.io.File
import java.nio.file.Paths

class Stopwords(private var language: Language, private val stemmer: Stemmer) {

    private var stopwords: MutableSet<String> = mutableSetOf()

    init {
        var fileName = ""
        //print(Paths.get("").toAbsolutePath().toString()) // Prints the working directory

        if(language == Language.EN) {

            fileName = "data\\stopwords\\en.txt"
        }

        if(language == Language.DE) {
            fileName = "data\\stopwords\\de.txt"
        }

        val file = File(fileName)
        if(file.exists()) {
            file.forEachLine { stopwords.add(stemmer.stem(it)) }
        }
    }

    fun filter(words: List<String>): List<String> {
        return words.filter { !stopwords.contains(it) }
                    .filter {
                        // only normal length words or numbers
                        it.length > 1 || it.all { it.isDigit() }
                    }
                    .filter { it.isNotEmpty() }
    }
}