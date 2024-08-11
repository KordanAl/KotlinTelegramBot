package org.example

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private const val BASE_URL = "https://api.telegram.org"

data class TelegramBotService(
    val botToken: String,
    var resultUpdateId: Int = 0,
) {
    private val updateIdRegex = "\"update_id\":(\\d+?),\n\"message\"".toRegex()
    private val messageTextRegex = "\"text\":\"(.+?)\"".toRegex()
    private val chatIdRegex = "\"id\":(\\d+?),\"first_name\"".toRegex()

    fun getUpdates(): String {
        val urlGetUpdates = "$BASE_URL/bot$botToken/getUpdates?offset=$resultUpdateId"
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun getUpdateId(updates: String): String? = getMatchResultValue(updateIdRegex.find(updates))
    fun getChatId(updates: String): String? = getMatchResultValue(chatIdRegex.find(updates))
    fun getText(updates: String): String? = getMatchResultValue(messageTextRegex.find(updates))

    fun sendMessage(chatId: String, text: String) {
        if (text.isEmpty() && text.length !in 1..4096) return
        if (text == "Hello") {
            val urlSendMessage = "$BASE_URL/bot$botToken/sendMessage?chat_id=$chatId&text=${
                text.replace(" ", "%20")
            }"
            val client: HttpClient = HttpClient.newBuilder().build()
            val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage)).build()
            val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
            println("Response from sendMessage: ${response.body()}")
        }
    }
}

private fun getMatchResultValue(matchResult: MatchResult?): String? {
    val groups = matchResult?.groups
    val value = groups?.get(1)?.value
    return value
}
