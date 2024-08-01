package ktb_stage_1

import java.io.File

private const val ONE_HUNDRED_PERCENT = 100
private const val MAX_VALUE_LEARNED_WORD = 3
private const val FOUR_WORDS_FOR_ANSWER_OPTIONS = 4
private const val INCORRECT_VALUE = 5

data class Word(
    val original: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
)

private fun creatingFileAndFillingItWithBasicWords(file: File) {
    if (!file.exists()) {
        file.createNewFile()

        val baseWords = listOf(
            "hello|привет|0",
            "dog|собака|0",
            "bird|птица|0",
            "tree|дерево|0",
            "book|книга|0",
            "car|машина|0",
            "house|дом|0",
            "water|вода|0",
            "sun|солнце|0",
            "moon|луна|0",
            "flower|цветок|0",
            "friend|друг|0",
            "cat|кошка|0"
        )
        file.writeText(baseWords.joinToString("\n"))
    }
}

private fun runFileParsing(wordFile: File, dictionary: MutableList<Word>) {
    try {
        val lines = wordFile.readLines()
        lines.map { it.split("|") }.forEach {
            val original = it[0]
            val translate = it[1]
            val correctAnswersCount = it.getOrNull(2)?.toIntOrNull() ?: 0
            dictionary.add(Word(original, translate, correctAnswersCount))
        }
    } catch (e: Exception) {
        println("Ошибка чтения файла: ${e::class.simpleName}")
    }
}

private fun String.capitalizeFirstChar(): String {
    return replaceFirstChar { it.uppercase() }
}

private fun getAnswerOptions(
    dictionary: List<Word>,
    unlearnedWords: List<Word>,
    wordToLearn: Word,
): List<String> {
    val allOptions = mutableSetOf(wordToLearn.translate.capitalizeFirstChar())
    allOptions.addAll(
        unlearnedWords
            .filter { it != wordToLearn }
            .shuffled()
            .take(FOUR_WORDS_FOR_ANSWER_OPTIONS - 1)
            .map { it.translate.capitalizeFirstChar() }
    )

    if (allOptions.size < FOUR_WORDS_FOR_ANSWER_OPTIONS) {
        val additionalOptions = dictionary
            .filter { it.correctAnswersCount == MAX_VALUE_LEARNED_WORD }
            .map { it.translate.capitalizeFirstChar() }
            .filterNot { allOptions.contains(it) }
            .shuffled()
            .take(FOUR_WORDS_FOR_ANSWER_OPTIONS - allOptions.size)
        allOptions.addAll(additionalOptions)
    }
    return allOptions.shuffled().toList()
}

private fun showAnswersWorldOptions(allAnswerWordsOptions: List<String>) =
    println(
        allAnswerWordsOptions.mapIndexed { index, word ->
            "${index + 1} - $word"
        }.joinToString(", ")
    )


private fun showScreenMenu(dictionary: List<Word>) {
    while (true) {
        showStartMenuText()

        when (readln().toIntOrNull()) {
            1 -> showLearningWords(dictionary)
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

private fun showStartMenuText() {
    println(
        """
        <-|Меню:
        <-|1 - Учить слова
        <-|2 - Статистика
        <-|0 - Выход
        <-|Введите необходимое значение:
    """.trimMargin("<-|")
    )
}

private fun showLearningWords(dictionary: List<Word>) {
    metka@ do {
        val unlearnedWords = dictionary.filter { it.correctAnswersCount < MAX_VALUE_LEARNED_WORD }
        if (unlearnedWords.isEmpty()) {
            println("Все слова выучены в базе!")
            break
        }

        val wordToLearn: Word = unlearnedWords.random()
        val originalWord: String = wordToLearn.original.capitalizeFirstChar()
        val translateWord: String = wordToLearn.translate.capitalizeFirstChar()
        val allAnswerWordsOptions: List<String> = getAnswerOptions(dictionary, unlearnedWords, wordToLearn)

        while (true) {
            println(originalWord)
            showAnswersWorldOptions(allAnswerWordsOptions)
            println("0. Выход")

            when (val input = readlnOrNull()?.toIntOrNull() ?: INCORRECT_VALUE) {
                0 -> break@metka
                in 1..allAnswerWordsOptions.size -> {
                    if (allAnswerWordsOptions[input - 1] == translateWord) {
                        wordToLearn.correctAnswersCount++
                        println("Правильно!")
                        break
                    } else {
                        println("Неправильно - слово [$translateWord]\n")
                        continue@metka
                    }
                }

                else -> println("Вы ввели некорректное значение, повторите попытку!")
            }
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
    creatingFileAndFillingItWithBasicWords(wordFile)

    val dictionary: MutableList<Word> = mutableListOf()
    runFileParsing(wordFile, dictionary)

    showScreenMenu(dictionary)
}