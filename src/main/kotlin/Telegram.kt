package org.example

private const val UPDATE_DELAY = 2000L

fun main(args: Array<String>) {

    val bot = TelegramBotService(botToken = args[0])

    val botTrainer = try {
        LearnWordsTrainer(3, 4)
    } catch (e: Exception) {
        println("Невозможно загрузить словарь")
        return
    }

    while (true) {
        Thread.sleep(UPDATE_DELAY)
        val data: UpdateData? = bot.getUpdates()
        if (data != null) {

            println(data)

            when (data.text.lowercase()) {

                "/start" -> bot.sendMenu(data.chatId)

                "hello" -> bot.sendMessage(data.chatId, "Hello")

            }

            when (data.data.lowercase()) {

                "statistics_clicked" -> bot.sendMessage(data.chatId, "Выучено 10 из 10 слов | 100%")
            }
        }
    }
}