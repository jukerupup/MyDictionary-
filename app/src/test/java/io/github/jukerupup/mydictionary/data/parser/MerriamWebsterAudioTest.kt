package io.github.jukerupup.mydictionary.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MerriamWebsterAudioTest {
    @Test
    fun followsOfficialAudioSubdirectoryRules() {
        assertEquals(url("bix", "bixbite01"), MerriamWebsterAudio.urlFor("bixbite01"))
        assertEquals(url("gg", "gggood01"), MerriamWebsterAudio.urlFor("gggood01"))
        assertEquals(url("number", "3d000001"), MerriamWebsterAudio.urlFor("3d000001"))
        assertEquals(url("number", "_alpha01"), MerriamWebsterAudio.urlFor("_alpha01"))
        assertEquals(url("r", "resili01"), MerriamWebsterAudio.urlFor("resili01"))
    }

    @Test
    fun rejectsBlankOrUnsafeReferences() {
        assertNull(MerriamWebsterAudio.urlFor(""))
        assertNull(MerriamWebsterAudio.urlFor("../secret"))
        assertNull(MerriamWebsterAudio.urlFor("two words"))
    }

    private fun url(directory: String, audio: String): String =
        "https://media.merriam-webster.com/audio/prons/en/us/mp3/$directory/$audio.mp3"
}
