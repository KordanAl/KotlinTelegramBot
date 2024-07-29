package ktb_stage_1

import java.io.File

const val ANSI_BLUE = "\u001B[34m"
const val ANSI_GREEN = "\u001B[32m"
const val ANSI_YELLOW = "\u001B[33m"
const val ANSI_RESET = "\u001B[0m"

private const val ONE_HUNDRED_PERCENT = 100
private const val MAX_VALUE_LEARNED_WORD = 3

data class Word(
    val original: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
) {
    private val upOriginal: String
        get() = original.replaceFirstChar { it.uppercase() }

    private val upTranslate: String
        get() = translate.replaceFirstChar { it.uppercase() }

    override fun toString(): String {
        return "Для слова $ANSI_YELLOW$upOriginal$ANSI_RESET" +
                " правильный перевод $ANSI_GREEN$upTranslate$ANSI_RESET," +
                " правильных ответов: $ANSI_BLUE$correctAnswersCount$ANSI_RESET"
    }
}

private fun checkingAndCreatingFile(file: File) {
    if (!file.exists()) {
        file.createNewFile()
        file.writeText("hello|привет|0\n")
        file.appendText("dog|собака|0\n")
        file.appendText("bird|птица|0\n")
        file.appendText("tree|дерево|0\n")
        file.appendText("book|книга|0\n")
        file.appendText("car|машина|0\n")
        file.appendText("house|дом|0\n")
        file.appendText("water|вода|0\n")
        file.appendText("sun|солнце|0\n")
        file.appendText("moon|луна|0\n")
        file.appendText("flower|цветок|0\n")
        file.appendText("friend|друг|0\n")
        file.appendText("cat|кошка|0")
    }
}

private fun runFileParsing(wordFile: File, dictionary: MutableList<Word>) {
    val lines: List<String> = wordFile.readLines()
    for (it in lines) {
        val line = it.split("|")
        val original = line[0]
        val translate = line[1]
        val intCorrectAnswersCount = line.getOrNull(2)?.toIntOrNull() ?: 0

        val word = Word(original = original, translate = translate, correctAnswersCount = intCorrectAnswersCount)
        dictionary.add(word)
    }
}

private fun showScreenMenu(dictionary: List<Word>) {
    while (true) {
        println(
            """
        Меню:
        1 - учить слова
        2 - Статистика
        0 - Выход
        Введите необходимое значение:
    """.trimIndent()
        )

        when (readln().toIntOrNull()) {
            1 -> learningWords(dictionary)
            2 -> showStatisticInfo(dictionary)
            0 -> {
                println("Вы вышли из тренажера!")
                break
            }

            else -> println("Такого раздела нет в меню, попробуйте ввести корректное значение!")
        }
        Thread.sleep(1000)
    }
}

private fun learningWords(dictionary: List<Word>) {
    do {
        val listOfUnlearnedWords = dictionary.filter { it.correctAnswersCount < MAX_VALUE_LEARNED_WORD }

        if (listOfUnlearnedWords.isEmpty()) {
            println("Все слова выучены!")
            break
        } else {
            val randomWordToLearn: Word = listOfUnlearnedWords.random()
            val correctTranslateWords: String = randomWordToLearn.translate.replaceFirstChar { it.uppercase() }
            val answerOptions: List<Any> = listOfUnlearnedWords.shuffled().take(3)
                .map { word -> word.translate.replaceFirstChar { it.uppercase() } } + correctTranslateWords

            println("Слово: ${randomWordToLearn.original}, выбери варианты ответов:")
            answerOptions.forEachIndexed { index, word ->
                println("${index + 1}. $word")
            }
            println("0. Выход")

            val inputNumber: Int? = readln().toIntOrNull()
            if (inputNumber == null || inputNumber == 0) break
        }
    } while (true)
}

private fun showStatisticInfo(dictionary: List<Word>) {
    val wordsLearned: Int = dictionary.filter { it.correctAnswersCount >= MAX_VALUE_LEARNED_WORD }.size
    val allWords: Int = dictionary.size
    val percentOfWordsLearned: Int = (wordsLearned.toDouble() / allWords * ONE_HUNDRED_PERCENT).toInt()

    println("Вы выучили $wordsLearned из $allWords слов | $percentOfWordsLearned%")
}

fun main() {
    val wordFile = File("words.txt")
    checkingAndCreatingFile(wordFile)


    val dictionary: MutableList<Word> = mutableListOf()
    runFileParsing(wordFile, dictionary)

    showScreenMenu(dictionary)
}