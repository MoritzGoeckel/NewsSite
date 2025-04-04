package programs

fun main() {
    val text = "Dresden wird einen neuen wichtigen Arbeitgeber bekommen: Der taiwanische Chiphersteller TSMC hat angekündigt, ein Werk in der sächsischen Landeshauptstadt zu errichten. Insgesamt sollen zehn Milliarden Euro in das Projekt investiert werden. Die Entscheidung kommt vor dem Hintergrund des Ziels der EU, den Anteil der europäischen Chipproduktion bis 2023 auf 20% zu erhöhen. Mit der Ansiedlung von TSMC in Dresden wird die gesamte europäische Wirtschaft davon profitieren, da Europa dadurch weniger abhängig von außereuropäischen Zulieferern wird.\n" +
            "\n" +
            "Die Entscheidung von TSMC wurde von der sächsischen Regierung als \"gute Nachricht\" begrüßt. Zusammen mit der Entscheidung von Intel für Magdeburg und der Erweiterung von Infineon in Dresden wird die Ansiedlung von TSMC der regionalen Entwicklung in Mitteldeutschland einen enormen Schub verleihen. Thomas Schmidt, Sachsens Landesminister für Regionalentwicklung, betont, dass die gesamte Europäische Union von dieser Ansiedlung profitieren wird.\n" +
            "\n" +
            "Die Ansiedlung von TSMC in Dresden wird auch für die Schaffung neuer Arbeitsplätze sorgen. Bis Ende 2027 sollen 2.000 neue Arbeitsplätze entstehen. Um die benötigten Fachkräfte zu finden und zu fördern, ist außerdem die Einrichtung eines gemeinsamen Ausbildungszentrums in Sachsen geplant. Die Entscheidung von TSMC wird als großer Erfolg für die Region gewertet und stärkt Dresdens Position als einer der wichtigsten Standorte der Halbleiterindustrie weltweit.\n" +
            "\n" +
            "Die geplante Chipfabrik in Dresden soll eine monatliche Fertigungskapazität von 40.000 sogenannten Wafern haben. Diese werden Chips mit Größenordnungen von 22 bis 28 Nanometern und zwölf bis 16 Nanometern enthalten. Das Joint Venture, an dem auch Bosch, Infineon und NXP beteiligt sind, wird voraussichtlich in der zweiten Hälfte des Jahres 2024 mit dem Bau der Fabrik beginnen und die Fertigung Ende 2027 aufnehmen. TSMC wird dabei 70 Prozent des geplanten Joint Ventures halten, während Bosch, Infineon und NXP jeweils zehn Prozent beteiligt sein werden.\n" +
            "\n" +
            "Die genaue Höhe der Investition wird noch festgelegt und hängt von staatlichen Förderungen ab. Der Bund hatte bereits Subventionen von bis zu fünf Milliarden Euro in Aussicht gestellt, die jedoch noch von der EU-Kommission genehmigt werden müssen. Bundeswirtschaftsminister Robert Habeck unterstützt die Investition von TSMC und betont die Bedeutung einer robusten heimischen Halbleiterproduktion für die globale Wettbewerbsfähigkeit Deutschlands und Europas. Die Bundesregierung wird die Pläne im Rahmen des European Chips Act unterstützen.\n"

    val result = text.replace("(?:^|\\n)(.+)(?:\$|\\n)".toRegex()) {
        "<span>" + it.groups.first()!!.value.replace("\n", "") + "</span>" + "\n"
    }

    println(result)
}