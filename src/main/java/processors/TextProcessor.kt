package processors

import structures.Language
import structures.Words

class TextProcessor(private var language: Language) {

    private var stemmer = processors.Stemmer(language);
    private var stopwords = processors.Stopwords(language, stemmer);
    private var tokenizer = processors.Tokenizer();
    private var transformer = processors.Transformer(language);
    private var discarder = processors.Discarder(language);

    fun makeWordsKeepText(text: String): Words {
        // apply transformations
        val transformed = transformer.apply(text)

        // tokenize
        var result = tokenizer.tokenize(transformed)

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

    fun makeWords(text: String): Words {
        if(discarder.shouldDiscard(text)){
            return Words();
        }

        // apply transformations
        val transformed = transformer.apply(text)

        // tokenize
        var result = tokenizer.tokenize(transformed)

        // stem
        result = stemmer.stem(result)

        // remove stopwords
        result = stopwords.filter(result);

        // convert to frequencies
        val words = mutableMapOf<String, Int>()
        for(word in result){
            words[word] = words.getOrDefault(word, 0) + 1
        }

        return Words(transformed, words)
    }
}