package server

import structures.ArticleLink
import structures.Original

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

class Article(val url: String, val headline: String, val body: String, val links: List<ArticleLink>, val img: String) {
    fun firstSentence(): String{
        val sentence = "^.*?\\.\\s".toRegex().find(body)
        return sentence?.value ?: ""
    }

    fun renderHeadline(): String{
        val length = 100 // TODO shorten to word ending
        return if (headline.length <= length) headline else headline.substring(0, length - 3) + "..."
    }
}