package com.makumessage.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val repo: Repository = FakeRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MakuTheme { App(repo) }
        }
    }
}

@Composable
fun MakuTheme(content: @Composable () -> Unit) {
    val purple = Color(0xFF7C4DFF)
    val scheme = if (androidx.compose.foundation.isSystemInDarkTheme())
        darkColorScheme(primary = purple)
    else
        lightColorScheme(primary = purple)
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun App(repo: Repository) {
    var loggedIn by rememberSaveable { mutableStateOf(false) }
    var openChat by rememberSaveable { mutableStateOf<Int?>(null) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            !loggedIn -> LoginScreen(repo) { loggedIn = true }
            openChat == null -> ChatListScreen(repo) { openChat = it }
            else -> {
                BackHandler { openChat = null }
                ChatScreen(repo, openChat!!) { openChat = null }
            }
        }
    }
}

@Composable
fun LoginScreen(repo: Repository, onDone: () -> Unit) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var register by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("MakuMessage", fontSize = 32.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = login, onValueChange = { login = it },
            label = { Text("Логин") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Пароль") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                val ok = if (register) repo.register(login.trim(), password) else repo.login(login.trim(), password)
                if (ok) onDone() else error =
                    if (register) "Логин от 3 символов, пароль от 4" else "Введи логин и пароль"
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) { Text(if (register) "Зарегистрироваться" else "Войти") }
        TextButton(onClick = { register = !register; error = null }) {
            Text(if (register) "Уже есть аккаунт? Войти" else "Нет аккаунта? Регистрация")
        }
    }
}

@Composable
fun Avatar(name: String, size: Int = 48) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.take(1).uppercase(), color = Color.White, fontSize = (size / 2.4).sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(repo: Repository, onOpen: (Int) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("MakuMessage") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize()) {
            items(repo.chats(), key = { it.id }) { chat ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(chat.id) }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(chat.name)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(chat.name, fontSize = 17.sp, maxLines = 1)
                        Text(
                            chat.last?.text ?: "Нет сообщений",
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(chat.last?.time ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(repo: Repository, chatId: Int, onBack: () -> Unit) {
    val chat = repo.chat(chatId) ?: return
    var text by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(chat.messages.size) {
        if (chat.messages.isNotEmpty()) listState.animateScrollToItem(chat.messages.lastIndex)
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(chat.name, 36)
                        Spacer(Modifier.width(10.dp))
                        Text(chat.name)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    placeholder = { Text("Сообщение") },
                    modifier = Modifier.weight(1f), maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                )
                IconButton(
                    onClick = {
                        if (text.isNotBlank()) { repo.send(chatId, text.trim()); text = "" }
                    },
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().padding(horizontal = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(chat.messages, key = { it.id }) { m ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.mine) Arrangement.End else Arrangement.Start) {
                    Column(
                        Modifier.widthIn(max = 300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (m.mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        val fg = if (m.mine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        Text(m.text, color = fg)
                        Text(m.time, color = fg.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.align(Alignment.End))
                    }
                }
            }
        }
    }
}
