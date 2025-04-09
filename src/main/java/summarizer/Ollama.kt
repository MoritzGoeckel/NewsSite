package summarizer

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import structures.Article
import structures.Original

fun main(){
    val api = Ollama()
    val article = Article.fromJson("""{
        "preview_head": "Vorwurf Menschenhandel: Razzien in mehreren Bundesländern",
        "preview_content": "Vorwurf Menschenhandel: Razzien in mehreren Bundesländern",
        "preview_url": "https://bnn.de/nachrichten/deutschland-und-welt/vorwurf-menschenhandel-razzien-in-mehreren-bundeslaendern",
        "source": "https://bnn.de/",
        "base_url": "bnn.de",
        "url": "https://bnn.de/nachrichten/deutschland-und-welt/vorwurf-menschenhandel-razzien-in-mehreren-bundeslaendern",
        "head": "Vorwurf Menschenhandel: Razzien in mehreren Bundesländern",
        "description": "Polizeien und andere Behörden durchsuchen am Morgen 31 Objekte in mehreren Bundesländern und Tschechien. Es geht auch um den Verdacht von Menschenhandel und illegaler Prostitution.",
        "content": "Vorwurf Menschenhandel: Razzien in mehreren Bundesländern\nPolizeien und andere Behörden durchsuchen am Morgen 31 Objekte in mehreren Bundesländern und Tschechien. Es geht auch um den Verdacht von Menschenhandel und illegaler Prostitution.\nBei Ermittlungen wegen des Verdachts von Menschenhandel und illegaler Prostitution wurden Razzien in Deutschland und Tschechien durchgeführt. (Symbolbild)\nFoto: David Inderlied/dpa\nWegen des Verdachts des gewerbsmäßigen Einschleusens von Ausländern, Menschenhandel und illegaler Prostitution sind in mehreren Bundesländern und Tschechien Wohnungen und Gebäude durchsucht worden. Tatverdächtige wurden in Deutschland und Tschechien festgenommen, sagte ein Sprecher der Bundespolizei Sachsen-Anhalt. Hintergrund seien Ermittlungen gegen eine Tätergruppe, die vor allem Frauen aus Vietnam wohl mittels erschlichener Visa in die EU eingeschleust und in die Prostitution gezwungen haben soll. Zuerst berichtete die „Mitteldeutsche Zeitung“.\nInsgesamt seien 25 Wohnungen und Prostitutionsstätten in Deutschland durchsucht worden, sechs in Tschechien. In Deutschland standen demnach etwa Häuser in Chemnitz in Sachsen, in Halle in Sachsen-Anhalt, in Gera in Thüringen, in Essen und Dortmund in Nordrhein-Westfalen und in Kassel in Hessen im Visier der Ermittler. Die Einsätze seien weitgehend abgeschlossen und in Abstimmung mit Europol erfolgt, so der Polizeisprecher.\n",
        "image": "https://bnn.de/img/14817118/v2YwUNzq8zvztJbZ4tQkqQ/urn-newsml-dpacom-20090101-250409-935-527977?size=1400&format=jpeg&variant=LANDSCAPE_16x9",
        "image_metadata": "",
        "created_at": "2025-04-09T08:24:20.504679Z",
        "published_at": "1970-01-01T00:00:00Z",
        "id": 149030
      }""")
    val result = api.summarize(article)
    println(result)
}

private const val MODEL = "gemma3:1b"

class Ollama : SummarizerImpl() {
    public override fun summarize(article: Article): Original {
        val text = makeText(article)
        return Original(
            makeHeadline(text),
            makeTeaser(text),
            makeBulletPoints(text) + "\n\n" + makeSummary(text),
            listOf(article.image),
            "",
            makeText(article))
    }

    public override fun summarize(articles: List<Article>): Original {
        val text = makeText(articles)
        return Original(
            makeHeadline(text),
            makeTeaser(text),
            makeBulletPoints(text) + "\n\n" + makeSummary(text),
            articles.map { it.image }.filter { it.strip().isNotEmpty() },
            "",
            text)
    }

    private fun cleanOutput(text: String): String{
        return text
            .replace("**", "")
            .strip()
            .removeSurrounding("„","“")
            .removeSurrounding("'", "'")
            .removeSurrounding("\"", "\"")
    }

    private fun makeSummary(inputText: String): String {
        val lines = mutableListOf<String>()
        lines.add("Schreibe eine Zusammenfassung des folgenden Artikels:")
        lines.add(inputText)
        lines.add("Schreibe eine Zusammenfassung für den Oben stehenden Artikel. Die Zusammenfassung soll dicht an Information sein und sich nicht wiederholen. Halte dich dafür Inhaltlich genau an den Artikel, aber formuliere die Informationen neu. Schreibe nur die Zusammenfassung als Output.")

        val prompt = lines.joinToString("\n")

        val result = query(prompt)
        val json = JsonParser.parseString(result)
        return removeLinesWith(cleanOutput(json.asJsonObject.get("response").asString), "Zusammenfassung")
    }

    fun makeText(article: Article): String{
        val lines = mutableListOf<String>()
        lines.add(cleanStr(article.preview_head))
        lines.add(cleanStr(article.head))
        lines.add(cleanStr(article.preview_content))
        lines.add(cleanStr(article.description))
        lines.add(cleanStr(article.content))
        return linesToString(lines)
    }

    fun makeText(articles: List<Article>): String{
        val lines = mutableListOf<String>()
        articles.forEach { article ->
            lines.add(cleanStr(article.preview_head))
            lines.add(cleanStr(article.head))
            lines.add(cleanStr(article.preview_content))
            lines.add(cleanStr(article.description))
            lines.add(cleanStr(article.content))
        }
        return linesToString(lines)
    }

    private fun splitCustom(line: String): List<String>{
        val termiators = setOf('\n', '.', '?', '!')
        val result = mutableListOf<String>()

        val str = StringBuilder()
        line.strip().forEach {
            if(it == '\n') str.append(".")
            else str.append(it)

            if(termiators.contains(it)){
                result.add(String(str))
                str.clear()
            }
        }
        if (str.isNotEmpty()){
            // unterminated string
            str.append(".")
            result.add(String(str))
        }

        val nothing = setOf('?', ' ', '!', '?', '\n', '\t')
        return result
            .map { it.strip() }
            .filter { it.any { char -> !nothing.contains(char) } }
    }

    private fun linesToString(lines: List<String>): String {
        val expanded = mutableListOf<String>()
        lines.map { it.strip() }
            .map { splitCustom(it) }
            .forEach { its -> its.forEach{ expanded.add(it.strip())} }

        val unique = mutableSetOf<String>()
        val result = mutableListOf<String>()
        for (line in expanded){
            val normalized = line.strip().toLowerCase()
            if(unique.add(normalized)){
                result.add(line)
            }
        }
        return result.filter { it.strip().isNotEmpty() }.joinToString(" ")
    }

    private fun removeLinesWith(text: String, keyword: String): String = text.split("\n")
        .filter { !it.toLowerCase().contains(keyword.toLowerCase()) }
        .filter { it.strip().isNotEmpty() }
        .joinToString("\n")

    fun makeTeaser(inputText: String): String{
        val lines = mutableListOf<String>()
        lines.add("Schreibe einen Teaser für den folgenden Artikel:")
        lines.add(inputText)
        lines.add("Schreibe einen Teaser für den Oben stehenden Artikel. Der Teaser soll maximal 25 Worte kurz sein. Schreibe NUR den Teaser als Output.")
        val prompt = lines.joinToString("\n")

        val result = query(prompt)
        val json = JsonParser.parseString(result)
        val text = json.asJsonObject.get("response").asString.strip()
        return removeLinesWith(cleanOutput(text.replace("**", "")), "teaser")
    }

    fun makeBulletPoints(inputText: String): String {
        val lines = mutableListOf<String>()
        lines.add("Fasse den folgenden Artikel als eine Liste von Fakten zusammen:")
        lines.add(inputText)
        lines.add("Fasse den Oben stehenden Artikel als eine Liste von Fakten zusammen. Es sollen zwischen 3 und 8 Listeneinträge sein. Die Informationen der Einträge sollen sich nicht wiederholen. Jeder Listeneintrag soll möglichst kurz sein und ein bis zwei Informationen enthalten. Kennzeichne die Bulletpoints mit '-'. Schreibe NUR die Liste als Output")

        val prompt = lines.joinToString("\n")

        val result = query(prompt)
        val json = JsonParser.parseString(result)
        val text = json.asJsonObject.get("response").asString.strip()
        val cleaned =
            text.lines()
                .map { it.strip() }
                .filter { it.startsWith('-') }
                .map { it.removePrefix("-") }
                .map{ it.strip() }
                .map { it.removeSuffix(".") }
                .map { "- $it" }
                .joinToString("\n")

        return cleanOutput(cleaned)
    }

    fun cleanStr(text: String): String{
        return text
    }

    fun makeHeadline(inputText: String): String {
        val lines = mutableListOf<String>()
        lines.add("Denke dir eine neue Überschrift für den Oben stehenden Artikel aus:")
        lines.add(inputText)
        lines.add("Denke dir eine neue Überschrift für den Oben stehenden Artikel aus. Die Überschrift soll neu sein und nicht Teil deines Inputs. Bitte formuliere die Überschrift neu. Die Überschrift soll möglichst viel Information enthalten, aber nicht zu lang sein. Schreibe nur die Überschrift und nichts anderes.")

        val prompt = lines.joinToString("\n")

        val result = query(prompt)
        val json = JsonParser.parseString(result)
        return removeLinesWith(cleanOutput(json.asJsonObject.get("response").asString), "Überschrift")
    }

    fun query(input: String): String{
        val client = OkHttpClient()

        val data = JsonObject()
        data.addProperty("model", "gemma3:1b")
        data.addProperty("prompt", input)
        data.addProperty("stream", false)

        val body: RequestBody = data.toString().toRequestBody("application/json".toMediaType())

        val request: Request = Request.Builder()
            .url("http://localhost:11434/api/generate")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        if(response.body == null){
            throw Exception("Null body!")
        }

        return response.body!!.string()
    }
}