import kotlin.math.roundToInt

fun main() {
    val start = System.currentTimeMillis()

    val page = MainPage();

    val urls = listOf(
        "https://www.spiegel.de/",
        "https://www.tagesschau.de/",
        "https://www.bild.de/",
        "https://www.br.de/nachrichten/",
        "https://www.morgenpost.de/vermischtes/",
        "https://www.t-online.de/nachrichten/",
        "https://www.ka-news.de/nachrichten/",
        "https://www.stern.de/",
        "https://www.augsburger-allgemeine.de/",
        "https://www.srf.ch/",
        "https://www.tah.de/",
        "https://www.br.de/",
        "https://www.finanznachrichten.de/",
        "https://www.ariva.de/news/")

    val articles = mutableListOf<Article>()

    urls.map { url ->
        val found = page.extract(url)
        println("Found ${found.size} for $url")
        found.forEach { articles.add(it) }
    }

    println(articles.distinct().size)

    val afterDownload = System.currentTimeMillis()

    println("Cluster")
    val clusterer = Clusterer()
    articles.forEach {
        var text = it.header.replace("in einem neuen Fenster öffnen", "")
        if(text.startsWith("Video ")) text = text.removePrefix("Video ")

        val words = text
            .split(' ', '/', ',', ':', '.')
            .map { it.lowercase() }
            .map {
                it.filter { it.isLetterOrDigit() }
            }
            .filter {
                it.length > 2 || it.all { it.isDigit() }
            }
            .filter { it.isNotEmpty() }
            .associateWith { 1 }

        //println(it.header)
        //println(words)

        clusterer.addDoc(Doc(text + " " + it.url, words))
    }

    val afterCluster = System.currentTimeMillis()

    println("Print")
    clusterer.clusters.filter { it.docs.size > 2 }
            // TODO: only show clusters with more than one source url
        .sortedBy { it.docs.size }
        .forEach { cluster ->
            cluster.docs.forEach { println(it.content) }
            println()
        }

    val afterPrint = System.currentTimeMillis()

    println("Statistics")
    println("Created ${clusterer.clusters.size} clusters")
    println("Loading    ${afterDownload - start}ms")
    println("Clustering ${afterCluster - afterDownload}ms / ${(articles.size.toDouble() / ((afterCluster - afterDownload) / 1000.0)).roundToInt()/1000}K documents/s")
    println("Print      ${afterPrint - afterCluster}ms")
}