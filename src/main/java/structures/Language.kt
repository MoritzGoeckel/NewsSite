package structures

public enum class Language {DE, EN, NONE}

fun Language.folderName(): String {
    if(this == Language.DE){
        return "de";
    }

    if(this == Language.EN){
        return "en";
    }

    return "none";
}