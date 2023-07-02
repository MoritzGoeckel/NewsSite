package structures

open class Words(val text: String, public val words: MutableMap<String, Int>){
    constructor(): this("", mutableMapOf())

    fun remove(word: String, num: Int): Int /*remaining*/ {
        var remaining = 0
        // reduce the number of that word, remove it if it is zero then
        words.computeIfPresent(word) {_, oldValue ->
            val newValue = oldValue - num
            if(newValue > 0){
                remaining = newValue
                newValue
            } else {
                remaining = 0
                null
            }
        }
        return remaining
    }

    fun isNotEmpty(): Boolean{
        return !isEmpty()
    }

    fun isEmpty(): Boolean{
        return words.isEmpty()
    }

    fun add(other: Words) {
        other.words.forEach { (word, num) ->
            words[word] = this.words.getOrDefault(word, 0) + num
        }
    }

    fun similarity(other: Words): Double {
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

    fun subtract(other: Words) {
        other.words.map {
            if(words.computeIfPresent(it.key) { _, value -> value - it.value } != null){
                if(words[it.key]!! < 1) {
                    words.remove(it.key)
                }
            }
        }
    }

    override fun toString(): String {
        return "{\"$text\" $words}"
    }
}
