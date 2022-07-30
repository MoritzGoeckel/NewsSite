package parsers

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Tag
import org.jsoup.select.NodeVisitor
import javax.print.DocFlavor

fun Tag.isHeadline(): Boolean {
    val name = this.normalName()
    if(name == "h1" || name == "h2" || name == "h3") return true
    return false
}

fun String.normalizeUrl(base_url: String): String{
    if(this.startsWith(base_url)) return this;
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