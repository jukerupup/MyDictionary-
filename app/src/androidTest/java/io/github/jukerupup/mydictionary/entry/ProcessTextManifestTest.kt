package io.github.jukerupup.mydictionary.entry

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProcessTextManifestTest {
    @Test
    fun processTextPlainTextResolvesToExactlyTheExportedQuickDefineActivity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            type = "text/plain"
        }

        val matches = context.packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName == context.packageName }

        assertEquals(1, matches.size)
        val activity = matches.single().activityInfo
        assertEquals("${context.packageName}.entry.ProcessTextActivity", activity.name)
        assertTrue(activity.exported)
    }
}
