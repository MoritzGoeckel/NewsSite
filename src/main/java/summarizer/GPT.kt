package summarizer

import printError
import structures.Original

class GPT {
    fun isAvailable(): Boolean{
        // TODO()
        return true
    }

    fun generateOriginal(text: String, images: List<String>): Original{
        val prompt = StringBuilder()
        prompt.append("Schreibe eine Zusammenfassung und eine Überschrift für den folgenden Text:\n")
        prompt.append(text)

        printError("GPT", text)
        printError("GPT", images.toString())
        TODO()
    }
}