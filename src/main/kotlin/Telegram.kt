package org.example

private const val UPDATE_DELAY = 2000L

fun main(args: Array<String>) {

    val telegram = TelegramBotService(botToken = args[0])

    while (true) {
        Thread.sleep(UPDATE_DELAY)
        val data: UpdateData? = telegram.getUpdatesData()
        if (data != null) {
            println(data)
            telegram.sendMessage(data.chatId, data.text)
        }
    }
}
