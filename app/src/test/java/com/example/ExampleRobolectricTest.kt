package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SessionGramRepository
import com.example.model.AttachmentType
import com.example.model.ChatType
import com.example.model.StatusDuration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches AFEGWICHAT`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AFEGWICHAT", appName)
    }

    @Test
    fun `test status creation with 7 days and 30 days display duration`() {
        val repo = SessionGramRepository()
        repo.createAccount("Tester")

        // Add 7-day status
        repo.addStatusText(
            text = "Testing 7-day extended status update 🛡️",
            duration = StatusDuration.DAYS_7,
            colors = listOf(0xFF134E5E, 0xFF71B280)
        )

        // Add 30-day status
        repo.addStatusText(
            text = "Testing 30-day monthly project milestone 🚀",
            duration = StatusDuration.DAYS_30,
            colors = listOf(0xFF833AB4, 0xFFFD1D1D)
        )

        val myStatus = repo.statuses.value.find { it.isMyStatus }
        assertNotNull(myStatus)
        assertTrue(myStatus!!.items.any { it.duration == StatusDuration.DAYS_7 })
        assertTrue(myStatus.items.any { it.duration == StatusDuration.DAYS_30 })
    }

    @Test
    fun `test group chat creation and member management`() {
        val repo = SessionGramRepository()
        repo.createAccount("Tester")

        val group = repo.createNewGroup(
            name = "Cypherpunk Alliance",
            topic = "Decentralized privacy mesh research",
            memberNames = listOf("Alex", "Elena Rostova", "CipherZero")
        )

        assertEquals("Cypherpunk Alliance", group.name)
        assertEquals(ChatType.GROUP, group.type)
        assertEquals(4, group.memberCount) // "You" + 3 peers

        // Add a new member
        repo.addGroupMember(group.id, "Neo", "05cafebabedeadbeef0123456789abcdef0123456789abcdef0123456789abcfff")

        val updatedGroup = repo.chats.value.find { it.id == group.id }
        assertNotNull(updatedGroup)
        assertEquals(5, updatedGroup!!.memberCount)
        assertTrue(updatedGroup.members.any { it.name == "Neo" })
    }

    @Test
    fun `test unlimited file transmission with telegram liberty`() {
        val repo = SessionGramRepository()
        repo.createAccount("Tester")
        val chatId = "chat_vault"

        val att = com.example.model.Attachment(
            id = "test_large_file",
            type = AttachmentType.LARGE_FILE,
            fileName = "ubuntu-24.04-desktop-amd64.iso",
            fileSizeBytes = 4101881856L, // 3.82 GB
            formattedSize = "3.82 GB",
            sha256Checksum = "9f83c1264b182e4439a2638848d7d8e69cbd3a1e26402431478148b3547e248f"
        )

        repo.sendMessage(chatId, "Transferred huge OS image via onion network", attachment = att)

        val messages = repo.messages.value[chatId] ?: emptyList()
        assertTrue(messages.any { it.attachment?.formattedSize == "3.82 GB" })
    }
}
