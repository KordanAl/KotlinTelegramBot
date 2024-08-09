package org.example

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private const val BASE_URL = "https://api.telegram.org"
private const val REQUEST_DELAY = 2000L

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0

    while (true) {
        Thread.sleep(REQUEST_DELAY)
        val updates: String = getUpdates(botToken, updateId)
        println(updates)

        val updateIdRegex = "\"update_id\":(\\d+?)(?:,|$)".toRegex()
        val matchResultUpdateId: MatchResult? = updateIdRegex.find(updates)
        val groupsUpdateId = matchResultUpdateId?.groups
        val id: String? = groupsUpdateId?.get(1)?.value
        if (updateId == -1) continue
        if (id != null) {
            updateId = id.toInt() + 1
            println(id)
        }

        val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
        val matchResult: MatchResult? = messageTextRegex.find(updates)
        val groups = matchResult?.groups
        val text: String? = groups?.get(1)?.value
        if (text != null) println(text)
    }

}

fun getUpdates(botToken: String, updateId: Int): String {
    val urlGetUpdates = "$BASE_URL/bot$botToken/getUpdates?offset=$updateId"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

    return response.body()
}
