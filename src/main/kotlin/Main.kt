package org.example

fun Question.asConsoleString(): String {
    val variants = this.variants
        .mapIndexed { index, word -> "${index + 1} - ${word.translate}" }
        .joinToString("\n")
    return this.correctAnswer.questionWord + "\n" + variants + "\n0 - выйти в меню"
}

fun main() {
    val consoleTrainer = try {
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
                    val question = consoleTrainer.getNextQuestion()
                    if (question == null) {
                        println("Все слова выучены")
                        break
                    } else {
                        println(question.asConsoleString())

                        val userAnswerInput = readln().toIntOrNull()
                        if (userAnswerInput == 0) break

                        if (consoleTrainer.checkAnswer(userAnswerInput?.minus(1))) {
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
                val statistics = consoleTrainer.getStatistics()
                println("Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percent}%")
            }

            0 -> break
            else -> println("Введите 1, 2 или 0")
        }
    }
}