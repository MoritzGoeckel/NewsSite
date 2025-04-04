package processors

import structures.Language
import java.io.File
import java.lang.Exception

class Stopwords(private var language: Language, private val stemmer: Stemmer) {

    private var stopwords: MutableSet<String> = mutableSetOf()

    init {
        stopwords.addAll(
            readLineConfig("stopwords", language)
                .map { stemmer.stem(it) }
        )
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