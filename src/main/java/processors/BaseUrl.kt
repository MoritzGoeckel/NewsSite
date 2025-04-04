package processors

fun getBaseUrl(url: String): String{
    return url.removePrefix("https")
        .removePrefix("http")
        .removePrefix(":")
        .removePrefix("//")
        .removePrefix("www.")
        .replace(Regex("/.*"), "")
}