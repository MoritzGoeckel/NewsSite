package parsers

import printTrace
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Tag
import org.jsoup.select.NodeVisitor
import java.net.URI
import java.net.URISyntaxException

fun Tag.isHeadline(): Boolean {
    val name = this.normalName()
    if(name == "h1" || name == "h2" || name == "h3") return true
    return false
}

fun String.removeUrlPrefixes(): String{
    var result = this
    if(this.startsWith("https://")) result = this.removePrefix("https://")
    else if(this.startsWith("http://")) result = this.removePrefix("http://")

    result = result.removePrefix("www.")
    return result
}

fun String.isUrlAbsolute(): Boolean{
    return this.startsWith("http://") || this.startsWith("https://") || this.startsWith("www.");
}

fun String.getHost(): String{
    return try {
        val host = URI(this).host
        if(host == null) return ""
        return host.removeUrlPrefixes()
    } catch (e: URISyntaxException){
        ""
    }
}

fun String.normalizeUrl(base_url: String): String{
    val baseWithoutPrefix = base_url.removeUrlPrefixes()
    val urlWithoutPrefix = this.removeUrlPrefixes()

    val baseHost = base_url.getHost()
    val host = this.getHost()

    if(urlWithoutPrefix.startsWith(baseWithoutPrefix)) return this; // already absolute

    // TODO use URI instead of self build things
    // uri.isAbsolute
    if(this.isUrlAbsolute() && host != baseHost){
        // Url is already absolute but points to a different domain
        printTrace("Extensions", "Discarding $this, because it is not part of $base_url")
        return ""; // discard
    }

    val separator = if(this.startsWith("/") || base_url.endsWith("/")) "" else "/"
    return base_url + separator + this
}

class TextNodesCollector : NodeVisitor {
    val strings: MutableList<String> = mutableListOf()
    override fun head(node: Node?, depth: Int) {
        if(node != null && node is TextNode && node.text().isNotEmpty()){
            strings.add(node.text())
        }
    }
    override fun tail(node: Node?, depth: Int) { }
}

fun Element.getTexts(): List<String>{
    val visitor = TextNodesCollector ()
    this.traverse(visitor)
    return visitor.strings
}

fun String.isHTML(): Boolean {
    return this.contains("""\<\/?\w+\>""".toRegex())
}