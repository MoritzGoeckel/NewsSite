package programs

import util.Configuration
import summarizer.GPT

fun main() {
    val config = Configuration()
    val gpt = GPT(config.openAIKey())
    val result = gpt.doRequest("Say hi please :)")
    println(result)
}