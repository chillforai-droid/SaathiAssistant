package com.saathi.assistant.adapters

/**
 * A well-known app. Package names differ per device/version, so every app lists several
 * candidates; the launcher picks whichever one is actually installed.
 */
data class KnownApp(
    val key: String,
    val displayName: String,
    val packages: List<String>,
    val aliases: List<String>,
    /** For system features without a stable package (camera, dialer). */
    val launchAction: String? = null,
    val emoji: String = "📱"
)

object KnownApps {
    val all: List<KnownApp> = listOf(
        KnownApp(
            "whatsapp", "WhatsApp", listOf("com.whatsapp", "com.whatsapp.w4b"),
            listOf("whatsapp", "whats app", "व्हाट्सएप", "व्हाट्सऐप", "व्हॉट्सऐप", "व्हाट्सअप", "वाट्सएप", "वॉट्सऐप", "व्हाटसएप", "व्हाट्स ऐप", "व्हाट्स एप"),
            emoji = "💬"
        ),
        KnownApp(
            "messenger", "Messenger", listOf("com.facebook.orca"),
            listOf("messenger", "fb messenger", "मैसेंजर", "मेसेंजर"), emoji = "🗨️"
        ),
        KnownApp(
            "facebook", "Facebook", listOf("com.facebook.katana"),
            listOf("facebook", "फेसबुक", "फ़ेसबुक", "fb", "एफबी"), emoji = "📘"
        ),
        KnownApp(
            "youtube", "YouTube", listOf("com.google.android.youtube"),
            listOf("youtube", "you tube", "यूट्यूब", "यूटूब", "यू ट्यूब"), emoji = "▶️"
        ),
        KnownApp(
            "youtube_studio", "YouTube Studio", listOf("com.google.android.apps.youtube.creator"),
            listOf("youtube studio", "yt studio", "यूट्यूब स्टूडियो"), emoji = "🎬"
        ),
        KnownApp(
            "instagram", "Instagram", listOf("com.instagram.android"),
            listOf("instagram", "इंस्टाग्राम", "इन्स्टाग्राम", "इंस्टा", "insta"), emoji = "📸"
        ),
        KnownApp(
            "telegram", "Telegram", listOf("org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram"),
            listOf("telegram", "टेलीग्राम", "टेलिग्राम"), emoji = "✈️"
        ),
        KnownApp(
            "chrome", "Chrome", listOf("com.android.chrome"),
            listOf("chrome", "क्रोम"), emoji = "🌐"
        ),
        KnownApp(
            "camera", "Camera", emptyList(),
            listOf("camera", "कैमरा", "कैमेरा"),
            launchAction = "android.media.action.STILL_IMAGE_CAMERA", emoji = "📷"
        ),
        KnownApp(
            "gallery", "Gallery", listOf(
                "com.google.android.apps.photos", "com.sec.android.gallery3d", "com.miui.gallery",
                "com.android.gallery3d", "com.google.android.gallery3d"
            ),
            listOf("gallery", "गैलरी", "photos", "google photos", "फोटोज", "फोटो", "फ़ोटो"), emoji = "🖼️"
        ),
        KnownApp(
            "files", "Files", listOf(
                "com.google.android.apps.nbu.files", "com.android.documentsui",
                "com.sec.android.app.myfiles", "com.mi.android.globalFileexplorer"
            ),
            listOf("files", "file manager", "filemanager", "फाइल्स", "फाइल", "फाइल मैनेजर", "फ़ाइल्स"), emoji = "📁"
        ),
        KnownApp(
            "phone", "Phone", emptyList(),
            listOf("phone", "dialer", "फोन", "फ़ोन", "डायलर"),
            launchAction = "android.intent.action.DIAL", emoji = "📞"
        )
    )

    fun byKey(key: String): KnownApp? = all.firstOrNull { it.key.equals(key, ignoreCase = true) }
}
