package structures

open class Words(val text: String, public val words: MutableMap<String, Int>){
    constructor(): this("", mutableMapOf())

    fun isNotEmpty(): Boolean{
        return !isEmpty()
    }

    fun isEmpty(): Boolean{
        return words.isEmpty()
    }


}
