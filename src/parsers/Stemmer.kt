package parsers

import structures.Language

class Stemmer(private var language: Language) {
    fun stem(words: List<String>): List<String>{
        return words.map { stem(it) }
                    .filter { it.isNotEmpty() }
    }

    fun stem(word: String): String {
        val result = word
            .lowercase()
            .filter { it.isLetterOrDigit() } // remove special chars

        if(language == Language.EN) {
            return result.removeSuffix("ed")
                .removeSuffix("s")
                .removeSuffix("ly")
                .removeSuffix("ing")
        }

        if (language == Language.DE) {
            return result // TODO: DE
        }

        throw Exception("Language not implemented!");
    }
}