package io.github.jukerupup.mydictionary

import android.app.Application
import io.github.jukerupup.mydictionary.app.AppContainer

class MyDictionaryApplication : Application() {
    val container: AppContainer by lazy {
        AppContainer.create(
            learnersKey = BuildConfig.MW_LEARNERS_KEY,
            thesaurusKey = BuildConfig.MW_THESAURUS_KEY,
        )
    }
}
