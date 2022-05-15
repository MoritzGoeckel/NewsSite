package structures

open class Words(val text: String, val words: Map<String, Int>){
    constructor(): this("", mapOf())

    fun isNotEmpty(): Boolean{
        return !isEmpty()
    }

    fun isEmpty(): Boolean{
        return words.isEmpty()
    }
}
