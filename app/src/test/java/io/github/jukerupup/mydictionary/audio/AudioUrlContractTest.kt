package io.github.jukerupup.mydictionary.audio

import io.github.jukerupup.mydictionary.data.parser.MerriamWebsterAudio
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AudioUrlContractTest {
    @Test
    fun pronunciationReferenceProducesOfficialKeyFreeMp3Url() {
        val url = requireNotNull(MerriamWebsterAudio.urlFor("resili01"))
        val uri = URI(url)

        assertEquals("https", uri.scheme)
        assertEquals("media.merriam-webster.com", uri.host)
        assertEquals("/audio/prons/en/us/mp3/r/resili01.mp3", uri.path)
        assertFalse("Pronunciation URLs must not contain credentials", url.contains('?'))
    }
}
