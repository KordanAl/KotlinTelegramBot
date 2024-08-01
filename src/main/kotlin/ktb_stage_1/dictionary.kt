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

private fun getAnswerOptions(
    dictionary: List<Word>,
    listOfUnlearnedWords: List<Word>,
    randomObjectWordToLearn: Word,
    translateOriginalWordToLearn: String,
): List<String> {
    val allAnswerWordsOptions = mutableSetOf<String>().apply {
        add(translateOriginalWordToLearn)
        addAll(
            listOfUnlearnedWords
                .filter { it != randomObjectWordToLearn }
                .shuffled()
                .take(FOUR_WORDS_FOR_ANSWER_OPTIONS - 1)
                .map { it.translate.capitalize() }
                .toMutableSet()
        )
    }

    if (allAnswerWordsOptions.size < FOUR_WORDS_FOR_ANSWER_OPTIONS) {
        val additionalAnswerOptions = dictionary
            .filter { it.correctAnswersCount == MAX_VALUE_LEARNED_WORD }
            .map { it.translate.capitalize() }
            .filterNot { allAnswerWordsOptions.contains(it) }
            .shuffled()
            .take(FOUR_WORDS_FOR_ANSWER_OPTIONS - allAnswerWordsOptions.size)

        allAnswerWordsOptions.addAll(additionalAnswerOptions)
    }
    return allAnswerWordsOptions.toList().shuffled()
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
        val listOfUnlearnedWords = dictionary.filter { it.correctAnswersCount < MAX_VALUE_LEARNED_WORD }
        if (listOfUnlearnedWords.isEmpty()) {
            println("Все слова выучены в базе!")
            break
        }

        val randomObjectWordToLearn: Word = listOfUnlearnedWords.random()
        val originalWordToLearn: String = randomObjectWordToLearn.original.capitalize()
        val translateOriginalWordToLearn: String = randomObjectWordToLearn.translate.capitalize()

        val allAnswerWordsOptions = getAnswerOptions(
            dictionary, listOfUnlearnedWords, randomObjectWordToLearn, translateOriginalWordToLearn,
        )

        do {
            println(originalWordToLearn)
            showAnswersWorldOptions(allAnswerWordsOptions)
            println("0. Выход")

            val inputNumber = readln().toIntOrNull() ?: INCORRECT_VALUE
            if (inputNumber == 0) break@metka

            if (inputNumber in (1..allAnswerWordsOptions.size)) {
                if (allAnswerWordsOptions[inputNumber - 1] == translateOriginalWordToLearn) {
                    randomObjectWordToLearn.correctAnswersCount++
                    println("Правильно!")
                    break
                } else {
                    println("Неправильно - слово [$translateOriginalWordToLearn]\n")
                    continue@metka
                }
            } else {
                println("Вы ввели некорректное значение, повторите попытку!")
            }
        } while (true)

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
