private const val UPDATE_DELAY = 2000L

fun main(args: Array<String>) {

    val bot = try {
        TelegramBotService(botToken = args[0])
    } catch (e: Exception) {
        println("Бот токен не найден!")
        return
    }

    try {
        while (true) {
            Thread.sleep(UPDATE_DELAY)
            bot.getUpdates()
        }
    } catch (e: Exception) {
        println("Нет ответа от Telegram!")
        return
    }
}
