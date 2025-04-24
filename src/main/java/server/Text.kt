package server

import java.time.Duration
import kotlin.math.min

fun toHTML(text: String): String {
    return text.replace("\n", "<br>")
}

fun formatContent(text: String): String{
    return text.replace("(?:^|\\n)(.+)(?:\$|\\n)".toRegex()) {
        if(it.groups.first() != null) {
            "<p>" + it.groups.first()!!.value.replace("\n", "") + "</p>" + "\n"
        }
        else {
            ""
        }
    }
}

fun shortenToClosestWord(text: String, length: Int): String {
    if (text.length <= length) {
        return text
    } else {
        val tolerance = 10
        val sb = StringBuilder()
        sb.append(text.substring(0, length - tolerance))
        val searchArea = text.substring(length - tolerance, min((length - tolerance) + tolerance * 2, text.length))
        if(!searchArea.contains(' ')) return text.substring(0, length - 3) + "..."
        val endIdx = searchArea.lastIndexOf(' ')
        sb.append(searchArea.substring(0, endIdx))
        return "$sb..."
    }
}

fun firstSentence(body: String): String{
    val sentence = "^.*?\\.\\s".toRegex().find(body)
    val result = sentence?.value ?: ""
    return shortenToClosestWord(result, 300)
}

fun formatTimeDuration(duration: Duration): String {
    if(duration.toSeconds() < 60) {
        if (duration.toSeconds() == 1L) {
            return "1 second"
        }
        return "${duration.toSeconds()} seconds"
    } else if(duration.toMinutes() < 60) {
        if (duration.toMinutes() == 1L) {
            return "1 minute"
        }
        return "${duration.toMinutes()} minutes"
    } else if(duration.toHours() < 24) {
        if (duration.toHours() == 1L) {
            return "1 hour"
        }
        return "${duration.toHours()} hours"
    } else if (duration.toDays() < 30) {
        if (duration.toDays() == 1L) {
            return "1 day"
        }
        return "${duration.toDays()} days"
    } else if (duration.toDays() < 365) {
        if (duration.toDays() / 30 == 1L) {
            return "1 month"
        }
        return "${duration.toDays() / 30} months"
    } else {
        if (duration.toDays() / 365 == 1L) {
            return "1 year"
        }
        return "${duration.toDays() / 365} years"
    }
}
