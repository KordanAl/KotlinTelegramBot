package org.example

data class Word(
    val questionWord: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
)

fun Question.asConsoleString(): String {
    val variants = this.variants
        .mapIndexed { index, word -> "${index + 1} - ${word.translate}" }
        .joinToString("\n")
    return this.correctAnswer.questionWord + "\n" + variants + "\n0 - выйти в меню"
}

fun main() {
    val trainer = try {
        LearnWordsTrainer(3, 4)
    } catch (e: Exception) {
        println("Невозможно загрузить словарь")
        return
    }

    while (true) {

        println("""
            <-|Меню:
            <-|1- Учить слова
            <-|2- Статистика
            <-|0 - Выход
        """.trimMargin("<-|"))

        when (readln().toIntOrNull()) {
            1 -> {
                while (true) {
                    val question = trainer.getNextQuestion()
                    if (question == null) {
                        println("Все слова выучены")
                        break
                    } else {
                        println(question.asConsoleString())

                        val userAnswerInput = readln().toIntOrNull()
                        if (userAnswerInput == 0) break

                        if (trainer.checkAnswer(userAnswerInput?.minus(1))) {
                            println("Правильно!\n")
                        } else {
                            println(
                                "Неправильно! ${question.correctAnswer.questionWord} - это" +
                                        " ${question.correctAnswer.translate}\n"
                            )
                        }
                    }
                }
            }

            2 -> {
                val statistics = trainer.getStatistics()
                println("Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percent}%")
            }

            0 -> break
            else -> println("Введите 1, 2 или 0")
        }
    }
}