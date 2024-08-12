package org.example

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

private const val ANSI_GREEN = "\u001B[32m"
private const val ANSI_YELLOW = "\u001B[33m"
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
    private var data: UpdateData? = null

    private val updateIdRegex: Regex = "\"update_id\":(\\d+?),\n\"message\"".toRegex()
    private val chatIdRegex: Regex = "\"id\":(\\d+?),\"first_name\"".toRegex()
    private val textRegex: Regex = "\"text\":\"(.*?)\"".toRegex()

    private fun getResponse(urlGetUpdates: String): HttpResponse<String> {
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun getDataFromUpdate(regexText: Regex, update: String): String? {
        val matchResult = regexText.find(update)
        return matchResult?.groups?.get(1)?.value
    }

    fun getUpdates(): UpdateData? {
        val urlGetUpdates = "$baseUrl/bot$botToken/getUpdates?offset=$resultUpdateId"
        val update = getResponse(urlGetUpdates).body()
        println("Response from getUpdates: $update")

        val updateId = getDataFromUpdate(updateIdRegex, update)
        val chatId = getDataFromUpdate(chatIdRegex, update)
        val text = getDataFromUpdate(textRegex, update)

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
            val encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
            val urlSendMessage = "$baseUrl/bot$botToken/sendMessage?chat_id=$chatId&text=${encodedText}"
            println(
                "Response from sendMessage: ${getResponse(urlSendMessage).body()}\n" +
                    "${ANSI_YELLOW}SendMessage = [$text]${ANSI_RESET}\n"
            )
        }
    }

}