package org.example

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private const val ANSI_GREEN = "\u001B[32m"
private const val ANSI_RESET = "\u001B[0m"

data class UpdateData(
    val updateId: String,
    val chatId: String,
    val text: String,
) {
    override fun toString(): String {
        return """
            $ANSI_GREEN
            <-|UpdateId = [$updateId]
            <-|ChatId = [$chatId]
            <-|Text = [$text]
            $ANSI_RESET
        """.trimMargin("<-|")
    }
}

data class TelegramBotService(
    private val botToken: String,
    private var resultUpdateId: Int = 0,
    private val baseUrl: String = "https://api.telegram.org",
) {
    private val updateIdRegex: Regex
        get() = "\"update_id\":(\\d+?),\n\"message\"".toRegex()

    private val messageTextRegex: Regex
        get() = "\"text\":\"(.+?)\"".toRegex()

    private val chatIdRegex: Regex
        get() = "\"id\":(\\d+?),\"first_name\"".toRegex()

    private var data: UpdateData? = null

    private fun getUpdates(): String {
        val urlGetUpdates = "$baseUrl/bot$botToken/getUpdates?offset=$resultUpdateId"
        return getResponse(urlGetUpdates).body()
    }

    private fun getUpdateId(updates: String): String? = getMatchResultValue(updateIdRegex.find(updates))
    private fun getChatId(updates: String): String? = getMatchResultValue(chatIdRegex.find(updates))
    private fun getText(updates: String): String? = getMatchResultValue(messageTextRegex.find(updates))

    fun getUpdatesData(): UpdateData? {
        val updates = getUpdates()
        println(updates)
        val updateId = getUpdateId(updates)
        val chatId = getChatId(updates)
        val text = getText(updates)
        if (updateId == null) return null else {
            data = UpdateData(
                updateId = updateId.toString(),
                chatId = chatId.toString(),
                text = text.toString()
            )
            resultUpdateId = data!!.updateId.toInt() + 1
        }
        return data
    }

    fun sendMessage(chatId: String, text: String) {
        if (text.length !in 1..4096) return
        if (text == "Hello") {
            val urlSendMessage = "$baseUrl/bot$botToken/sendMessage?chat_id=$chatId&text=${
                text.replace(" ", "%20")
            }"
            println("Response from sendMessage: ${getResponse(urlSendMessage).body()}")
        }
    }

    private fun getResponse(urlGetUpdates: String): HttpResponse<String> {
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun getMatchResultValue(matchResult: MatchResult?): String? {
        return matchResult?.groups?.get(1)?.value
    }
}