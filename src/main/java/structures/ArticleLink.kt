package structures

import processors.getBaseUrl

class ArticleLink(val header: String, val url: String, val source: String) {
    fun sourceName(): String{
        return getBaseUrl(source)
    }
}
