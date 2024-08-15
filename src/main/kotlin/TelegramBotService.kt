package org.example

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

private const val ANSI_GREEN = "\u001B[32m"
private const val ANSI_RESET = "\u001B[0m"

private const val BASE_URL = "https://api.telegram.org"

const val LEARN_WORDS_BUTTON = "learn_words_clicked"
const val STATISTICS_BUTTON = "statistics_clicked"
const val BACK_TO_MENU_BUTTON = "back_to_menu_clicked"

const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

data class UpdateData(
    val updateId: String,
    val chatId: String,
    val text: String,
    val data: String,
) {

    override fun toString(): String {
        return """
            $ANSI_GREEN
            <-|UpdateId = [$updateId]
            <-|ChatId = [$chatId]
            <-|Text = [$text]
            <-|Data = [$data]
            $ANSI_RESET
        """.trimMargin("<-|")
    }
}

data class TelegramBotService(
    private val botToken: String,
    private var lastUpdateId: Int = 0,
) {
    // Функция получения обновления и данных если они пришли с ответом.
    fun getUpdates(): UpdateData? {
        val urlGetUpdates = "$BASE_URL/bot$botToken/getUpdates?offset=$lastUpdateId"
        val update: String = getResponse(urlGetUpdates).body()
        println("Response from getUpdates: $update")

        val newUpdateId = getDataFromUpdate("\"update_id\":(\\d+)", update)?.toIntOrNull()

        return if (newUpdateId == null) {
            null
        } else {
            lastUpdateId = newUpdateId + 1
            UpdateData(
                updateId = newUpdateId.toString(),
                chatId = getDataFromUpdate("\"chat\":\\{\"id\":(\\d+)", update).toString(),
                text = getDataFromUpdate("\"text\":\"(.*?)\"", update).toString(),
                data = getDataFromUpdate("\"data\":\"(.*?)\"", update).toString()
            )
        }
    }

    // Функция отправки сообщения пользователю в чате с ботом.
    fun sendMessage(chatId: String, text: String): String {
        val encod = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
        val urlSendMessage = "$BASE_URL/bot$botToken/sendMessage?chat_id=$chatId&text=${encod}"
        return getResponse(urlSendMessage).body()
    }

    // Функция вывода основного меню пользователю в чате с ботом.
    fun sendMenu(chatId: String): String {
        val urlSendMessage = "$BASE_URL/bot$botToken/sendMessage"
        val sendMenuBody = """
            {
                "chat_id": $chatId,
                "text": "Основное меню",
                "reply_markup": {
                    "inline_keyboard": [
                        [
                            {
                                "text": "Изучить слова",
                                "callback_data": "$LEARN_WORDS_BUTTON"
                            },
                            {
                                "text": "Статистика",
                                "callback_data": "$STATISTICS_BUTTON"
                            }
                        ]
                    ]
                }
            }
        """.trimIndent()
        return getResponse(urlSendMessage, sendMenuBody).body()
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
        val userClickedButton = if (botUpdate.data.startsWith(CALLBACK_DATA_ANSWER_PREFIX)) {
            botUpdate.data.substring(CALLBACK_DATA_ANSWER_PREFIX.length).toIntOrNull()
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
    private fun sendQuestion(chatId: String, question: Question): String? {
        val urlSendMessage = "$BASE_URL/bot$botToken/sendMessage"
        val sendQuestionBody = """
            {
                "chat_id": $chatId,
                "text": "${question.correctAnswer.questionWord}",
                "reply_markup": {
                    "inline_keyboard": [
                        [
                            ${question.variants.mapIndexed { index, variant -> 
                            """{
                                "text": "${variant.translate}",
                                "callback_data": "$CALLBACK_DATA_ANSWER_PREFIX$index"
                            }""" }.joinToString(separator = ",")
                            }                                  
                        ],                       
                        [
                            {
                                "text": "Выход в меню",
                                "callback_data": "$BACK_TO_MENU_BUTTON"
                            }     
                        ]
                    ]
                }
            }
        """.trimIndent()

        return getResponse(urlSendMessage, sendQuestionBody).body()
    }

    // Функция предназначена для поиска данных по regex тексту.
    private fun getDataFromUpdate(regexText: String, update: String): String? {
        val matchResult = regexText.toRegex().find(update)
        return matchResult?.groups?.get(1)?.value
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