package ktb_stage_1

import java.io.File

const val ANSI_BLUE = "\u001B[34m"
const val ANSI_GREEN = "\u001B[32m"
const val ANSI_YELLOW = "\u001B[33m"
const val ANSI_RESET = "\u001B[0m"

const val ONE_HUNDRED_PERCENT = 100

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

private fun File.checkingAndCreatingFile() {
    if (!exists()) {
        createNewFile()
        writeText("hello|привет|0\n")
        appendText("dog|собака|3\n")
        appendText("cat|кошка|3")
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

private fun showMenu(dictionary: List<Word>) {
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

        when (val input: Int? = readln().toIntOrNull()) {
            1 -> println("Вы ввели $input")
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

private fun showStatisticInfo(dictionary: List<Word>) {
    val learnedWords = dictionary.filter { it.correctAnswersCount >= 3 }
    val learnedCount = learnedWords.size
    val totalCount = dictionary.size
    val percentage = if (totalCount > 0) {
        (learnedCount.toDouble() / totalCount * ONE_HUNDRED_PERCENT).toInt()
    } else {
        0
    }
    println("Вы выучили $learnedCount из $totalCount слов | $percentage%")

    while (true) {
        println("Чтобы выйти в главное меню введите 0:")
        val inputStatistic: Int? = readln().toIntOrNull()
        if (inputStatistic == 0) {
            break
        } else {
            println("Такого раздела нет в меню, попробуйте ввести корректное значение!")
        }
    }
}

fun main() {

    val wordFile = File("words.txt")
    wordFile.checkingAndCreatingFile()

    val dictionary: MutableList<Word> = mutableListOf()
    runFileParsing(wordFile, dictionary)

    showMenu(dictionary)
}