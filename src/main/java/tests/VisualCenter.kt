package tests

import graphics.getVisualCenter
import java.io.File
import java.lang.Exception
import javax.imageio.ImageIO


fun main() {
    val path = "C:/Users/Moritz/Desktop/New folder";

    val img = ImageIO.read(File("${path}/17.jpg"))
    val center = getVisualCenter(img, File("${path}/info_17.jpg"))
    println("$center")

    /*for(i in 1 .. 17){
        try {
            val img = ImageIO.read(File("${path}/${i}.jpg"))
            val center = getVisualCenter(img, File("${path}/info_${i}.jpg"))
            println("$i -> $center")
        } catch (e: Exception) {
            println("Can't read: ${path}/info_${i}.jpg")
        }
    }*/
}

