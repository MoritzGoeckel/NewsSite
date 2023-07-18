import java.time.Duration

class Configuration {
    private var frontPageScrapingInterval: Duration = Duration.ofMinutes(20)
    private var openAIKey: String = ""

    fun frontPageScrapingInterval(): Duration {
        return frontPageScrapingInterval
    }

    fun postgresUser(): String {
        return "postgres"
    }

    fun postgresPassword(): String {
        return "manager"
    }

    fun postgresUrl(): String {
        return "jdbc:postgresql://localhost:5432/news_site"
    }

    fun openAIKey(): String {
        return openAIKey
    }

    init {
        run {
            val variableName = "log_level"
            val maybe: String? = System.getenv(variableName)
            if (!maybe.isNullOrEmpty()) {
                // SimpleLogger.DEFAULT_LOG_LEVEL_KEY
                System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", maybe);
                printInfo("Configuration", "$variableName=${maybe}")
            } else {
                printInfo("Configuration", "$variableName=UNSET")
            }
        }

        run {
            val variableName = "front_page_scraping_interval"
            val maybe: String? = System.getenv(variableName)
            if (!maybe.isNullOrEmpty()) {
                frontPageScrapingInterval = Duration.ofSeconds(Integer.parseInt(maybe).toLong())
            }
            printInfo("Configuration", "$variableName=${frontPageScrapingInterval}")
        }

        run {
            val variableName = "open_ai_key"
            val maybe: String? = System.getenv(variableName)
            if(!maybe.isNullOrEmpty()){
                openAIKey = maybe
            }
            printInfo("Configuration", "$variableName=${openAIKey}")
        }
    }
}