package ktb_stage_1

import java.io.File
import java.util.*

fun main() {

    val wordFile = File("words.txt")
    wordFile.createNewFile()
    wordFile.writeToLowercaseToBeginning(
        "hello привет\n" +
                "dog собака\n" +
                "cat кошка"
    )

    wordFile.readLines().forEach {
        println(it)
        Thread.sleep(1500)
    }

}

fun File.writeToLowercaseToBeginning(text: String) {
    val resultText = text.lowercase(Locale.getDefault())
    val originalText = this.readText()

    this.writeText(resultText + "\n" + originalText)
}
