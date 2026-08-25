package io.github.jukerupup.mydictionary.app

import androidx.test.core.app.ApplicationProvider
import io.github.jukerupup.mydictionary.domain.model.DictionaryCredential
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppContainerTest {
    @Test
    fun `blank credentials produce explicit configuration state`() {
        val configuration = AppContainer.create(
            context = ApplicationProvider.getApplicationContext(),
            learnersKey = "",
            thesaurusKey = "  ",
        ).configuration

        assertEquals(
            setOf(DictionaryCredential.Learners, DictionaryCredential.Thesaurus),
            (configuration as AppConfiguration.MissingCredentials).credentials,
        )
    }

    @Test
    fun `both credentials produce ready state`() {
        val configuration = AppContainer.create(
            context = ApplicationProvider.getApplicationContext(),
            learnersKey = "learner",
            thesaurusKey = "thesaurus",
        ).configuration

        assertSame(AppConfiguration.Ready, configuration)
    }
}
