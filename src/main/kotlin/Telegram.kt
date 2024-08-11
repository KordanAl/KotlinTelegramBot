package org.example

private const val REQUEST_DELAY = 2000L

private fun showDataInConsole(nameData: String, data: String?) {
    if (data != null) println("->[${nameData.replaceFirstChar { it.uppercase() }}: $data]<-")
}

fun main(args: Array<String>) {

    val telegram = TelegramBotService(botToken = args[0])

    while (true) {
        Thread.sleep(REQUEST_DELAY)
        val updates: String = telegram.getUpdates()
        println(updates)

        if (telegram.resultUpdateId == -1) continue

        val updateId: String? = telegram.getUpdateId(updates)
        if (updateId != null) telegram.resultUpdateId = updateId.toInt() + 1
        showDataInConsole("updateId", updateId)

        val chatId: String? = telegram.getChatId(updates)
        showDataInConsole("chatId", chatId)

        val text: String? = telegram.getText(updates)
        showDataInConsole("text", text)

        if (chatId != null && text != null) telegram.sendMessage(chatId, text)
    }
}