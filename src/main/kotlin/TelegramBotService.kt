package org.example

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

private const val ANSI_GREEN = "\u001B[32m"
private const val ANSI_RESET = "\u001B[0m"

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
    private val baseUrl: String = "https://api.telegram.org",
) {
    private var updateData: UpdateData? = null

    fun getUpdates(): UpdateData? {
        val urlGetUpdates = "$baseUrl/bot$botToken/getUpdates?offset=$lastUpdateId"
        val update: String = getResponse(urlGetUpdates).body()
        println("Response from getUpdates: $update")

        if (getDataFromUpdate("\"update_id\":(\\d+)", update) == null) {
            return null
        } else {
            updateData = UpdateData(
                updateId = getDataFromUpdate("\"update_id\":(\\d+)", update).toString(),
                chatId = getDataFromUpdate("\"chat\":\\{\"id\":(\\d+)", update).toString(),
                text = getDataFromUpdate("\"text\":\"(.*?)\"", update).toString(),
                data = getDataFromUpdate("\"data\":\"(.*?)\"", update).toString()
            )
            lastUpdateId = updateData!!.updateId.toInt() + 1
        }
        return updateData
    }

    fun sendMessage(chatId: String, text: String): String {
        val encod = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
        val urlSendMessage = "$baseUrl/bot$botToken/sendMessage?chat_id=$chatId&text=${encod}"
        return getResponse(urlSendMessage).body()
    }

    fun sendMenu(chatId: String): String {
        val urlSendMessage = "$baseUrl/bot$botToken/sendMessage"
        val sendMenuBody = """
            {
                "chat_id": $chatId,
                "text": "Основное меню",
                "reply_markup": {
                    "inline_keyboard": [
                        [
                            {
                                "text": "Изучить слова",
                                "callback_data": "learn_words_clicked"
                            },
                            {
                                "text": "Статистика",
                                "callback_data": "statistics_clicked"
                            }
                        ]
                    ]
                }
            }
        """.trimIndent()
        return getResponse(urlSendMessage, sendMenuBody).body()
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