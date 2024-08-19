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

const val UPDATE_DELAY = 2000L

const val BOOK = "\uD83D\uDCDA"
const val BICEPS = "\uD83D\uDCAA"
const val DOOR = "\uD83D\uDEAA"
const val STATISTIC = "\uD83D\uDCC8"
const val RELOAD = "\uD83D\uDD04"
const val DARTS = "\uD83C\uDFAF"
const val CHECK_RIGHT = "\uD83D\uDC49"
const val CHECK_LEFT = "\uD83D\uDC48"

private const val ANSI_GREEN = "\u001B[32m"
private const val ANSI_RESET = "\u001B[0m"

private const val BASE_URL = "https://api.telegram.org"

const val LEARN_WORDS_BUTTON: String = "learn_words_clicked"
const val STATISTICS_BUTTON: String = "statistics_clicked"
const val BACK_TO_MENU_BUTTON: String = "back_to_menu_clicked"

const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

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
    val replyMarkup: ReplyMarkup,
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
) {

    override fun toString(): String {
        return """
            $ANSI_GREEN
            <-|UpdateId = [$updateId]
            <-|ChatId = [$chatId]
            <-|Message = [$message]
            <-|CallbackData = [$callbackData]
            $ANSI_RESET
        """.trimMargin("<-|")
    }
}

data class TelegramBotService(
    private val botToken: String,
    private var lastUpdateId: Long = 0L,
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }
    // Функция получения обновления и данных если они пришли с ответом.
    fun getUpdates(): UpdateData? {
        val urlGetUpdates = "$BASE_URL/bot$botToken/getUpdates?offset=$lastUpdateId"
        val responseString: String = getResponse(urlGetUpdates).body()
        println("Response from getUpdates: $responseString")

        val response: Response = json.decodeFromString(responseString)
        val update = response.result
        val firstUpdate = update.firstOrNull()
        val updateId = firstUpdate?.updateId ?: return null
        val chatId = firstUpdate.message?.chat?.id
            ?: firstUpdate.callbackQuery?.message?.chat?.id
            ?: return null
        val message = firstUpdate.message?.text ?: ""
        val callbackData = firstUpdate.callbackQuery?.data ?: ""

        lastUpdateId = updateId + 1

        return UpdateData(
            updateId = updateId,
            chatId = chatId,
            message = message,
            callbackData = callbackData,
        )
    }
    // Функция отправки сообщения пользователю в чате с ботом.
    fun sendMessage(chatId: Long, text: String): String {
        val encod = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
        val urlSendMessage = "$BASE_URL/bot$botToken/sendMessage?chat_id=$chatId&text=${encod}"
        return getResponse(urlSendMessage).body()
    }
    // Функция вывода основного меню пользователю в чате с ботом.
    fun sendMenu(chatId: Long): String {
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
                    )
                )
            )
        )
        val requestBodyString = json.encodeToString(requestBody)
        return getResponse(urlSendMessage, requestBodyString).body()
    }
    // Функция получения нового Слова для изучения, если есть не выученные слова.
    fun getLastQuestions(
        bot: TelegramBotService,
        botTrainer: LearnWordsTrainer,
        botUpdate: UpdateData,
    ): Question? {
        val question = botTrainer.getNextQuestion()
        return if (question == null) {
            bot.sendMessage(botUpdate.chatId, "Все слова выучены")
            null
        } else {
            bot.sendQuestion(botUpdate.chatId, question)
            question
        }
    }
    // Проверка на правильный ответ и отправка соответствующего сообщения пользователю в чате с ботом.
    fun checkNextQuestionAndSend(trainer: LearnWordsTrainer, botUpdate: UpdateData, question: Question) {
        val userClickedButton = if (botUpdate.callbackData.startsWith(CALLBACK_DATA_ANSWER_PREFIX)) {
            botUpdate.callbackData.substring(CALLBACK_DATA_ANSWER_PREFIX.length).toIntOrNull()
        } else null

        if (trainer.checkAnswer(userClickedButton)) {
            sendMessage(botUpdate.chatId, "Правильно!")
        } else {
            sendMessage(
                botUpdate.chatId, "Неправильно! " +
                        "${question.correctAnswer.questionWord} - это ${question.correctAnswer.translate}"
            )
        }
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
                            text = variant.translate, callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$index"
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