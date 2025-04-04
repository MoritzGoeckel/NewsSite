package graphics

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.abs

fun clamp(value: Int, min: Int, max: Int): Int{
    if (value < min) return min
    if (value > max) return max
    return value
}

fun add(first: Color, second: Color): Color {
    return Color(
        clamp(first.red + second.red, 0, 255),
        clamp(first.green + second.green, 0, 255),
        clamp(first.blue + second.blue, 0, 255)
    )
}

fun blend(first: Color, second: Color, weight: Float): Color {
    val invWeight = 1 - weight
    return Color(
        (weight * first.red + invWeight * second.red).toInt(),
        (weight * first.green + invWeight * second.green).toInt(),
        (weight * first.blue + invWeight * second.blue).toInt()
    )
}

fun drawSquare(x: Int, y: Int, color: Color, size: Int, image: BufferedImage){
    val rgb = color.rgb
    for(ox in -size .. size){
        for(oy in -size .. size){
            if(x + ox > 0 && x + ox < image.width && y + oy > 0 && y + oy < image.height){
                image.setRGB(x + ox, y + oy, rgb)
            }
        }
    }
}

fun decide(value: Float, threshold: Float): Float{
    return if (value >= threshold) 1f else 0f
}

fun grey(value: Int): Color {
    return Color(value, value, value)
}

fun grey(value: Float): Color {
    return grey((value * 255).toInt())
}

fun difference(first: Color, second: Color): Float {
    return (abs(first.red - second.red) + abs(first.green - second.green) + abs(first.blue - second.blue)) / (255f * 3f)
}