package structures

class Scored<T>(var score: Double, val element: T){
    override fun toString(): String {
        return "{$score -> $element}"
    }
}