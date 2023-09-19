package server

import structures.ArticleLink
import structures.Original
import java.lang.StringBuilder
import kotlin.math.min

fun formatContent(text: String): String{
    return text.replace("(?:^|\\n)(.+)(?:\$|\\n)".toRegex()) {
        if(it.groups.first() != null) {
            "<p>" + it.groups.first()!!.value.replace("\n", "") + "</p>" + "\n"
        }
        else {
            ""
        }
    }
}

fun articleFromOriginal(original: Original): Article{
    return Article(
        original.url,
        original.head,
        formatContent(original.content),
        original.getSources(),
        original.images.first())
}

fun shortenToClosestWord(text: String, length: Int): String {
    if (text.length <= length) {
        return text
    } else {
        val tolerance = 10
        val sb = StringBuilder()
        sb.append(text.substring(0, length - tolerance))
        val searchArea = text.substring(length - tolerance, min((length - tolerance) + tolerance * 2, text.length))
        if(!searchArea.contains(' ')) return text.substring(0, length - 3) + "..."
        val endIdx = searchArea.lastIndexOf(' ')
        sb.append(searchArea.substring(0, endIdx))
        return sb.toString()
    }
}

class Article(val url: String, val headline: String, val body: String, val links: List<ArticleLink>, val img: String) {
    fun firstSentence(): String{
        val sentence = "^.*?\\.\\s".toRegex().find(body)
        val result = sentence?.value ?: ""
        return shortenToClosestWord(result, 300)
    }

    fun renderHeadline(): String{
        val length = 100 // TODO shorten to word ending
        return shortenToClosestWord(headline, 100)
    }
}