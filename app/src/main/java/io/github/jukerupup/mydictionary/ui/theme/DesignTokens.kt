package io.github.jukerupup.mydictionary.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object DictionarySpacing {
    val Space1 = 4.dp
    val Space2 = 8.dp
    val Space3 = 12.dp
    val Space4 = 16.dp
    val Space5 = 20.dp
    val Space6 = 24.dp
    val Space8 = 32.dp
    val Space10 = 40.dp
    val Space12 = 48.dp
}

object DictionaryShapes {
    val Small = RoundedCornerShape(4.dp)
    val Medium = RoundedCornerShape(8.dp)
    val Large = RoundedCornerShape(12.dp)
    val Chip = RoundedCornerShape(percent = 50)
}

object DictionaryLayout {
    val MaxReadingWidth = 720.dp
    val MinDialogWidth = 280.dp
    val MaxDialogWidth = 560.dp
}

object DictionaryMotion {
    const val MicroMillis = 120
    const val StandardMillis = 220
    const val DialogMillis = 280
}
