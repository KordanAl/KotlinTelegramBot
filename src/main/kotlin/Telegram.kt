package org.example

private const val UPDATE_DELAY = 2000L

fun main(args: Array<String>) {

    val englishWordsLearningBot = TelegramBotService(botToken = args[0])

    while (true) {
        Thread.sleep(UPDATE_DELAY)
        val updatedData: UpdateData? = englishWordsLearningBot.getUpdates()
        if (updatedData != null) {
            println(updatedData)
            englishWordsLearningBot.sendMessage(updatedData.chatId, updatedData.text)
        }
    }
}