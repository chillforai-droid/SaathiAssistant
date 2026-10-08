package com.saathi.assistant

import com.saathi.assistant.command.Args
import com.saathi.assistant.command.CommandParser
import com.saathi.assistant.command.CommandType
import com.saathi.assistant.command.ParseError
import com.saathi.assistant.command.WorkflowTrigger
import org.junit.Assert.assertEquals
import org.junit.Test

class CommandParserTest {
    private val parser = CommandParser()

    private fun app(text: String, key: String) {
        val r = parser.parse(text)
        assertEquals("type for '$text'", CommandType.OPEN_APP, r.type)
        assertEquals("app for '$text'", key, r.arg(Args.APP))
    }

    @Test fun opensWhatsApp() {
        listOf("WhatsApp खोलो", "व्हाट्सएप खोलो", "open WhatsApp", "WhatsApp open करो").forEach { app(it, "whatsapp") }
    }

    @Test fun opensYouTube() {
        listOf("YouTube खोलो", "यूट्यूब खोलो", "open YouTube").forEach { app(it, "youtube") }
    }

    @Test fun opensSettings() {
        listOf("Settings खोलो", "सेटिंग खोलो").forEach {
            val r = parser.parse(it)
            assertEquals(CommandType.OPEN_SETTINGS, r.type)
            assertEquals("general", r.arg(Args.SETTING))
        }
        assertEquals("wifi", parser.parse("वाईफाई सेटिंग खोलो").arg(Args.SETTING))
    }

    @Test fun callsContact() {
        listOf("Rahul को call करो", "Rahul को फोन करो", "call Rahul").forEach {
            val r = parser.parse(it)
            assertEquals(CommandType.CALL, r.type)
            assertEquals("Rahul", r.arg(Args.CONTACT))
        }
    }

    @Test fun callsNumber() {
        val r = parser.parse("+91 98765 43210 को call करो")
        assertEquals("+919876543210", r.arg(Args.NUMBER))
    }

    @Test fun sendsMessage() {
        val r = parser.parse("WhatsApp पर Rahul को Hello भेजो")
        assertEquals(CommandType.SEND_MESSAGE, r.type)
        assertEquals("whatsapp", r.arg(Args.APP))
        assertEquals("Rahul", r.arg(Args.CONTACT))
        assertEquals("Hello", r.arg(Args.MESSAGE))
        val e = parser.parse("send hello to rahul on whatsapp")
        assertEquals("rahul", e.arg(Args.CONTACT))
        assertEquals("hello", e.arg(Args.MESSAGE))
    }

    @Test fun sendWithoutMessageFailsClearly() {
        assertEquals(ParseError.NO_MESSAGE, parser.parse("Rahul को भेजो WhatsApp पर").error)
    }

    @Test fun clickOnSendButtonIsNotAMessage() {
        val r = parser.parse("Send पर क्लिक करो")
        assertEquals(CommandType.CLICK, r.type)
        assertEquals("Send", r.arg(Args.TEXT))
    }

    @Test fun navigationAndScroll() {
        assertEquals(CommandType.BACK, parser.parse("पीछे जाओ").type)
        assertEquals(CommandType.HOME, parser.parse("home").type)
        assertEquals("up", parser.parse("scroll up").arg(Args.DIRECTION))
        assertEquals("down", parser.parse("नीचे स्क्रॉल करो").arg(Args.DIRECTION))
    }

    @Test fun workflowTriggers() {
        val t = listOf(WorkflowTrigger("w1", listOf("YouTube Upload Prep")))
        val r = parser.parse("YouTube Upload Prep चलाओ", t)
        assertEquals(CommandType.RUN_WORKFLOW, r.type)
        assertEquals("w1", r.arg(Args.WORKFLOW_ID))
    }

    @Test fun unknownAndEmpty() {
        assertEquals(ParseError.UNRECOGNIZED, parser.parse("xyz").error)
        assertEquals(ParseError.EMPTY, parser.parse("   ").error)
    }
}
