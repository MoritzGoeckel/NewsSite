package parsers

import structures.Language
import structures.Words

class TextProcessor(private var language: Language) {

    private var stemmer = parsers.Stemmer(language);
    private var stopwords = parsers.Stopwords(language, stemmer);
    private var tokenizer = parsers.Tokenizer();

    fun makeWords(text: String): Words {
        // tokenize
        var result = tokenizer.tokenize(text)

        // stem
        result = stemmer.stem(result)

        // remove stopwords
        result = stopwords.filter(result);

        // convert to frequencies
        val words = mutableMapOf<String, Int>()
        for(word in result){
            words[word] = words.getOrDefault(word, 0) + 1
        }

        return Words(text, words)
    }
}