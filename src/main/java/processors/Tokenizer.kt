package processors

class Tokenizer{
    fun tokenize(text: String): List<String>{
       return text.split(' ', '/', ',', ':', '.', '-')
    }
}