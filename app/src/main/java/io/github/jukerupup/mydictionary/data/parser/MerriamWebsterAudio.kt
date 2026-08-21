package io.github.jukerupup.mydictionary.data.parser

object MerriamWebsterAudio {
    private const val BASE_URL = "https://media.merriam-webster.com/audio/prons/en/us/mp3"
    private val safeReference = Regex("^[A-Za-z0-9_-]+$")

    fun urlFor(audioReference: String?): String? {
        val audio = audioReference?.trim().orEmpty()
        if (audio.isEmpty() || !safeReference.matches(audio)) return null
        val directory = when {
            audio.startsWith("bix", ignoreCase = true) -> "bix"
            audio.startsWith("gg", ignoreCase = true) -> "gg"
            !audio.first().isLetter() -> "number"
            else -> audio.first().lowercaseChar().toString()
        }
        return "$BASE_URL/$directory/$audio.mp3"
    }
}
