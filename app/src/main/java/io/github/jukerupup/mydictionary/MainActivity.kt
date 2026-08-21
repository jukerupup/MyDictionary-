package io.github.jukerupup.mydictionary

import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jukerupup.mydictionary.app.AppConfiguration
import io.github.jukerupup.mydictionary.ui.showcase.PrimitiveShowcaseScreen
import io.github.jukerupup.mydictionary.ui.theme.MyDictionaryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isDarkTheme = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }
        val configuration = (application as MyDictionaryApplication).container.configuration
        setContent {
            MyDictionaryTheme {
                if (BuildConfig.DEBUG) {
                    PrimitiveShowcaseScreen()
                } else {
                    ConfigurationSurface(configuration)
                }
            }
        }
    }
}

@Composable
private fun ConfigurationSurface(configuration: AppConfiguration) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = "MyDictionary", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = when (configuration) {
                    AppConfiguration.Ready -> "Dictionary services are configured."
                    is AppConfiguration.MissingCredentials ->
                        "API keys are not configured. Add them to local.properties."
                },
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
