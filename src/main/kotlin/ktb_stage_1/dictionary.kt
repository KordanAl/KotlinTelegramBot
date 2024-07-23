package ktb_stage_1

import java.io.File
import java.util.*

data class Word(
    val original: String,
    val translate: String,
    var correctAnswersCount: Int = 0
)

fun File.writeToLowercaseToBeginning(text: String) {
    val resultText = text.lowercase(Locale.getDefault())
    val originalText = this.readText()

    this.writeText(resultText + "\n" + originalText)
}

fun main() {

    val wordFile = File("words.txt")

    // Тестовые данные для первого запуска
//    wordFile.createNewFile()
//    wordFile.writeToLowercaseToBeginning(
//        "hello|привет|0\n" +
//                "dog|собака|\n" +
//                "cat|кошка|3"
//    )
    // наш будущий словарь
    val dictionary: MutableList<Word> = mutableListOf()

    val lines: List<String> = wordFile.readLines()
    for (it in lines) {
        val line = it.split("|")
        val original = line[0]
        val translate = line[1]
        val intCorrectAnswersCount = line.getOrNull(2)?.toIntOrNull() ?: 0

        val word = Word(original = original, translate = translate, correctAnswersCount = intCorrectAnswersCount)
        dictionary.add(word)
    }

    dictionary.forEach{ println(it) }
}
