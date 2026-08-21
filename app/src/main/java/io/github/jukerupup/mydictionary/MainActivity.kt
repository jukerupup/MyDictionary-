package io.github.jukerupup.mydictionary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jukerupup.mydictionary.app.AppConfiguration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val configuration = (application as MyDictionaryApplication).container.configuration
        setContent { ConfigurationSurface(configuration) }
    }
}

@Composable
private fun ConfigurationSurface(configuration: AppConfiguration) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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
}
