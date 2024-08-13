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

    while (true) {
        Thread.sleep(UPDATE_DELAY)
        val botUpdate = telegramBot.getUpdates()
        if (botUpdate != null) {
            println(botUpdate)

            if (botUpdate.text.lowercase() == "/start") telegramBot.sendMenu(botUpdate.chatId)
            // Сделал when заранее, так будет 2 кнопка
            when (botUpdate.data.lowercase()) {

                STATISTICS_BUTTON -> telegramBot.sendMessage(
                    botUpdate.chatId,
                    "Выучено 10 из 10 слов | 100%"
                )

            }
        }
    }
}