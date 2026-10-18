package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.AttachmentType
import com.example.model.ChatType
import com.example.model.StatusDuration
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SessionGramRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun `test authenticated user cloud profile restore and realtime data sync`() = runBlocking {
        val uid = signInTestUser("tester@gmail.com")
        val repo = SessionGramRepository(firestore)

        val currentUser = auth.currentUser
        assertNotNull(currentUser)

        // Restore or initialize user
        val account = repo.restoreOrInitializeGoogleUser(currentUser!!)
        assertNotNull(account)
        assertEquals(true, account.isLoggedIn)
        assertTrue(account.sessionId.startsWith("05"))

        // Add a message to the vault
        repo.sendMessage(
            chatId = "chat_vault",
            text = "Secret note stored in cloud vault"
        )
        val vaultMessages = repo.messages.value["chat_vault"] ?: emptyList()
        assertTrue(vaultMessages.any { it.text == "Secret note stored in cloud vault" })

        // Add 7-day status
        repo.addStatusText(
            text = "Testing 7-day extended status",
            duration = StatusDuration.DAYS_7
        )
        val statuses = repo.statuses.value
        assertTrue(statuses.any { s -> s.items.any { it.duration == StatusDuration.DAYS_7 } })
    }

    @Test
    fun `test group chat and file transfer with unlimited telegram liberty`() = runBlocking {
        signInTestUser("groupadmin@gmail.com")
        val repo = SessionGramRepository(firestore)
        val currentUser = auth.currentUser!!

        repo.restoreOrInitializeGoogleUser(currentUser)

        val group = repo.createNewGroup(
            name = "Security Researchers",
            topic = "Zero-Knowledge Encryption",
            memberNames = listOf("Alice", "Bob")
        )

        assertEquals("Security Researchers", group.name)
        assertEquals(ChatType.GROUP, group.type)
        assertEquals(3, group.memberCount)

        // Large file transmission
        val att = com.example.model.Attachment(
            id = "test_large_file",
            type = AttachmentType.LARGE_FILE,
            fileName = "linux_kernel.tar.xz",
            fileSizeBytes = 2147483648L,
            formattedSize = "2.0 GB",
            sha256Checksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        )
        repo.sendMessage(group.id, "Shared huge raw archive via onion route", attachment = att)

        val messages = repo.messages.value[group.id] ?: emptyList()
        assertTrue(messages.any { it.attachment?.formattedSize == "2.0 GB" })
    }
}
