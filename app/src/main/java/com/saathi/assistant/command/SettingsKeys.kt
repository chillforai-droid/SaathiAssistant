package com.saathi.assistant.command

/** Settings pages the app can open, with the spoken/typed words that select them. */
object SettingsKeys {
    val aliases: Map<String, List<String>> = mapOf(
        "general" to listOf("settings", "setting", "सेटिंग", "सेटिंग्स", "सेटिंगस", "सेटींग", "सैटिंग"),
        "wifi" to listOf("wifi", "wi-fi", "वाईफाई", "वाईफ़ाई", "वाई-फाई", "वाइफाई"),
        "bluetooth" to listOf("bluetooth", "ब्लूटूथ", "ब्लूटुथ"),
        "display" to listOf("display", "डिस्प्ले", "brightness", "ब्राइटनेस"),
        "sound" to listOf("sound", "साउंड", "volume", "वॉल्यूम", "आवाज"),
        "battery" to listOf("battery", "बैटरी"),
        "location" to listOf("location", "लोकेशन"),
        "accessibility" to listOf("accessibility", "एक्सेसिबिलिटी"),
        "apps" to listOf("apps", "ऐप्स", "एप्स"),
        "storage" to listOf("storage", "स्टोरेज"),
        "security" to listOf("security", "सिक्योरिटी")
    )
    val keys: Set<String> get() = aliases.keys
}
