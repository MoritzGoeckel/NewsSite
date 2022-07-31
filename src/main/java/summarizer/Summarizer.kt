package summarizer

import processors.TextProcessor
import processors.readLineConfig
import structures.*

fun CharArray.parseSentences(): List<String> {
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
    // TODO: 15. September

    var idx = 0
    var lastIdx = 0

    val enclosingPairs = hashMapOf(
        Pair('"', '"'),
        Pair('\'', '\''),
        Pair('(', ')'),
        Pair('{', '}'),
        Pair('[', ']'),
        Pair('„', '“'),
        //Pair('«', '»')
    )

    val closingStack = mutableListOf<Char>()

    while (idx < size) {
        val current = this[idx]
        if (closingStack.isNotEmpty() && closingStack.last() == current){
            // close enclosing
            closingStack.removeLast()
        } else {
            val foundClosing = enclosingPairs[current]
            if (foundClosing != null) {
                // open enclosing
                closingStack.add(foundClosing)
            }
        }

        if (closingStack.isEmpty() && delimiters.contains(current)) {
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
        val terminator = setOf('.', '?', '!')

        return text
            .filter { it != '\r' } // we use only \n and no \r
            .toCharArray()
            .parseSentences() // actually parse sentences
            .asSequence()
            .map { it.trim() } // remove trailing whitespace
            .filter { it.isNotEmpty() } // remove empty sentences
            .filter { it.count { char -> char.isWhitespace() } >= 3 } // remove too short sentences
            .filter {
                // remove sentences that start with connecting words
                sentence -> !discardedStarts.any { sentence.lowercase().startsWith(it) }
            }
            .map {
                // Terminate unterminated sentences with the default terminator '.'
                if(!terminator.contains(it.last())) {
                    "$it." // Add default terminator
                } else {
                    it // Already terminated
                }
            }
            //.filter { terminator.contains(it.last()) } // include only terminated sentences
            // .map { println(it); it } // print sentences
            .map { processor.makeWordsKeepText(it) } // create documents with words
            .toList()
    }
}