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
        lines.map { it.split("|") }.forEach { word ->
            val original = word[0].replaceFirstChar { it.uppercase() }
            val translate = word[1].replaceFirstChar { it.uppercase() }
            val correctAnswersCount = word.getOrNull(2)?.toIntOrNull() ?: 0
            dictionary.add(Word(original, translate, correctAnswersCount))
        }
    } catch (e: Exception) {
        println("Ошибка чтения файла: ${e::class.simpleName}")
    }
}

private fun getAnswerOptions(
    dictionary: List<Word>,
    unlearnedWords: List<Word>,
    wordToLearn: Word,
): List<Word> {
    val allOptions = mutableSetOf(wordToLearn)
    allOptions.addAll(
        unlearnedWords
            .filter { it != wordToLearn }
            .shuffled()
            .take(FOUR_WORDS_FOR_ANSWER_OPTIONS - 1)
    )

    if (allOptions.size < FOUR_WORDS_FOR_ANSWER_OPTIONS) {
        val additionalOptions = dictionary
            .filter { it.correctAnswersCount == MAX_VALUE_LEARNED_WORD }
            .filterNot { allOptions.contains(it) }
            .shuffled()
            .take(FOUR_WORDS_FOR_ANSWER_OPTIONS - allOptions.size)
        allOptions.addAll(additionalOptions)
    }
    return allOptions.shuffled().toList()
}

private fun showAnswersWorldOptions(allAnswerWordsOptions: List<Word>) =
    println(
        allAnswerWordsOptions.mapIndexed { index, word ->
            "${index + 1} - ${word.translate}"
        }.joinToString(", ")
    )

private fun showScreenMenu(wordFile: File, dictionary: List<Word>) {
    while (true) {
        showStartMenuText()

        when (readln().toIntOrNull()) {
            1 -> showLearningWords(wordFile, dictionary)
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

private fun showLearningWords(wordFile: File, dictionary: List<Word>) {
    do {
        val unlearnedWords = dictionary.filter { it.correctAnswersCount < MAX_VALUE_LEARNED_WORD }
        if (unlearnedWords.isEmpty()) {
            println("Все слова выучены в базе!")
            break
        }

        val wordToLearn: Word = unlearnedWords.random()
        val allAnswerWordsOptions: List<Word> = getAnswerOptions(dictionary, unlearnedWords, wordToLearn)

        println(wordToLearn.original)
        showAnswersWorldOptions(allAnswerWordsOptions)
        println("0 - Выход в меню")

        when (val input = readlnOrNull()?.toIntOrNull() ?: INCORRECT_VALUE) {
            0 -> break
            in 1..allAnswerWordsOptions.size -> {
                if (allAnswerWordsOptions.map { it.translate }[input - 1] == wordToLearn.translate) {
                    wordToLearn.correctAnswersCount++
                    println("Правильно!\n")
                    saveDictionary(wordFile, dictionary)
                    continue
                } else {
                    println("Неправильно - слово [${wordToLearn.translate}]\n")
                }
            }

            else -> println("Вы ввели некорректное значение!")
        }

    } while (true)
}

private fun showStatisticInfo(dictionary: List<Word>) {
    val wordsLearned: Int = dictionary.filter { it.correctAnswersCount >= MAX_VALUE_LEARNED_WORD }.size
    val allWords: Int = dictionary.size
    val percentOfWordsLearned: Int = (wordsLearned.toDouble() / allWords * ONE_HUNDRED_PERCENT).toInt()

    println("Вы выучили $wordsLearned из $allWords слов | $percentOfWordsLearned%")
}

private fun saveDictionary(file: File, dictionary: List<Word>) {
    try {
        file.writeText(
            dictionary.joinToString("\n") {
            "${it.original}|${it.translate}|${it.correctAnswersCount}"
            }
        )
    } catch (e: Exception) {
        println("Ошибка записи файла: ${e::class.simpleName}")
    }
}

fun main() {
    val wordFile = File("words.txt")
    creatingFileAndFillingItWithBasicWords(wordFile)

    val dictionary: MutableList<Word> = mutableListOf()
    runFileParsing(wordFile, dictionary)

    showScreenMenu(wordFile, dictionary)
}