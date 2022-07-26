package structures

open class Words(val text: String, public val words: MutableMap<String, Int>){
    constructor(): this("", mutableMapOf())

    fun isNotEmpty(): Boolean{
        return !isEmpty()
    }

    fun isEmpty(): Boolean{
        return words.isEmpty()
    }

    override fun toString(): String {
        return "{\"$text\" $words}"
    }
}

fun Words.similarity(other: Words): Double {
    if(this.words.isEmpty() || other.words.isEmpty()) return 0.0

    val smaller: Map<String, Int>
    val larger: Map<String, Int>
    if(this.words.size > other.words.size) {
        larger = this.words
        smaller = other.words
    } else {
        larger = other.words
        smaller = this.words
    }

    var same = 0
    var all = 0
    smaller.forEach{
        if(larger.containsKey(it.key)) same += larger[it.key]!! + it.value
        all += it.value
    }

    larger.forEach{
        all += it.value
    }

    return same.toDouble() / all.toDouble()
}

fun Words.subtract(other: Words) {
    other.words.map {
        if(words.computeIfPresent(it.key) { _, value -> value - it.value } != null){
            if(words[it.key]!! < 1) {
                words.remove(it.key)
            }
        }
    }
}
