package io.github.jukerupup.mydictionary.app

import io.github.jukerupup.mydictionary.domain.model.DictionaryCredential
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AppContainerTest {
    @Test
    fun `blank credentials produce explicit configuration state`() {
        val configuration = AppContainer.create("", "  ").configuration

        assertEquals(
            setOf(DictionaryCredential.Learners, DictionaryCredential.Thesaurus),
            (configuration as AppConfiguration.MissingCredentials).credentials,
        )
    }

    @Test
    fun `both credentials produce ready state`() {
        val configuration = AppContainer.create("learner", "thesaurus").configuration

        assertSame(AppConfiguration.Ready, configuration)
    }
}
