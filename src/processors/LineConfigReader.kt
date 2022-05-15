package processors

import structures.Language
import java.io.File

fun readLineConfig(path: String, language: Language): List<String> {
    var result = mutableListOf<String>()

    // Global
    var globalFile = File("""data\${path}\global.txt""")
    if(globalFile.exists()) {
        globalFile.readLines()
            .filter { it.isNotEmpty() }
            .forEach { result.add(it) }
    } else {
        // throw Exception("Could not find file $file")
    }

    // Language specific
    val localFile = File("""data\${path}\${language}.txt""")
    if(localFile.exists()) {
        localFile.readLines()
            .filter { it.isNotEmpty() }
            .forEach { result.add(it) }
    } else {
        // throw Exception("Could not find file $file")
    }

    return result
}