package parsers

import util.printTrace
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Tag
import org.jsoup.select.NodeVisitor
import util.printError
import java.net.URI

fun Tag.isHeadline(): Boolean {
    val name = this.normalName()
    if(name == "h1" || name == "h2" || name == "h3") return true
    return false
}

/*fun String.removeUrlPrefixes(): String{
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
}*/

fun String.toURI(): URI?{
    return try {
        URI.create(this)
    } catch (e: IllegalArgumentException){
        null
    }
}

fun String.normalizeUrl(base_url: String): String {

    val base = URI.create(base_url)
    var uri = toURI() ?: "$base_url/$this".toURI()

    if(uri == null){
        // printError("NormalizeUrl","Can't resolve URL for base=$base url=$this")
        return ""
    }

    if(!uri.isAbsolute){
        uri = URI.create("$base_url/$this")
    }

    if (uri!!.host != null && base.host != uri.host) {
        printTrace("Extensions", "Discarding $this, because it is not part of $base_url")
        return "" // discard urls from different host
    }

    return uri.normalize().toString()
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