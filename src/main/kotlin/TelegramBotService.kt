import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@Serializable
data class Update(
    @SerialName("update_id")
    val updateId: Long,
    @SerialName("message")
    val message: Message? = null,
    @SerialName("callback_query")
    val callbackQuery: CallbackQuery? = null,
) {
    companion object ANSI {
        private const val ANSI_GREEN = "\u001B[32m"
        private const val ANSI_RESET = "\u001B[0m"
    }

    override fun toString(): String {
        return """
            $ANSI_GREEN
            <-|UpdateId = [$updateId]
            <-|Message = [$message]
            <-|CallbackQuery = $callbackQuery
            $ANSI_RESET
        """.trimMargin("<-|")
    }
}

@Serializable
data class Message(
    @SerialName("text")
    val text: String,
    @SerialName("chat")
    val chat: Chat,
    @SerialName("message_id")
    val messageId: Long,
) {
    override fun toString(): String {
        return """
            <-|messageId = [$messageId]
            <-|CallbackQuery = chat = [$chat]
            <-|CallbackQuery = text = [$text]
        """.trimMargin("<-|")
    }
}

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String? = null,
    @SerialName("message")
    val message: Message? = null,
) {
    override fun toString(): String {
        return """
            <-|data = [$data]
            <-|CallbackQuery = message = [$message]
        """.trimMargin("<-|")
    }
}

@Serializable
data class Chat(
    @SerialName("id")
    val id: Long,
)

@Serializable
data class Response(
    @SerialName("result")
    val result: List<Update>,
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id")
    val chatId: Long,
    @SerialName("text")
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup? = null,
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboard>>,
)

@Serializable
data class InlineKeyboard(
    @SerialName("callback_data")
    val callbackData: String,
    @SerialName("text")
    val text: String,
)

data class TelegramBotService(
    private val botToken: String,
    private var lastUpdateId: Long = 0L,
) {
    companion object Constants {
        private const val BOOK = "\uD83D\uDCDA"
        private const val BICEPS = "\uD83D\uDCAA"
        private const val DOOR = "\uD83D\uDEAA"
        private const val STATISTIC = "\uD83D\uDCC8"
        private const val RELOAD = "\uD83D\uDD04"
        private const val CHECK_RIGHT = "\uD83D\uDC49"
        private const val CHECK_LEFT = "\uD83D\uDC48"
        private const val GREEN_CHECK = "✅"
        private const val DOWNLOAD = "\uD83D\uDD24"
        private const val STAR = "\uD83D\uDCAB"

        private const val LEARN_WORDS_BUTTON = "learn_words_clicked"
        private const val STATISTICS_BUTTON = "statistics_clicked"
        private const val RESET_BUTTON = "reset_statistic_clicked"
        private const val BACK_TO_MENU_BUTTON = "back_to_menu_clicked"
        private const val CALLBACK_DATA_ANSWER_ = "answer_"

        private const val BASE_URL = "https://api.telegram.org"
    }

    private val trainers = HashMap<Long, LearnWordsTrainer>()
    private val json = Json { ignoreUnknownKeys = true }

    // Функция получения актуального тренера для каждого пользователя
    private fun getUpdateTrainer(chatId: Long) = trainers.getOrPut(chatId) {
        LearnWordsTrainer("${chatId}.txt")
    }

    // Функция получения обновления и данных если они пришли с ответом.
    fun getUpdates() {
        val responseString: String? = getResponse(getUpdatesUrl())?.body()
        if (responseString == null) {
            println("Ошибка: не удалось получить обновления!")
            return
        }

        val response: Response = json.decodeFromString(responseString)
        if (response.result.isEmpty()) {
            println("Нет новых обновлений!")
            return
        }

        val sortedUpdates = response.result.sortedBy { it.updateId }
        sortedUpdates.forEach { update ->
            println("Полученное обновление: $responseString")
            handleUpdate(update)
            println(update)
        }
        lastUpdateId = sortedUpdates.last().updateId + 1

    }

    // Функции получения данных
    private fun getChatId(update: Update) = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id
    private fun getMessageId(update: Update) = update.callbackQuery?.message?.messageId
    private fun getMessage(update: Update) = update.message?.text
    private fun getCallbackData(update: Update) = update.callbackQuery?.data

    // Функция для обработки обновления и вывода соответствующей информации для каждого пользователя
    private fun handleUpdate(update: Update) {
        val chatId = getChatId(update) ?: return
        val messageId = getMessageId(update)
        val message = getMessage(update)
        val callbackData = getCallbackData(update)

        val trainer = getUpdateTrainer(chatId)

        if (message?.lowercase() == "/start") handleStartCommand(chatId)

        callbackData?.let {
            if (messageId != null) {
                handleCallbackQuery(trainer, chatId, it, messageId)
            }
        }
    }

    // Функция вывода стартовой информации
    private fun handleStartCommand(chatId: Long) {
        sendMessage(chatId, "Привет!")
        sendMenu(chatId)
    }

    // Функция обработки нажатой кнопки
    private fun handleCallbackQuery(
        trainer: LearnWordsTrainer,
        chatId: Long,
        callbackData: String,
        messageId: Long,
    ) {
        when (callbackData.lowercase()) {

            LEARN_WORDS_BUTTON -> {
                deleteMessage(chatId, listOf(messageId - 1, messageId))
                checkNextQuestionAndSend(trainer, chatId)
            }

            STATISTICS_BUTTON -> {
                deleteMessage(chatId, listOf(messageId - 1, messageId))
                getStatisticAndSend(trainer, chatId)
            }

            RESET_BUTTON -> {
                deleteMessage(chatId, listOf(messageId - 1, messageId))
                resetProgressAndSend(trainer, chatId)
            }

            BACK_TO_MENU_BUTTON -> {
                deleteMessage(chatId, listOf(messageId - 1, messageId))
                sendMenu(chatId)
            }
        }

        if (callbackData.lowercase().startsWith(CALLBACK_DATA_ANSWER_)) {
            deleteMessage(chatId, listOf(messageId - 1, messageId))
            checkCallbackDataAndSend(trainer, chatId, callbackData)
            checkNextQuestionAndSend(trainer, chatId)
        }
    }

    // Функция вывода статистики изучения слов
    private fun getStatisticAndSend(trainer: LearnWordsTrainer, chatId: Long) {
        val statistics = trainer.getStatistics()
        sendMessage(
            chatId,
            "$BICEPS Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percent}%"
        )
        sendMenu(chatId)
    }

    // Функция сброса прогресса изученных слов
    private fun resetProgressAndSend(trainer: LearnWordsTrainer, chatId: Long) {
        trainer.resetProgress()
        sendMessage(chatId, "Прогресс сброшен")
        sendMenu(chatId)
    }

    // Функция проверки правильного ответа в нажатой кнопке
    private fun checkCallbackDataAndSend(trainer: LearnWordsTrainer, chatId: Long, callbackData: String) {
        val answerId = callbackData.substringAfter(CALLBACK_DATA_ANSWER_).toInt()
        if (trainer.checkAnswer(answerId)) {
            sendMessage(chatId, "Правильно!")
        } else {
            sendMessage(
                chatId,
                "Неправильно! ${trainer.question?.correctAnswer?.questionWord} - " +
                        "это ${trainer.question?.correctAnswer?.translate}"
            )
        }
    }

    // Функция отправки сообщения пользователю в чате с ботом.
    private fun sendMessage(chatId: Long, text: String): String? {
        val encodText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
        val responseBody = getResponse(getSendMessageUrl(chatId, encodText))?.body()
        if (responseBody == null) println("Ошибка: не удалось отправить сообщение!")
        return responseBody
    }

    //Функция удаления сообщения бота, если нужно корректировать messageId
    private fun deleteMessage(chatId: Long, listMessageId: List<Long>) {
        listMessageId.forEach { messageId ->
            val response = getResponse(getDeleteMessageUrl(chatId, messageId))
            if (response?.body() == null) {
                println("Ошибка: не удалось удалить сообщение с ID $messageId!")
            }
        }
    }

    // Функция вывода основного меню пользователю в чате с ботом.
    private fun sendMenu(chatId: Long): String? {
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = """
                $DOWNLOAD Загрузка словаря...
                $GREEN_CHECK Словарь загружен!
                
                $STAR Добро пожаловать в основное меню!
            """.trimIndent(),
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyboard(text = "$BOOK Изучать слова", callbackData = LEARN_WORDS_BUTTON),
                        InlineKeyboard(text = "$STATISTIC Статистика", callbackData = STATISTICS_BUTTON),
                    ),
                    listOf(
                        InlineKeyboard(text = "$RELOAD Сбросить прогресс", callbackData = RESET_BUTTON)
                    )
                )
            )
        )
        val requestBodyString = json.encodeToString(requestBody)
        val responseBody = getResponse(getSendMessagePostUrl(), requestBodyString)?.body()

        if (responseBody == null) {
            println("Ошибка: не удалось отправить меню!")
        }
        return responseBody
    }

    // Проверка на правильный ответ и отправка соответствующего сообщения пользователю в чате с ботом.
    private fun checkNextQuestionAndSend(trainer: LearnWordsTrainer, chatId: Long) {
        val question = trainer.getNextQuestion()
        if (question == null) {
            sendMessage(chatId, "Все слова выучены.")
        } else {
            sendQuestion(chatId, question)
        }
    }

    // Функция отправки Слова для изучения и его вариантов ответов
    private fun sendQuestion(chatId: Long, question: Question): String? {
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = "Переведи слово: $CHECK_RIGHT ${question.correctAnswer.questionWord} $CHECK_LEFT",
            replyMarkup = ReplyMarkup(
                listOf(
                    question.variants.mapIndexed { index, variant ->
                        InlineKeyboard(
                            text = variant.translate, callbackData = "$CALLBACK_DATA_ANSWER_$index"
                        )
                    },
                    listOf(
                        InlineKeyboard(
                            text = "$DOOR Выход в меню", callbackData = BACK_TO_MENU_BUTTON
                        )
                    )
                )
            )
        )
        val requestBodyString = json.encodeToString(requestBody)
        return getResponse(getSendMessagePostUrl(), requestBodyString)?.body()
    }

    //Функция получения url отправки сообщения get
    private fun getSendMessageUrl(chatId: Long, text: String?): String {
        return "$BASE_URL/bot$botToken/sendMessage?chat_id=${chatId}&text=${text}"
    }

    //Функция получения url удаления сообщения
    private fun getDeleteMessageUrl(chatId: Long, messageId: Long): String {
        return "$BASE_URL/bot$botToken/deleteMessage?chat_id=${chatId}&message_id=${messageId}"
    }

    //Функция получения url обновления
    private fun getUpdatesUrl(): String {
        return "$BASE_URL/bot$botToken/getUpdates?offset=$lastUpdateId"
    }

    //Функция получения url отправки сообщения post
    private fun getSendMessagePostUrl(): String {
        return "$BASE_URL/bot$botToken/sendMessage"
    }

    //Функция получения ответа от сервера
    private fun getResponse(urlGetUpdates: String): HttpResponse<String>? {
        return try {
            val client: HttpClient = HttpClient.newBuilder().build()
            val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            println("Ошибка при отправке запроса: ${e.message}")
            null
        }
    }

    //Перегруженная функция получения ответа от сервера используя POST-запрос
    private fun getResponse(urlGetUpdates: String, str: String): HttpResponse<String>? {
        return try {
            val client: HttpClient = HttpClient.newBuilder().build()
            val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates))
                .header("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(str))
                .build()
            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            println("Ошибка при отправке POST-запроса: ${e.message}")
            null
        }
    }
}