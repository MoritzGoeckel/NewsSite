package tests

import parsers.ArticlePageParser
import structures.Language
import summarizer.Summarizer

fun main() {
    val articlePageParser = ArticlePageParser()
    val urls = listOf("https://ga.de/news/politik/ausland/moderatorin-ohnmaechtig-britisches-premier-duell-abgebrochen_aid-73504841",
        "https://www.ariva.de/news/roundup-3-russland-will-nach-2024-aus-internationaler-10253752",
        "https://www.stern.de/wirtschaft/warnstreik--lufthansa-streicht-alle-fluege-in-frankfurt-und-muenchen-32574188.html",
        "https://www.wiwo.de//politik/ausland/ukraine-krieg-die-lage-am-dienstag-lambrecht-deutschland-hat-ukraine-mehrfachraketenwerfer-geliefert/28116300.html",
        "https://www.nordbayern.de//panorama/dramatische-lage-verheerende-brande-in-brandenburg-und-sachsen-ausser-kontrolle-1.12379117",
        "https://www.stern.de/sport/em-in-england--englaenderinnen-nach-4-0-spektakel-gegen-schweden-im-finale-32576204.html",
        "https://www.stern.de/digital/online/amazon-prime-erhoeht-preise---so-behalten-sie-das-alte-abo-32573572.html")

    val summarizer = Summarizer(Language.DE, 300)
    urls.map {
        try {
            val details = articlePageParser.extract(it)

            if (details.content.isEmpty()) {
                println("No content for $it")
            } else {
                println("################# START $it #################")
                println(details.content)
                println("################# SUMMARY $it #################")
                val summary = summarizer.summarize(details.content, details.content)
                println(summary)
                println("################# END $it #################")
            }
        } catch (e: Exception){
            println("Error downloading $it: ${e.message}")
        }
    }
}