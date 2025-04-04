package programs

/*
fun main() {
    val config = util.Configuration()
    val textProcessor = TextProcessor(Language.DE)
    val urls = File("data/pages/de.txt").readLines()
    val downloader = ArticleDownloader(textProcessor, urls)
    val containsCache = ContainsCache()

    val connection = DriverManager.getConnection(config.postgresUrl(), config.postgresUser(), config.postgresPassword())
    assert(connection.isValid(0))
    containsCache.fill(connection)

    val articlesQueue = mutableListOf<Article>()

    while (true) {
        val foundArticles = downloader.getNew()
        val newArticles = foundArticles.filter(Article::isNotEmpty).filter(containsCache::insert)
        printInfo("main", """New articles: ${newArticles.size} (down from ${foundArticles.size})""")
        newArticles.forEach{
            try {
                val isInserted = it.insertInto(connection)
                if (isInserted){
                    articlesQueue.add(it)
                }
                // assert(isInserted)
            } catch (e: Exception) {
                printError("main", """"Failed inserting: $it ${e.message}""")
            }
        }
        Thread.sleep(config.frontPageScrapingInterval().toMillis())
    }

    // TODO handle articlesQueue with clusterer / details
}*/