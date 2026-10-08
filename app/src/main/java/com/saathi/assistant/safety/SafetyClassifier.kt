package com.saathi.assistant.safety

import com.saathi.assistant.automation.Action
import com.saathi.assistant.automation.ActionType

enum class SafetyLevel { SAFE, CONFIRM, BLOCK }

enum class SafetyReason { NONE, CREDENTIAL, FINANCIAL, SECURITY_CONTROL, SENSITIVE_ACTION, CALL, SHARE, USER_MARKED, PHONE_URI }

data class SafetyVerdict(val level: SafetyLevel, val reason: SafetyReason = SafetyReason.NONE)

/**
 * Conservative keyword/type based classifier. It is a safety net, not a guarantee:
 * it intentionally prefers false positives (blocking/confirming) over false negatives.
 */
object SafetyClassifier {
    private val splitter = Regex("[^\\p{L}\\p{M}\\p{N}]+")
    private val secretLike = Regex("^\\d{4,8}$")

    private val credentialWords = setOf(
        "password", "passwd", "passcode", "otp", "pin", "cvv", "cvc", "captcha", "recaptcha",
        "पासवर्ड", "पासकोड", "ओटीपी", "पिन", "कैप्चा"
    )
    private val financialWords = setOf(
        "upi", "netbanking", "neft", "imps", "rtgs", "transfer", "payment", "pay",
        "भुगतान", "पेमेंट", "ट्रांसफर"
    )
    private val securityPhrases = listOf(
        "factory reset", "play protect", "device admin", "change password", "change pin",
        "screen lock", "फैक्टरी रीसेट", "पासवर्ड बदल"
    )
    private val confirmWords = setOf(
        "send", "post", "share", "delete", "remove", "publish",
        "भेजें", "भेजो", "भेजे", "पोस्ट", "शेयर", "हटाएं", "हटाएँ", "मिटाएं", "डिलीट"
    )

    private fun words(s: String): List<String> = s.lowercase().split(splitter).filter { it.isNotEmpty() }

    fun classify(a: Action): SafetyVerdict {
        when (a.type) {
            ActionType.TYPE_TEXT -> {
                if (secretLike.matches(a.value.trim())) return block(SafetyReason.CREDENTIAL)
                if ((words(a.value) + words(a.target)).any { it in credentialWords }) return block(SafetyReason.CREDENTIAL)
            }
            ActionType.CLICK_TEXT, ActionType.CLICK_ID -> {
                val w = words(a.target)
                val lower = a.target.lowercase()
                if (w.any { it in credentialWords }) return block(SafetyReason.CREDENTIAL)
                if (w.any { it in financialWords }) return block(SafetyReason.FINANCIAL)
                if (securityPhrases.any { lower.contains(it) }) return block(SafetyReason.SECURITY_CONTROL)
                if (w.any { it in confirmWords }) return confirm(SafetyReason.SENSITIVE_ACTION)
            }
            ActionType.DIAL -> return confirm(SafetyReason.CALL)
            ActionType.SHARE -> return confirm(SafetyReason.SHARE)
            ActionType.OPEN_URI -> if (a.target.lowercase().startsWith("tel:")) return confirm(SafetyReason.PHONE_URI)
            else -> Unit
        }
        if (a.confirmationRequired) return confirm(SafetyReason.USER_MARKED)
        return SafetyVerdict(SafetyLevel.SAFE)
    }

    private fun block(r: SafetyReason) = SafetyVerdict(SafetyLevel.BLOCK, r)
    private fun confirm(r: SafetyReason) = SafetyVerdict(SafetyLevel.CONFIRM, r)
}
