package io.github.jukerupup.mydictionary.app

import android.content.Context
import io.github.jukerupup.mydictionary.audio.Media3AudioController
import io.github.jukerupup.mydictionary.data.parser.WiktionaryParser
import io.github.jukerupup.mydictionary.data.remote.WiktionaryHttpClient
import io.github.jukerupup.mydictionary.data.repository.WiktionaryDictionaryRepository
import io.github.jukerupup.mydictionary.domain.audio.AudioController
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository

sealed interface AppConfiguration {
    data object Ready : AppConfiguration

    data class MissingCredentials(
        val credentials: Set<io.github.jukerupup.mydictionary.domain.model.DictionaryCredential>,
    ) : AppConfiguration
}

class AppContainer private constructor(
    val configuration: AppConfiguration,
    val dictionaryRepository: DictionaryRepository,
    val audioControllerFactory: (Context) -> AudioController,
) {
    companion object {
        fun create(context: Context): AppContainer {
            val repository = WiktionaryDictionaryRepository(
                englishApi = WiktionaryHttpClient.createEnglishApi(),
                chineseApi = WiktionaryHttpClient.createChineseApi(),
                parser = WiktionaryParser(),
            )
            return AppContainer(
                configuration = AppConfiguration.Ready,
                dictionaryRepository = repository,
                audioControllerFactory = { appContext ->
                    Media3AudioController(appContext.applicationContext)
                },
            )
        }
    }
}
