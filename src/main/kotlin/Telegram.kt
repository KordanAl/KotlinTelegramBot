package org.example

fun main(args: Array<String>) {

    val bot = try {
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
        val botUpdate = bot.getUpdates()

        if (botUpdate != null) {
            println(botUpdate)

            if (botUpdate.message.lowercase() == "/start") bot.sendMenu(botUpdate.chatId)

            when (botUpdate.callbackData.lowercase()) {

                LEARN_WORDS_BUTTON -> bot.getLastQuestions(bot, botTrainer, botUpdate).also { currentQuestion = it }

                STATISTICS_BUTTON -> {
                    val statistics = botTrainer.getStatistics()
                    bot.sendMessage(
                        botUpdate.chatId,
                        "$BICEPS Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percent}%"
                    )
                    bot.sendMenu(botUpdate.chatId)
                }

                else -> when{
                    botUpdate.callbackData.lowercase().startsWith(CALLBACK_DATA_ANSWER_PREFIX) -> {

                        currentQuestion?.let { it ->
                            bot.checkNextQuestionAndSend(botTrainer, botUpdate, it)
                            bot.getLastQuestions(bot, botTrainer, botUpdate).also { currentQuestion = it }
                        }
                    }

                    botUpdate.callbackData.lowercase() == BACK_TO_MENU_BUTTON -> bot.sendMenu(botUpdate.chatId)
                }
            }
        }
    }
}