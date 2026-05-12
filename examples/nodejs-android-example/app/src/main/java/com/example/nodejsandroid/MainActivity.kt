package com.example.nodejsandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.nodejsandroid.ui.theme.NodejsAndroidTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NodeServer(this).start()

        setContent {
            NodejsAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NodeVerison(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun NodeVerison(modifier: Modifier = Modifier) {
    var greeting by remember { mutableStateOf("Starting...") }

    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    val response = fetchGreeting()
                    greeting = response
                    break
                } catch (e: Exception) {
                    delay(500)
                }
            }
        }
    }

    Text(
        text = greeting,
        modifier = modifier
    )
}

suspend fun fetchGreeting(): String {
    val url = URL("http://localhost:3000")
    val connection = withContext(Dispatchers.IO) {
        url.openConnection()
    } as HttpURLConnection
    return connection.inputStream.bufferedReader().use { it.readText() }
}