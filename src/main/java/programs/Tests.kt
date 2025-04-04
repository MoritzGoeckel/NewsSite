package programs

import parsers.normalizeUrl

class Tests {

    @Test
    fun normalizeUrl(){
        expectEq("https://www.zeit.de/news/2025-04/04/uni",
            "https://www.zeit.de/news/2025-04/04/uni".normalizeUrl("https://www.zeit.de/"))

        expectEq("https://www.zeit.de/news/",
            "https://www.zeit.de/news/".normalizeUrl("https://www.zeit.de/"))

        expectEq("",
            "https://www.someotherdomain.de/news/".normalizeUrl("https://www.zeit.de/"))

        expectEq("https://www.zeit.de/news/2025-04/04/uni",
            "/news/2025-04/04/uni".normalizeUrl("https://www.zeit.de/"))

        expectEq("https://www.zeit.de/news/2025-04/04/uni",
            "news/2025-04/04/uni".normalizeUrl("https://www.zeit.de"))

        expectEq("http://www.zeit.de/news/2025-04/04/uni",
            "news/2025-04/04/uni".normalizeUrl("http://www.zeit.de"))

        expectEq("https://www.zeit.de/news/2025-04/04/uni",
            "https://www.zeit.de/news/2025-04/04/uni".normalizeUrl("http://www.zeit.de"))
    }
}

annotation class Test

fun expectEq(expected: String, actual: String){
    if(expected != actual){
        throw Exception("$expected != $actual")
    }
}

fun main(){
    val test = Tests()
    for(m in test.javaClass.methods){
        if(m.annotations.contains(Test())) {
            print(m.name)
            m.invoke(test)
            print(" OK")
            println()
        }
    }
}