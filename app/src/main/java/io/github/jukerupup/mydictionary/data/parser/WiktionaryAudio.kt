package io.github.jukerupup.mydictionary.data.parser

/**
 * Builds the pronunciation audio URL for a Wiktionary audio filename.
 *
 * Wiktionary audio files are hosted on Wikimedia and reachable via:
 * `https://en.wiktionary.org/wiki/Special:FilePath/FILENAME`
 * (which redirects to the uploaded `.ogg`/`.wav`/`.mp3`).
 */
object WiktionaryAudio {
    private const val BASE_URL = "https://en.wiktionary.org/wiki/Special:FilePath/"
    private val safeReference = Regex("^[A-Za-z0-9 _().'-]+$")

    fun urlFor(audioReference: String?): String? {
        val audio = audioReference?.trim().orEmpty()
        if (audio.isEmpty() || !safeReference.matches(audio)) return null
        return BASE_URL + java.net.URLEncoder.encode(audio, "UTF-8")
            .replace("+", "%20")
    }
}
