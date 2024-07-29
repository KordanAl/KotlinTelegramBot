package ktb_stage_1

import java.io.File

const val ANSI_BLUE = "\u001B[34m"
const val ANSI_GREEN = "\u001B[32m"
const val ANSI_YELLOW = "\u001B[33m"
const val ANSI_RESET = "\u001B[0m"

private const val ONE_HUNDRED_PERCENT = 100
private const val ZERO_NUMBER = 0
private const val MAXIMUM_VALUE_OF_A_LEARNED_WORD = 3

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
        file.appendText("dog|собака|3\n")
        file.appendText("cat|кошка|3")
    }
}

private fun runFileParsing(wordFile: File, dictionary: MutableList<Word>) {
    val lines: List<String> = wordFile.readLines()
    for (it in lines) {
        val line = it.split("|")
        val original = line[0]
        val translate = line[1]
        val intCorrectAnswersCount = line.getOrNull(2)?.toIntOrNull() ?: ZERO_NUMBER

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
    val learnedCount = getTheNumberOfWordsLearned(dictionary)
    val totalCount = dictionary.size
    val percentageOfWordsLearned: Int = getPercentageFromTwoNumbers(learnedCount, totalCount)
    println("Вы выучили $learnedCount из $totalCount слов | $percentageOfWordsLearned%")
}


private fun getPercentageFromTwoNumbers(learnedCount: Int, totalCount: Int): Int =
    if (totalCount > ZERO_NUMBER) {
        (learnedCount.toDouble() / totalCount * ONE_HUNDRED_PERCENT).toInt()
    } else {
        ZERO_NUMBER
    }

private fun getTheNumberOfWordsLearned(dictionary: List<Word>) =
    dictionary.filter { it.correctAnswersCount >= MAXIMUM_VALUE_OF_A_LEARNED_WORD }.size

fun main() {
    val wordFile = File("words.txt")
    checkingAndCreatingFile(wordFile)

    val dictionary: MutableList<Word> = mutableListOf()
    runFileParsing(wordFile, dictionary)

    showMenu(dictionary)
}