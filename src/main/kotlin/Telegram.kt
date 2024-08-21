private const val UPDATE_DELAY = 2000L

fun main(args: Array<String>) {
    val bot = try {
        TelegramBotService(botToken = args[0])
    } catch (e: Exception) {
        println("Бот токен не найден!")
        return
    }

    while (true) {
        Thread.sleep(UPDATE_DELAY)
        val update = bot.getUpdates() ?: continue
        println(update)
    }
}
