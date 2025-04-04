package processors

import structures.Language
import structures.folderName
import java.io.File
import printInfo

fun readLineConfig(path: String, language: Language): List<String> {
    val result = mutableListOf<String>()

    // Global
    val globalFile = File("""data\${path}\global.txt""")
    if(globalFile.exists()) {
        globalFile.readLines()
            .filter { it.isNotEmpty() }
            .forEach { result.add(it) }
    } else {
        printInfo("LineConfigReader", "Could not find file ${globalFile.absolutePath}")
    }

    // Language specific
    val localFile = File("""data\${path}\${language.folderName()}.txt""")
    if(localFile.exists()) {
        localFile.readLines()
            .filter { it.isNotEmpty() }
            .forEach { result.add(it) }
    } else {
        printInfo("LineConfigReader", "Could not find file ${localFile.absolutePath}")
    }

    return result
}