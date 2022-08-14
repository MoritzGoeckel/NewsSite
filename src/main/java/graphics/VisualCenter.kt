package graphics

import structures.Point
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import javax.imageio.ImageIO

fun downloadImage(imageUrl: URL): BufferedImage {
    return ImageIO.read(imageUrl)
}

fun getVisualCenter(imageUrl: URL, debugImagePath: File? = null): Point {
    val image = downloadImage(imageUrl)
    return getVisualCenter(image, debugImagePath)
}

fun getVisualCenter(image: BufferedImage, debugImagePath: File? = null): Point {
    var outImg: BufferedImage? = null
    if(debugImagePath != null){
        outImg = BufferedImage(image.width, image.height, image.type)
    }

    val threshold = 0.05f
    var xs = 0
    var ys = 0
    var counts = 0

    for(x in 0 until image.width){
        for(y in 0 until image.height){
            val color = Color(image.getRGB(x,y))

            // Check how homogenous this area is
            var diff = 0f
            var count = 0
            for(ox in -1 .. 1){
                for(oy in -1 .. 1){
                    if(x + ox > 0 && x + ox < image.width && y + oy > 0 && y + oy < image.height){
                        val otherColor = Color(image.getRGB(x + ox,y + oy))
                        diff += difference(color, otherColor)
                        ++count
                    }
                }
            }

            // Decide if it is an edge
            val decision = decide(diff / count, threshold)

            // Add edge to statistic
            if(decision > 0.9f){
                xs += x
                ys += y
                ++counts
            }

            // Draw edges, blend with original image, if requested
            outImg?.setRGB(x, y, add(grey(decision), color).rgb)
        }
    }

    val centerX = xs / counts
    val centerY = ys / counts

    if(outImg != null) {
        drawSquare(centerX, centerY, Color(255, 0, 0), 5, outImg)
        ImageIO.write(outImg, "jpg", debugImagePath)
    }

    return Point(
        centerX.toFloat() / image.width,
        centerY.toFloat() / image.height
    )
}
