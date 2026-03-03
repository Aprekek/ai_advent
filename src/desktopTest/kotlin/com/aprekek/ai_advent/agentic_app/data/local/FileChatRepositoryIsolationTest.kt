package com.aprekek.ai_advent.agentic_app.data.local

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.port.IdGenerator
import com.aprekek.ai_advent.agentic_app.domain.port.TimeProvider
import com.aprekek.ai_advent.agentic_app.util.defaultJson
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class FileChatRepositoryIsolationTest {
    @Test
    fun `stores profile chat history in isolated namespaces`() = runTest {
        val tempDir = Files.createTempDirectory("apragent-isolation-test")
        val directories = AppDirectories(tempDir)
        val repository = FileChatRepository(
            profileStateStore = ProfileStateStore(directories, defaultJson()),
            idGenerator = object : IdGenerator {
                private var value = 0
                override fun nextId(): String = "id-${++value}"
            },
            timeProvider = object : TimeProvider {
                private var now = 1L
                override fun nowMillis(): Long = now++
            }
        )

        val chatA = repository.createChat("uA", "Chat A")
        val chatB = repository.createChat("uB", "Chat B")

        repository.appendMessage(
            userId = "uA",
            chatId = chatA.id,
            message = ChatMessage("m1", chatA.id, ChatRole.USER, "hello", 10)
        )
        repository.appendMessage(
            userId = "uB",
            chatId = chatB.id,
            message = ChatMessage("m2", chatB.id, ChatRole.USER, "world", 20)
        )

        val messagesA = repository.listMessages("uA", chatA.id)
        val messagesB = repository.listMessages("uB", chatB.id)

        assertEquals(1, messagesA.size)
        assertEquals("hello", messagesA.first().content)
        assertEquals(1, messagesB.size)
        assertEquals("world", messagesB.first().content)
    }
}
