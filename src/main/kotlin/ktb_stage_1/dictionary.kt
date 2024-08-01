package ktb_stage_1

import java.io.File

private const val ONE_HUNDRED_PERCENT = 100
private const val MAX_VALUE_LEARNED_WORD = 3
private const val THREE_WORDS_FOR_ANSWER_OPTIONS = 3
private const val FOUR_WORDS_FOR_ANSWER_OPTIONS = 4

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

private fun String.capitalize(): String {
    return replaceFirstChar { it.uppercase() }
}

private fun showScreenMenu(dictionary: List<Word>) {
    while (true) {
        println(
            """
        Меню:
        1 - Учить слова
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
            println("Все слова выучены в базе!")
            break
        }
        val randomObjectWordToLearn: Word = listOfUnlearnedWords.random()

        val originalWordToLearn: String = randomObjectWordToLearn.original
        val translateOriginalWordToLearn: String = randomObjectWordToLearn.translate

        val wordAnswerOptions = getAllAnswerOptions(
            dictionary, listOfUnlearnedWords, randomObjectWordToLearn, translateOriginalWordToLearn
        )

        println(originalWordToLearn.capitalize())
        val outputAnswers: String = wordAnswerOptions.mapIndexed { index, word ->
            "${index + 1} - ${word.capitalize()}"
        }.joinToString(", ")
        println(outputAnswers)
        println("0. Выход")

        val inputNumber: Int = readln().toIntOrNull() ?: 0
        if (inputNumber == 0) break

    } while (true)
}

private fun getAllAnswerOptions(
    dictionary: List<Word>,
    listOfUnlearnedWords: List<Word>,
    randomObjectWordToLearn: Word,
    translateOriginalWordToLearn: String,
): List<String> {
    val unlearnedOptionsWords: List<Word> = listOfUnlearnedWords.filter { it != randomObjectWordToLearn }
    val initialOptionsWords: List<Word> = if (unlearnedOptionsWords.size < THREE_WORDS_FOR_ANSWER_OPTIONS) {
        unlearnedOptionsWords + dictionary
            .filter { it.correctAnswersCount >= MAX_VALUE_LEARNED_WORD && it != randomObjectWordToLearn }
            .shuffled()
            .take(THREE_WORDS_FOR_ANSWER_OPTIONS - unlearnedOptionsWords.size)
    } else {
        unlearnedOptionsWords.shuffled().take(THREE_WORDS_FOR_ANSWER_OPTIONS)
    }

    val wordAnswerOptions = mutableSetOf<String>()
    wordAnswerOptions.add(translateOriginalWordToLearn)
    wordAnswerOptions.addAll(initialOptionsWords.shuffled().map { it.translate })

    while (wordAnswerOptions.size < FOUR_WORDS_FOR_ANSWER_OPTIONS) {
        val additionalWords = dictionary
            .filter { it.correctAnswersCount >= MAX_VALUE_LEARNED_WORD && it.translate != translateOriginalWordToLearn }
            .shuffled()
        for (word in additionalWords) {
            wordAnswerOptions.add(word.translate)
            if (wordAnswerOptions.size == FOUR_WORDS_FOR_ANSWER_OPTIONS) break
        }
    }
    return wordAnswerOptions.shuffled().toList()
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