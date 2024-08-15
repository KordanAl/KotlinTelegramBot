package org.example

private const val UPDATE_DELAY = 2000L

fun main(args: Array<String>) {

    val telegramBot = try {
        TelegramBotService(botToken = args[0])
    } catch (e: Exception) {
        println("Бот токен не найден!")
        return
    }

    val botTrainer = try {
        LearnWordsTrainer(3, 4)
    } catch (e: Exception) {
        println("Невозможно загрузить словарь!")
        return
    }

    var currentQuestion: Question? = null

    while (true) {
        Thread.sleep(UPDATE_DELAY)
        val botUpdate = telegramBot.getUpdates()

        if (botUpdate != null) {
            println(botUpdate)

            if (botUpdate.text.lowercase() == "/start") telegramBot.sendMenu(botUpdate.chatId)

            when (botUpdate.data.lowercase()) {

                LEARN_WORDS_BUTTON -> {
                    telegramBot.startProcessingNewQuestion(botTrainer,telegramBot,botUpdate)
                        .also { currentQuestion = it }
                }

                STATISTICS_BUTTON -> {
                    val statistics = botTrainer.getStatistics()
                    telegramBot.sendMessage(
                        botUpdate.chatId,
                        "Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percent}%"
                    )
                }

                else -> if (CALLBACK_DATA_ANSWER_PREFIX in botUpdate.data.lowercase()) {
                    currentQuestion?.let { it ->
                        telegramBot.checkNextQuestionAndSend(botTrainer, botUpdate, it)
                        telegramBot.startProcessingNewQuestion(botTrainer, telegramBot, botUpdate)
                            .also { currentQuestion = it }
                    }
                }
            }
        }
    }
}