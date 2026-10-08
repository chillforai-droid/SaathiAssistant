package com.saathi.assistant

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.assistant.command.Args
import com.saathi.assistant.command.CommandParser
import com.saathi.assistant.command.CommandType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a device/emulator. It verifies the app context and the parser on a real Android runtime.
 * It does NOT test third-party UI automation (WhatsApp etc.) — that needs manual testing per device.
 */
@RunWith(AndroidJUnit4::class)
class ParserInstrumentedTest {
    @Test fun appContextPackage() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.saathi.assistant", ctx.packageName)
    }

    @Test fun parsesCoreCommandsOnDevice() {
        val p = CommandParser()
        assertEquals("whatsapp", p.parse("WhatsApp खोलो").arg(Args.APP))
        assertEquals("youtube", p.parse("YouTube खोलो").arg(Args.APP))
        assertEquals(CommandType.OPEN_SETTINGS, p.parse("सेटिंग खोलो").type)
        assertEquals("Rahul", p.parse("Rahul को call करो").arg(Args.CONTACT))
    }
}
