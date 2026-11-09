package com.makumessage.app

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Message(val id: Long, val text: String, val mine: Boolean, val time: String)

class Chat(val id: Int, val name: String, initial: List<Message>) {
    val messages: SnapshotStateList<Message> = mutableStateListOf<Message>().also { it.addAll(initial) }
    val last: Message? get() = messages.lastOrNull()
}

/** Экраны работают только с этим интерфейсом. Позже подменим фейк на настоящий сервер. */
interface Repository {
    fun login(login: String, password: String): Boolean
    fun register(login: String, password: String): Boolean
    fun chats(): List<Chat>
    fun chat(id: Int): Chat?
    fun send(chatId: Int, text: String)
}

class FakeRepository : Repository {
    private var nextId = 100L
    private val list = listOf(
        Chat(1, "Маку", listOf(
            Message(1, "Привет! Это MakuMessage", false, "12:01"),
            Message(2, "Привет, работает!", true, "12:02"),
        )),
        Chat(2, "Друг", listOf(Message(3, "Скинешь APK?", false, "11:40"))),
        Chat(3, "Margy Store", listOf(Message(4, "Приложение принято", false, "вчера"))),
        Chat(4, "Избранное", emptyList()),
    )

    private fun now() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    override fun login(login: String, password: String) = login.isNotBlank() && password.isNotBlank()
    override fun register(login: String, password: String) = login.length >= 3 && password.length >= 4
    override fun chats() = list
    override fun chat(id: Int) = list.firstOrNull { it.id == id }

    override fun send(chatId: Int, text: String) {
        chat(chatId)?.messages?.add(Message(nextId++, text, true, now()))
    }
}
