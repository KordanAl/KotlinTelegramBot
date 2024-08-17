package org.example

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

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

    private val dictionary: MutableSet<Word> by lazy {
        loadDictionary().toMutableSet()
    }
    private val notLearnedWords: MutableSet<Word> by lazy {
        dictionary.filter { it.correctAnswersCount < maxValueLearnedCount }.toMutableSet()
    }
    private val learnedWords: MutableSet<Word> by lazy {
        dictionary.filter { it.correctAnswersCount >= maxValueLearnedCount }.toMutableSet()
    }
    // Функция получения статистики
    fun getStatistics(): Statistics {
        val learned = learnedWords.size
        val total = dictionary.size
        val percent = learned * ONE_HUNDRED_PERCENT / total
        return Statistics(learned, total, percent)
    }
    // Функция получения слово для изучения и 4-х вариантов ответов
    fun getNextQuestion(): Question? {
        if (notLearnedWords.isEmpty()) return null

        val shuffledNotLearned = notLearnedWords.shuffled()
        val questionWords = if (shuffledNotLearned.size < countOfQuestionsWords) {
            shuffledNotLearned.take(countOfQuestionsWords) +
                    learnedWords.shuffled().take(countOfQuestionsWords - shuffledNotLearned.size)
        } else {
            shuffledNotLearned.take(countOfQuestionsWords)
        }
        val correctAnswer = questionWords.random()
        question = Question(
            variants = questionWords.shuffled(),
            correctAnswer = correctAnswer
        )
        return question
    }

    fun checkAnswer(userAnswerIndex: Int?): Boolean {
        val correctAnswerIndex = question?.variants?.indexOf(question?.correctAnswer)
        return correctAnswerIndex != null && correctAnswerIndex == userAnswerIndex?.also {
            question?.correctAnswer?.correctAnswersCount?.let { count ->
                if (count >= maxValueLearnedCount) {
                    notLearnedWords.remove(question?.correctAnswer)

                    learnedWords.add(question?.correctAnswer!!)
                }
                saveDictionary(question?.correctAnswer!!)
            }
        }
    }

    private fun loadDictionary(): Set<Word> {
        val dictionary = mutableSetOf<Word>()
        try {
            File("words.txt").useLines { lines ->
                lines.forEach { word ->
                    val splitLine = word.split("|")
                    if (splitLine.size >= 2) {
                        dictionary.add(
                            Word(
                                splitLine[0].replaceFirstChar { it.uppercase() },
                                splitLine[1].replaceFirstChar { it.uppercase() },
                                splitLine.getOrNull(2)?.toIntOrNull() ?: 0
                            )
                        )
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            throw IllegalStateException("Файл не найден: ${e.message}")
        } catch (e: IOException) {
            throw IllegalStateException("Ошибка чтения файла: ${e.message}")
        } catch (e: Exception) {
            throw IllegalStateException("Неизвестная ошибка: ${e.message}")
        }
        return dictionary
    }

    private fun saveDictionary(word: Word) {
        val tempFile = File("temp_words.txt")
        val originalFile = File("words.txt")

        originalFile.useLines { lines ->
            tempFile.bufferedWriter().use { writer ->
                lines.forEach { line ->
                    val splitLine = line.split("|")
                    if (splitLine.size >= 2 && splitLine[0] == word.questionWord && splitLine[1] == word.translate) {
                        writer.write("${word.questionWord}|${word.translate}|${word.correctAnswersCount}\n")
                    } else {
                        writer.write(line + "\n")
                    }
                }
            }
        }
        tempFile.renameTo(originalFile)
    }
}