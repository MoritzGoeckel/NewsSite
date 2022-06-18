package structures

import com.google.gson.JsonObject

class Point(val x: Float, val y: Float){
    fun toJson(): JsonObject {
        val result = JsonObject()
        result.addProperty("x", x)
        result.addProperty("y", y)
        return result
    }

    override fun toString(): String {
        return "x=$x, y=$y"
    }
}