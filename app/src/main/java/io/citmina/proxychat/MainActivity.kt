package io.citmina.proxychat

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.citmina.proxychat.ui.theme.proxychatTheme
import androidx.core.view.WindowCompat
import kotlinx.coroutines.*
import okhttp3.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import io.citmina.proxychat.data.ChatMessage
import io.citmina.proxychat.service.ChatService
import io.citmina.proxychat.util.ApiKeyManager

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var apiKeyManager: ApiKeyManager
    private lateinit var chatService: ChatService
    private val defaultApiKey = "null"
    private val maxContextLength = 12
    
    private val models = listOf(
        "gpt-4-turbo-preview",
        "gpt-4",
        "gpt-4-32k",
        "gpt-3.5-turbo",
        "gpt-3.5-turbo-16k"
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        apiKeyManager = ApiKeyManager(this)
        chatService = ChatService(OkHttpClient())
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        
        enableEdgeToEdge()
        setContent {
            proxychatTheme {
                var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
                var currentMessage by remember { mutableStateOf(TextFieldValue("")) }
                var isLoading by remember { mutableStateOf(false) }
                var streamingText by remember { mutableStateOf("") }
                var selectedModel by remember { mutableStateOf(models[3]) }
                var isModelMenuExpanded by remember { mutableStateOf(false) }
                var showApiKeyDialog by remember { mutableStateOf(false) }
                var apiKeyInput by remember { mutableStateOf("") }
                var apiKey by remember { mutableStateOf(apiKeyManager.getStoredApiKey() ?: defaultApiKey) }
                val focusManager = LocalFocusManager.current
                val coroutineScope = rememberCoroutineScope()

                if (showApiKeyDialog) {
                    AlertDialog(
                        onDismissRequest = { showApiKeyDialog = false },
                        title = { Text("设置 API Key") },
                        text = {
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                label = { Text("输入新的 API Key") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (apiKeyInput.isNotEmpty()) {
                                    apiKey = apiKeyInput
                                    apiKeyManager.storeApiKey(apiKeyInput)
                                    showApiKeyDialog = false
                                }
                            }) {
                                Text("确定")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showApiKeyDialog = false }) {
                                Text("取消")
                            }
                        }
                    )
                }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            navigationIcon = {
                                IconButton(onClick = { showApiKeyDialog = true }) {
                                    Text("Key")
                                }
                            },
                            title = {
                                Box {
                                    TextButton(
                                        onClick = { isModelMenuExpanded = true }
                                    ) {
                                        Text(selectedModel)
                                    }
                                    DropdownMenu(
                                        expanded = isModelMenuExpanded,
                                        onDismissRequest = { isModelMenuExpanded = false }
                                    ) {
                                        models.forEach { model ->
                                            DropdownMenuItem(
                                                text = { Text(model) },
                                                onClick = {
                                                    selectedModel = model
                                                    isModelMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            reverseLayout = true
                        ) {
                            items(messages.asReversed()) { message ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Card(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (message.isUser) 
                                                MaterialTheme.colorScheme.primaryContainer 
                                            else 
                                                MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    ) {
                                        Text(
                                            text = message.content,
                                            modifier = Modifier.padding(12.dp),
                                            color = if (message.isUser) 
                                                MaterialTheme.colorScheme.onPrimaryContainer 
                                            else 
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                            if (streamingText.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Card(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                                            )
                                        ) {
                                            Text(
                                                text = streamingText,
                                                modifier = Modifier.padding(12.dp),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .imePadding()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = currentMessage,
                                    onValueChange = { currentMessage = it },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    placeholder = { Text("输入消息") },
                                    shape = MaterialTheme.shapes.large,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    minLines = 1,
                                    maxLines = 5
                                )
                                FilledTonalButton(
                                    onClick = {
                                        if (currentMessage.text.isNotEmpty() && !isLoading) {
                                            val userMessage = currentMessage.text
                                            messages = (messages + ChatMessage(userMessage, true))
                                                .takeLast(maxContextLength)
                                            currentMessage = TextFieldValue("")
                                            focusManager.clearFocus()
                                            isLoading = true
                                            streamingText = ""
                                            
                                            coroutineScope.launch {
                                                try {
                                                    val response = withContext(Dispatchers.IO) {
                                                        chatService.sendChatRequest(messages, selectedModel, apiKey) { text ->
                                                            withContext(Dispatchers.Main) {
                                                                streamingText += text
                                                            }
                                                        }
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        isLoading = false
                                                        if (response != null) {
                                                            messages = (messages + ChatMessage(response, false))
                                                                .takeLast(maxContextLength)
                                                            streamingText = ""
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        isLoading = false
                                                        messages = (messages + ChatMessage("抱歉，发生错误：${e.message}", false))
                                                            .takeLast(maxContextLength)
                                                        streamingText = ""
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Text(if (isLoading) "发送中..." else "发送")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
