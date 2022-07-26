package summarizer

import processors.TextProcessor
import structures.*

fun CharSequence.splitIncluding(vararg delimitersList: Char): List<String> {
    val delimiters = delimitersList.toHashSet()
    val result = ArrayList<String>()

    if (length == 0) {
        return result
    }

    var idx = 0
    var lastIdx = 0

    while (idx < length) {
        if (delimiters.contains(this[idx])) {
            result.add(substring(lastIdx, idx + 1))
            lastIdx = idx + 1
        }
        ++idx;
    }
    result.add(substring(lastIdx, length))
    return result.filter {
        it.length > 1
                && it.any { char -> !delimiters.contains(char) }
    }
}

class Summarizer(private val language: Language, private val length: Int) {
    private val processor = TextProcessor(language)

    fun summarize(text: String): String {
        val sentences = makeSentences(text)
        val document = processor.makeWordsKeepText(text)

        val candidates = sentences.map {
            Scored<Words>(it.similarity(document) / it.words.size, it)
        }.toMutableList()
        candidates.sortByDescending { it.score }

        val sb = StringBuilder()
        while (sb.length < length && candidates.isNotEmpty()){
            val sentence = candidates.removeFirst().element
            if(sb.length + sentence.text.length <= length) {
                sb.append(sentence.text)
                sb.append(' ')

                // Remove current sentence, re-calculate scores, sort again
                document.subtract(sentence)
                candidates.map { it.score = it.element.similarity(document) / it.element.words.size; it }
                candidates.sortByDescending { it.score }
            }
        }

        return sb.toString()
    }

    private fun makeSentences(text: String): List<Words> {
        return text
            .filter { it != '\r' }
            .splitIncluding('\n', '.', '!')
            .filter { !it.contains('"') && !it.contains('\'') } // TODO: quote parsing does not work yet
            .filter { !it.endsWith('\n') } // exclude headlines
            .map { it.trim() }
            .map { processor.makeWordsKeepText(it) }
    }
}