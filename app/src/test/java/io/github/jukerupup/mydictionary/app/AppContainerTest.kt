package io.github.jukerupup.mydictionary.app

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppContainerTest {
    @Test
    fun `container is always ready without api keys`() {
        val container = AppContainer.create(
            context = ApplicationProvider.getApplicationContext(),
        )

        assertSame(AppConfiguration.Ready, container.configuration)
    }

    @Test
    fun `container provides a repository and an audio controller factory`() {
        val container = AppContainer.create(
            context = ApplicationProvider.getApplicationContext(),
        )

        assertTrue(container.dictionaryRepository != null)
        val audioController = container.audioControllerFactory(
            ApplicationProvider.getApplicationContext(),
        )
        assertTrue(audioController != null)
    }
}
