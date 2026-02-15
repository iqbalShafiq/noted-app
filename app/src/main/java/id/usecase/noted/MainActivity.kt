package id.usecase.noted

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import id.usecase.noted.feature.note.presentation.navigation.NoteNavigation
import id.usecase.noted.ui.theme.NotedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotedTheme {
                NoteNavigation(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
