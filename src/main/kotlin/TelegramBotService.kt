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

    fun sendMessage(chatId: String, text: String): String {
        val encod = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
        val urlSendMessage = "$BASE_URL/bot$botToken/sendMessage?chat_id=$chatId&text=${encod}"
        return getResponse(urlSendMessage).body()
    }

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

    fun startProcessingNewQuestion(
        botTrainer: LearnWordsTrainer,
        telegramBot: TelegramBotService,
        botUpdate: UpdateData
    ): Question? {
        val question = botTrainer.getNextQuestion()
        return if (question == null) {
            telegramBot.sendMessage(botUpdate.chatId, "Все слова выучены")
            null
        } else {
            telegramBot.sendQuestion(botUpdate.chatId, question)
            question
        }
    }

    fun checkNextQuestionAndSend(trainer: LearnWordsTrainer, botUpdate: UpdateData, question: Question) {
        val userClickedButton = if (botUpdate.data.startsWith(CALLBACK_DATA_ANSWER_PREFIX)) {
            botUpdate.data.substring(CALLBACK_DATA_ANSWER_PREFIX.length).toIntOrNull()
        } else null

        if (trainer.checkAnswer(userClickedButton)) {
            sendMessage(botUpdate.chatId, "Правильно!")
        } else {
            sendMessage(botUpdate.chatId, "Неправильно! " +
                    "${question.correctAnswer.questionWord} - это ${question.correctAnswer.translate}"
            )
        }
    }

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
                    }"""
            }.joinToString(separator = ",")}       
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

    private fun getResponse(urlGetUpdates: String): HttpResponse<String> {
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun getResponse(urlGetUpdates: String, str: String): HttpResponse<String> {
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(str))
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }
}