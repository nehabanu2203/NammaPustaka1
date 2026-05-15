package com.example.nammapustaka.ui.screens

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var userInput by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("Hello! I am your Namma Pustaka assistant. You can type your question or tap the mic to speak.") }
    var isListening by remember { mutableStateOf(false) }
    var isThinking by remember { mutableStateOf(false) }

    // Gemini Initialization
    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyCFHqsS1eqD6T89zDpI0TDdamkQCFICIXA",
            requestOptions = RequestOptions(apiVersion = "v1"),
        )
    }

    // Voice recognition setup
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() { isListening = true }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    userInput = text
                    askGemini(text, generativeModel, { isThinking = it }, { aiResponse = it }, scope)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose { speechRecognizer.destroy() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Assistant ✨", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "A Smart Library Assistant",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // AI Response Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                    if (isThinking) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn {
                            item {
                                Text(
                                    text = aiResponse,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Combined Text & Voice Input Area
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(32.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                IconButton(
                    onClick = { startListening(speechRecognizer) },
                    modifier = Modifier.background(
                        if (isListening) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                        RoundedCornerShape(50)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                TextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    placeholder = { Text("Ask about books...") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
                
                IconButton(
                    onClick = {
                        if (userInput.isNotBlank()) {
                            askGemini(userInput, generativeModel, { isThinking = it }, { aiResponse = it }, scope)
                            userInput = ""
                        }
                    },
                    enabled = userInput.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (userInput.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
            
            if (isListening) {
                Text(
                    "Listening to your voice...", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 48.dp, top = 4.dp)
                )
            }
        }
    }
}

private fun startListening(recognizer: SpeechRecognizer) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
    }
    recognizer.startListening(intent)
}

private fun askGemini(
    query: String, 
    model: GenerativeModel, 
    onLoading: (Boolean) -> Unit, 
    onResult: (String) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    onLoading(true)
    scope.launch {
        try {
            val apiKey = model.apiKey
            if (apiKey.startsWith("REPLACE") || apiKey.length < 10) {
                onResult("Please set your Gemini API Key in AIAssistantScreen.kt. Go to Google AI Studio to get a free key.")
                onLoading(false)
                return@launch
            }

            val response = model.generateContent(query)
            onResult(response.text ?: "I am not sure how to answer that.")
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("API_KEY_INVALID")) {
                onResult("Error: Your API Key is invalid. Please check it in AIAssistantScreen.kt.")
            } else {
                onResult("AI Error: ${e.localizedMessage}. Check internet or key.")
            }
        } finally {
            onLoading(false)
        }
    }
}
