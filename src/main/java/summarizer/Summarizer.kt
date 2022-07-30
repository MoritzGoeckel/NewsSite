package summarizer

import processors.TextProcessor
import processors.readLineConfig
import structures.*

fun CharArray.splitSentences(): List<String> {
    val delimiters = hashSetOf('\n', '.', '!')
    val numericDelimiters = '.'
    val result = ArrayList<String>()

    if (size == 0) {
        return result
    }

    val isNumericDelimiter = fun (i: Int): Boolean {
        return this[i] == numericDelimiters
                && i != 0
                && i != size - 1
                && this[i - 1].isDigit()
                && this[i + 1].isDigit()
    }

    val isMultipleDots = fun (i: Int): Boolean {
        val dot = '.'
        return this[i] == dot
                && ((i != 0 && this[i - 1] == dot) || (i != size - 1 && this[i + 1] == dot))
    }

    val nextLetterOrDigit = fun (i: Int): Char {
        for (idx in i until this.size){
            if (this[idx].isLetterOrDigit()) return this[idx]
        }
        return ' '
    }

    // TODO handle 'bbb .' issues
    var idx = 0
    var lastIdx = 0
    var isInQuote = false // TODO make it go to false after a certain distance automatically

    while (idx < size) {
        if(this[idx] == '\''){
            isInQuote = !isInQuote // toggle
        }

        if (!isInQuote && delimiters.contains(this[idx])) {
            if(!isNumericDelimiter(idx) && !isMultipleDots(idx)) {
                val nextChar = nextLetterOrDigit(idx)
                if(nextChar == ' ' || nextChar.isUpperCase()) {
                    // Only if next char is upper case, we assume its a sentence terminator
                    // Else it is probably a abbreviation
                    result.add(this.concatToString(lastIdx, idx + 1))
                    lastIdx = idx + 1
                }
            }
        }
        ++idx;
    }

    result.add(this.concatToString(lastIdx, size))
    return result.filter {
        it.length > 1
                && it.any { char -> !delimiters.contains(char) }
    }
}

class Summarizer(private val language: Language, private val length: Int) {
    private val processor = TextProcessor(language)

    private var discardedStarts: MutableSet<String> = mutableSetOf()
    init {
        discardedStarts.addAll(
            readLineConfig("discard_summary_sentence_start", language)
                .map { it.lowercase() }
        )
    }

    fun summarize(benchmarkText: String, sentencesSource: String): String {
        val sentences = makeSentences(sentencesSource)
        val document = processor.makeWordsKeepText(benchmarkText)

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

        return sb
            .toString()
            .replace(" . ", ". ") // some last cosmetics
    }

    private fun makeSentences(text: String): List<Words> {
        val quotes = setOf('\'', '„', '“', '«', '»')
        val terminator = setOf('.', '?', '!')

        // TODO add . after headlines?

        return text
            .filter { it != '\r' } // we use only \n and no \r
            .map { if (quotes.contains(it)) '"' else it } // replace quotes
            .toCharArray()
            .splitSentences()
            .asSequence()
            .map { it.trim() } // remove trailing whitespace
            .filter { terminator.contains(it.last()) } // TODO exclude headlines. Include only terminated sentences
            .filter {
                // remove sentences that start bad (expensive)
                sentence -> !discardedStarts.any { sentence.lowercase().startsWith(it) }
            }
            .map { processor.makeWordsKeepText(it) }
            .toList() // create documents with words
    }
}