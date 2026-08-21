package io.github.jukerupup.mydictionary.app

import io.github.jukerupup.mydictionary.domain.model.DictionaryCredential

sealed interface AppConfiguration {
    data object Ready : AppConfiguration

    data class MissingCredentials(
        val credentials: Set<DictionaryCredential>,
    ) : AppConfiguration
}

class AppContainer private constructor(
    val configuration: AppConfiguration,
) {
    companion object {
        fun create(learnersKey: String, thesaurusKey: String): AppContainer {
            val missing = buildSet {
                if (learnersKey.isBlank()) add(DictionaryCredential.Learners)
                if (thesaurusKey.isBlank()) add(DictionaryCredential.Thesaurus)
            }
            val configuration = if (missing.isEmpty()) {
                AppConfiguration.Ready
            } else {
                AppConfiguration.MissingCredentials(missing)
            }
            return AppContainer(configuration)
        }
    }
}
