package id.usecase.noted

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import id.usecase.noted.navigation.DeepLink
import id.usecase.noted.navigation.DeepLinkHandler
import id.usecase.noted.navigation.NoteNavigation
import id.usecase.noted.navigation.NoteDetailNavKey
import id.usecase.noted.ui.theme.NotedTheme

class MainActivity : ComponentActivity() {
    private var pendingDeepLink: DeepLink? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDeepLink = DeepLinkHandler.parse(intent)
        setContent {
            NotedTheme {
                NoteNavigation(
                    modifier = Modifier.fillMaxSize(),
                    initialDeepLink = pendingDeepLink,
                    onDeepLinkConsumed = {
                        pendingDeepLink = null
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink = DeepLinkHandler.parse(intent)
    }
}
