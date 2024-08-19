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

    // Функция получения обновления и данных если они пришли с ответом.
    fun getUpdates(): UpdateData? {
        val urlGetUpdates = "$BASE_URL/bot$botToken/getUpdates?offset=$lastUpdateId"
        val responseString: String = getResponse(urlGetUpdates).body()
        println("Response from getUpdates: $responseString")

        val response: Response = json.decodeFromString(responseString)
        if (response.result.isEmpty()) return null

        val sortedUpdates = response.result.sortedBy { it.updateId }
        val firstUpdate = sortedUpdates.firstOrNull() ?: return null

        val chatId = firstUpdate.message?.chat?.id
            ?: firstUpdate.callbackQuery?.message?.chat?.id
            ?: return null

        lastUpdateId = sortedUpdates.last().updateId + 1

        return UpdateData(
            updateId = sortedUpdates.last().updateId,
            chatId = chatId,
            message = firstUpdate.message?.text ?: "",
            callbackData = firstUpdate.callbackQuery?.data ?: "",
        )
    }

    // Функция для обработки обновления и вывода соответствующей информации для каждого пользователя
    fun handleUpdate(bot: TelegramBotService, update: UpdateData) {
        when {
            update.message.lowercase() == "/start" -> bot.sendMenu(update.chatId)
            update.callbackData.lowercase() == LEARN_WORDS_BUTTON -> currentQuestion = bot.getLastQuestions(bot, update)
            update.callbackData.lowercase() == STATISTICS_BUTTON -> handleStatisticsButton(bot, update)
            update.callbackData.lowercase() == RESET_BUTTON -> handleResetButton(bot, update.chatId, update)
            update.callbackData.lowercase() == BACK_TO_MENU_BUTTON -> bot.sendMenu(update.chatId)
            update.callbackData.lowercase().startsWith(CALLBACK_DATA_ANSWER_) -> handleAnswer(bot, update)
        }
    }

    // Функция вывода статистики
    private fun handleStatisticsButton(bot: TelegramBotService, update: UpdateData) {
        val trainer = getUpdateTrainer(update)
        val statistics = trainer.getStatistics()
        bot.sendMessage(
            update.chatId,
            "$BICEPS Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percent}%"
        )
        bot.sendMenu(update.chatId)
    }

    //Функция сброса прогресса
    private fun handleResetButton(bot: TelegramBotService, chatId: Long, update: UpdateData) {
        val trainer = getUpdateTrainer(update)
        trainer.resetProgress()
        sendMessage(chatId, "Прогресс сброшен")
        bot.sendMenu(update.chatId)
    }

    //функция проверки ответов
    private fun handleAnswer(bot: TelegramBotService, update: UpdateData) {
        val trainer = getUpdateTrainer(update)
        currentQuestion?.let { question ->
            bot.checkNextQuestionAndSend(update, question)
            if (question.correctAnswer.correctAnswersCount >= trainer.maxValueLearnedCount) {
                currentQuestion = bot.getLastQuestions(bot, update)
                if (currentQuestion == null) {
                    bot.sendMenu(update.chatId)
                }
            } else {
                currentQuestion = bot.getLastQuestions(bot, update)
            }
        }
    }

    // Функция отправки сообщения пользователю в чате с ботом.
    private fun sendMessage(chatId: Long, text: String): String {
        val encod = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
        val urlSendMessage = "$BASE_URL/bot$botToken/sendMessage?chat_id=$chatId&text=${encod}"
        return getResponse(urlSendMessage).body()
    }

    // Функция вывода основного меню пользователю в чате с ботом.
    private fun sendMenu(chatId: Long): String {
        val urlSendMessage = "$BASE_URL/bot$botToken/sendMessage"
        sendMessage(chatId, "$RELOAD Загрузка словаря...")

        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = "$DARTS Основное меню - English Words Learning Bot.",
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
    private fun getLastQuestions(bot: TelegramBotService, botUpdate: UpdateData): Question? {
        val trainer = getUpdateTrainer(botUpdate)
        val question = trainer.getNextQuestion()

        return if (question == null || question.correctAnswer.correctAnswersCount >= trainer.maxValueLearnedCount) {
            bot.sendMessage(botUpdate.chatId, "Все слова выучены.")
            null
        } else {
            bot.sendQuestion(botUpdate.chatId, question)
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
            sendMessage(update.chatId, "Правильно!")
        } else {
            sendMessage(
                update.chatId, "Неправильно! " +
                        "${question.correctAnswer.questionWord} - это ${question.correctAnswer.translate}"
            )
        }
    }

    // Функция получения актуального тренера для каждого пользователя
    private fun getUpdateTrainer(update: UpdateData) = trainers.getOrPut(update.chatId) {
        LearnWordsTrainer("${update.chatId}.txt")
    }

    // Функция отправки Слова для изучения и его вариантов ответов
    private fun sendQuestion(chatId: Long, question: Question): String? {
        val urlSendMessage = "$BASE_URL/bot$botToken/sendMessage"
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