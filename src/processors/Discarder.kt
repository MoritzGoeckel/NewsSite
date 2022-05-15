package processors

import structures.Language
import java.io.File

class Discarder(private val language: Language) {
    private var filter: MutableSet<Regex> = mutableSetOf()
    private val options: RegexOption = RegexOption.IGNORE_CASE

    init {
        filter.addAll(
            readLineConfig("discard", language)
                .map { Regex(it, options) }
        )
    }

    fun shouldDiscard(text: String): Boolean {
        for(f in filter) {
            if(text.contains(f)) {
                println(text + " -> discard") // TODO
                return true;
            }
        }
        return false;
    }
}