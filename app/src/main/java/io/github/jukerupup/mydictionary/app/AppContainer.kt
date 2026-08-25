package io.github.jukerupup.mydictionary.app

import android.content.Context
import io.github.jukerupup.mydictionary.audio.Media3AudioController
import io.github.jukerupup.mydictionary.data.parser.MerriamWebsterParser
import io.github.jukerupup.mydictionary.data.remote.MerriamWebsterHttpClient
import io.github.jukerupup.mydictionary.data.repository.MerriamWebsterDictionaryRepository
import io.github.jukerupup.mydictionary.domain.audio.AudioController
import io.github.jukerupup.mydictionary.domain.model.DictionaryCredential
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository
import okhttp3.HttpUrl.Companion.toHttpUrl

sealed interface AppConfiguration {
    data object Ready : AppConfiguration

    data class MissingCredentials(
        val credentials: Set<DictionaryCredential>,
    ) : AppConfiguration
}

class AppContainer private constructor(
    val configuration: AppConfiguration,
    val dictionaryRepository: DictionaryRepository,
    val audioController: AudioController,
) {
    companion object {
        fun create(context: Context, learnersKey: String, thesaurusKey: String): AppContainer {
            val missing = buildSet {
                if (learnersKey.isBlank()) add(DictionaryCredential.Learners)
                if (thesaurusKey.isBlank()) add(DictionaryCredential.Thesaurus)
            }
            val configuration = if (missing.isEmpty()) {
                AppConfiguration.Ready
            } else {
                AppConfiguration.MissingCredentials(missing)
            }
            val api = MerriamWebsterHttpClient.createApi(
                "https://www.dictionaryapi.com/".toHttpUrl(),
            )
            val repository = MerriamWebsterDictionaryRepository(
                api = api,
                parser = MerriamWebsterParser(),
                learnersKey = learnersKey,
                thesaurusKey = thesaurusKey,
            )
            return AppContainer(
                configuration = configuration,
                dictionaryRepository = repository,
                audioController = Media3AudioController(context.applicationContext),
            )
        }
    }
}
