package org.example

import java.io.File

private const val ONE_HUNDRED_PERCENT = 100
private const val MAX_VALUE_LEARNED_WORD = 3
private const val FOUR_WORDS = 4

data class Statistics (
    val learned: Int,
    val total: Int,
    val percent: Int,
)

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word,
)

class LearnWordsTrainer {
    private var question: Question? = null
    private val dictionary = loadDictionary()

    fun getStatistics(): Statistics {
        val learned = dictionary.filter { it.correctAnswersCount >= MAX_VALUE_LEARNED_WORD }.size
        val total = dictionary.size
        val percent = learned * ONE_HUNDRED_PERCENT / total
        return Statistics(learned, total, percent,)
    }

    fun getNextQuestion(): Question? {
        val notLearnedList: List<Word> = dictionary.filter { it.correctAnswersCount < MAX_VALUE_LEARNED_WORD }
        if (notLearnedList.isEmpty()) return null
        val questionWords: MutableList<Word> = notLearnedList.take(FOUR_WORDS).shuffled().toMutableList()
        val correctAnswer = questionWords.random()

        // Проверка на случай если невыученных слов будет меньше 4, то мы берем варианты из уже выученных слов
        if (questionWords.count() < FOUR_WORDS) {
            val additionalAnswers = dictionary
                .filterNot { it.correctAnswersCount < MAX_VALUE_LEARNED_WORD }
                .take(FOUR_WORDS - questionWords.count())
                .shuffled()
            questionWords.addAll(additionalAnswers)
        }

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
        val dictionary = mutableListOf<Word>()
        val wordFile = File("words.txt")
        if (!wordFile.exists()) {
            wordFile.createNewFile()
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
            wordFile.writeText(baseWords.joinToString("\n"))
        }

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




