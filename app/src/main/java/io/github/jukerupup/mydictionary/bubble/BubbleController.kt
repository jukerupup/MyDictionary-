package io.github.jukerupup.mydictionary.bubble

import io.github.jukerupup.mydictionary.domain.lookup.LookupState
import io.github.jukerupup.mydictionary.domain.lookup.LookupStateEngine
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow

/**
 * Drives the floating bubble's query behavior. Reuses [LookupStateEngine] so the
 * bubble gets the exact same latest-query-wins, cancellation, and typed failure
 * semantics as the full screen.
 *
 * Kept free of Android framework types so it can be unit-tested on the JVM.
 */
class BubbleController(
    repository: DictionaryRepository,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AutoCloseable {
    private val engine = LookupStateEngine(repository, scope, dispatcher)

    val state: StateFlow<LookupState> = engine.state

    fun lookup(rawQuery: CharSequence?) = engine.lookup(rawQuery)

    fun cancel() = engine.cancel()

    override fun close() {
        engine.cancel()
    }
}
