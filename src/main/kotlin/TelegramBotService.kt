package org.example

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
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String,
    @SerialName("chat")
    val chat: Chat,
    @SerialName("message_id")
    val messageId: Long,
)

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String? = null,
    @SerialName("message")
    val message: Message? = null,
)

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

data class UpdateData(
    val updateId: Long,
    val chatId: Long,
    val messageId: Long,
    val message: String,
    val callbackData: String,
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
        private const val DARTS = "\uD83C\uDFAF"
        private const val CHECK_RIGHT = "\uD83D\uDC49"
        private const val CHECK_LEFT = "\uD83D\uDC48"
        private const val GREEN_CHECK = "✅"

        private const val LEARN_WORDS_BUTTON = "learn_words_clicked"
        private const val STATISTICS_BUTTON = "statistics_clicked"
        private const val RESET_BUTTON = "reset_statistic_clicked"
        private const val BACK_TO_MENU_BUTTON = "back_to_menu_clicked"
        private const val CALLBACK_DATA_ANSWER_ = "answer_"

        private const val BASE_URL = "https://api.telegram.org"
    }

    private val trainers = HashMap<Long, LearnWordsTrainer>()
    private var currentQuestion: Question? = null
    private val json = Json { ignoreUnknownKeys = true }

    // Функция получения актуального тренера для каждого пользователя
    private fun getUpdateTrainer(update: UpdateData) = trainers.getOrPut(update.chatId) {
        LearnWordsTrainer("${update.chatId}.txt")
    }

    // Функция получения обновления и данных если они пришли с ответом.
    fun getUpdates(): UpdateData? {
        val urlGetUpdates = "$BASE_URL/bot$botToken/getUpdates?offset=$lastUpdateId"
        val responseString: String = getResponse(urlGetUpdates).body()
        println("Response from getUpdates: $responseString")

        val response: Response = json.decodeFromString(responseString)
        if (response.result.isEmpty()) return null

        val sortedUpdates = response.result.sortedBy { it.updateId }
        val firstUpdate = sortedUpdates.firstOrNull() ?: return null

        val chatId = firstUpdate.message?.chat?.id ?: firstUpdate.callbackQuery?.message?.chat?.id
        ?: return null

        lastUpdateId = sortedUpdates.last().updateId + 1

        return UpdateData(
            updateId = sortedUpdates.last().updateId,
            chatId = chatId,
            messageId = firstUpdate.callbackQuery?.message?.messageId ?: 0L,
            message = firstUpdate.message?.text ?: "",
            callbackData = firstUpdate.callbackQuery?.data ?: "",
        )
    }

    // Функция для обработки обновления и вывода соответствующей информации для каждого пользователя
    fun handleUpdate(bot: TelegramBotService, update: UpdateData) {

        if (update.message.lowercase() == "/start") {
            bot.sendMessage(update, "Привет!")
            bot.sendMenu(update)
        }
        when (update.callbackData.lowercase()) {

            LEARN_WORDS_BUTTON -> {
                deleteMessage(update, listOf(update.messageId - 1, update.messageId))
                currentQuestion = bot.getLastQuestions(bot, update)
                if (currentQuestion == null) deleteMessage(
                    update, listOf(update.messageId - 2, update.messageId - 1, update.messageId)
                )
            }

            STATISTICS_BUTTON -> {
                deleteMessage(update, listOf(update.messageId - 1, update.messageId))
                handleStatisticsButton(bot, update)
            }

            RESET_BUTTON -> {
                deleteMessage(update, listOf(update.messageId - 1, update.messageId))
                handleResetButton(bot, update)
            }

            BACK_TO_MENU_BUTTON -> {
                deleteMessage(update, listOf(update.messageId - 1, update.messageId))
                bot.sendMenu(update)
            }
        }

        if (update.callbackData.lowercase().startsWith(CALLBACK_DATA_ANSWER_)) {
            deleteMessage(update, listOf(update.messageId - 1, update.messageId))
            handleAnswer(bot, update)
        }
    }

    // Функция вывода статистики
    private fun handleStatisticsButton(bot: TelegramBotService, update: UpdateData) {
        val trainer = getUpdateTrainer(update)
        val statistics = trainer.getStatistics()
        bot.sendMessage(
            update,
            "$BICEPS Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percent}%"
        )
        bot.sendMenu(update)
    }

    //Функция сброса прогресса
    private fun handleResetButton(bot: TelegramBotService, update: UpdateData) {
        val trainer = getUpdateTrainer(update)
        trainer.resetProgress()
        sendMessage(update, "Прогресс сброшен")
        bot.sendMenu(update)
    }

    //функция проверки ответов
    private fun handleAnswer(bot: TelegramBotService, update: UpdateData) {
        currentQuestion?.let { question ->
            bot.checkNextQuestionAndSend(update, question)
            currentQuestion = bot.getLastQuestions(bot, update)
        }
    }

    // Функция отправки сообщения пользователю в чате с ботом.
    private fun sendMessage(update: UpdateData, text: String): String {
        val encod = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
        val urlSendMessage = "$BASE_URL/bot$botToken/sendMessage?chat_id=${update.chatId}&text=${encod}"
        return getResponse(urlSendMessage).body()
    }

    //Функция удаления сообщения бота, если нужно корректировать messageId
    private fun deleteMessage(update: UpdateData, listMessageId: List<Long>) {
        for (messageId in listMessageId) {
            // Проверяем, чтобы не удалять сообщение с командой /start
            if (update.message.lowercase() == "/start") {
                continue
            }
            val urlDeleteMessage = "$BASE_URL/bot$botToken/deleteMessage?chat_id=${update.chatId}&" +
                    "message_id=${messageId}"
            getResponse(urlDeleteMessage).body()
        }
    }

    // Функция вывода основного меню пользователю в чате с ботом.
    private fun sendMenu(update: UpdateData): String {
        val urlSendMessage = "$BASE_URL/bot$botToken/sendMessage"
        val requestBody = SendMessageRequest(
            chatId = update.chatId,
            text = """
                $RELOAD Загрузка словаря...
                $GREEN_CHECK Словарь загружен!
                $DARTS Добро пожаловать в основное меню!
            """.trimIndent(),
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyboard(text = "$BOOK Изучать слова", callbackData = LEARN_WORDS_BUTTON),
                        InlineKeyboard(text = "$STATISTIC Статистика", callbackData = STATISTICS_BUTTON),
                    ),
                    listOf(
                        InlineKeyboard(text = "Сбросить прогресс", callbackData = RESET_BUTTON)
                    )
                )
            )
        )
        val requestBodyString = json.encodeToString(requestBody)
        return getResponse(urlSendMessage, requestBodyString).body()
    }

    // Функция получения нового Слова для изучения, если есть не выученные слова.
    private fun getLastQuestions(bot: TelegramBotService, update: UpdateData): Question? {
        val trainer = getUpdateTrainer(update)
        val question = trainer.getNextQuestion()

        return if (question == null) {
            bot.sendMessage(update, "Все слова выучены.")
            bot.sendMenu(update)
            null
        } else {
            bot.sendQuestion(update, question)
            question
        }
    }

    // Проверка на правильный ответ и отправка соответствующего сообщения пользователю в чате с ботом.
    private fun checkNextQuestionAndSend(update: UpdateData, question: Question) {
        val trainer = getUpdateTrainer(update)

        val userClickedButton = if (update.callbackData.startsWith(CALLBACK_DATA_ANSWER_)) {
            update.callbackData.substring(CALLBACK_DATA_ANSWER_.length).toIntOrNull()
        } else null

        if (trainer.checkAnswer(userClickedButton)) {
            sendMessage(update, "Правильно!")
        } else {
            sendMessage(
                update, "Неправильно! " +
                        "${question.correctAnswer.questionWord} - это ${question.correctAnswer.translate}"
            )
        }
    }

    // Функция отправки Слова для изучения и его вариантов ответов
    private fun sendQuestion(update: UpdateData, question: Question): String? {
        val urlSendMessage = "$BASE_URL/bot$botToken/sendMessage"
        val requestBody = SendMessageRequest(
            chatId = update.chatId,
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
        return getResponse(urlSendMessage, requestBodyString).body()
    }

    //Функция получения ответа от сервера
    private fun getResponse(urlGetUpdates: String): HttpResponse<String> {
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    //перегруженная Функция получения ответа от сервера используя пост запрос
    private fun getResponse(urlGetUpdates: String, str: String): HttpResponse<String> {
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(str))
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }
}