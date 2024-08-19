package org.example

import java.io.File
import java.lang.IllegalStateException

private const val ONE_HUNDRED_PERCENT = 100

data class Word(
    val questionWord: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
)

data class Statistics(
    val learned: Int,
    val total: Int,
    val percent: Int,
)

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word,
)

class LearnWordsTrainer(
    private val maxValueLearnedCount: Int = 3,
    private val countOfQuestionsWords: Int = 4,
) {
    private var question: Question? = null
    private val dictionary = loadDictionary()

    fun getStatistics(): Statistics {
        val learned = dictionary.filter { it.correctAnswersCount >= maxValueLearnedCount }.size
        val total = dictionary.size
        val percent = learned * ONE_HUNDRED_PERCENT / total
        return Statistics(learned, total, percent)
    }

    fun getNextQuestion(): Question? {
        val notLearnedList: List<Word> = dictionary.filter { it.correctAnswersCount < maxValueLearnedCount }
        if (notLearnedList.isEmpty()) return null
        val questionWords = if (notLearnedList.size < countOfQuestionsWords) {
            val learnedList = dictionary.filter { it.correctAnswersCount >= maxValueLearnedCount }.shuffled()
            notLearnedList
                .shuffled()
                .take(countOfQuestionsWords) + learnedList.take(countOfQuestionsWords - notLearnedList.size)
        } else {
            notLearnedList.shuffled().take(countOfQuestionsWords)
        }.shuffled()
        val correctAnswer = questionWords.random()
        question = Question(
            variants = questionWords,
            correctAnswer = correctAnswer,
        )
        return question

    }

    fun checkAnswer(userAnswerIndex: Int?): Boolean {
        return question?.let {
            val correctAnswerId = it.variants.indexOf(it.correctAnswer)
            if (correctAnswerId == userAnswerIndex) {
                it.correctAnswer.correctAnswersCount++
                saveDictionary(dictionary)
                true
            } else {
                false
            }
        } ?: false
    }

    private fun loadDictionary(): List<Word> {
        try {
            val dictionary = mutableListOf<Word>()
            val wordFile = File("words.txt")
            wordFile.readLines().forEach { word ->
                val splitLine = word.split("|")
                dictionary.add(
                    Word(
                        splitLine[0].replaceFirstChar { it.uppercase() },
                        splitLine[1].replaceFirstChar { it.uppercase() },
                        splitLine[2].toIntOrNull() ?: 0
                    )
                )
            }
            return dictionary
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalStateException("Некорректный файл")
        }
    }

    private fun saveDictionary(words: List<Word>) {
        try {
            val wordsFile = File("words.txt")
            wordsFile.writeText("")
            for (word in words) {
                wordsFile.appendText("${word.questionWord}|${word.translate}|${word.correctAnswersCount}\n")
            }
        } catch (e: Exception) {
            println("Ошибка записи файла: ${e::class.simpleName}")
        }
    }

}