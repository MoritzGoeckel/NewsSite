package processors

import structures.Language

class Transformer(private var language: Language) {

    class Action(private var regex: Regex, private var replacement: String){
        constructor(regex: Regex) : this(regex, ""){ }

        fun apply(text: String):String {
            return text.replace(regex, replacement)
        }
    }

    private var transformations: MutableSet<Action> = mutableSetOf()
    private val options: RegexOption = RegexOption.IGNORE_CASE

    init {
        // replaces
        val lines = readLineConfig("replace", language)
        for(i in lines.indices step 2) {
            transformations.add(Action(Regex(lines[i+0].removeSuffix("END"), options), lines[i+1].removeSuffix("END")))
        }

        // removes
        transformations.addAll(
            readLineConfig("remove", language)
                .map { Action(Regex(it.removeSuffix("END"), options)) }
        )
    }

    fun apply(text: String): String {
        var result = text
        for(transformation in transformations) {
            result = transformation.apply(result)
        }

        if(text != result) {
            // Do it again, until there is no more transformation
            result = apply(result);
        }

        return result
    }
}