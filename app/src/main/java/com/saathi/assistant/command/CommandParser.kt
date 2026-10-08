package com.saathi.assistant.command

import com.saathi.assistant.adapters.KnownApps
import java.text.Normalizer

/**
 * Rule-based Hindi / English / Hinglish command parser. Pure Kotlin (no Android types) so it is
 * unit-testable on the JVM. It never executes anything — it only describes the user's intent.
 */
class CommandParser {

    private class Tok(val orig: String, val norm: String)

    // ---- vocabulary (normalized once, via norm()) ----
    private fun words(vararg w: String): Set<String> = w.map { norm(it) }.filter { it.isNotEmpty() }.toSet()

    private val open = words("खोलो", "खोलें", "खोलिए", "खोलिये", "खोल", "खोलना", "open", "launch", "kholo", "khol", "kholiye", "ओपन", "चलाओ", "चलाइए", "chalao")
    private val fillers = words(
        "करो", "कर", "कीजिए", "कीजिये", "करें", "करना", "करिए", "दो", "दीजिए", "देना", "karo", "kar", "do", "please", "plz", "pls",
        "कृपया", "प्लीज", "जरा", "मुझे", "mujhe", "है", "hai", "app", "ऐप", "एप", "एप्प", "application", "the", "a"
    )
    private val connectors = words("पर", "par", "pe", "on", "में", "me", "via", "through")
    private val ko = words("को", "ko")
    private val callWords = words("call", "कॉल", "काल", "फोन", "phone", "dial", "डायल")
    private val sendWords = words("भेजो", "भेज", "भेजें", "भेजिए", "भेजिये", "भेजना", "bhejo", "bhej", "bhejna", "send")
    private val messageWords = words("message", "msg", "मैसेज", "मेसेज", "sms")
    private val shareWords = words("share", "शेयर", "साझा")
    private val typeWords = words("type", "टाइप", "लिखो", "likho", "write", "लिखें", "लिखिए")
    private val clickWords = words("click", "क्लिक", "tap", "टैप", "press", "दबाओ", "दबाएं", "दबाएँ")
    private val scrollWords = words("scroll", "स्क्रॉल", "स्क्रोल")
    private val dirDown = words("down", "नीचे", "niche")
    private val dirUp = words("up", "ऊपर", "upar")
    private val dirLeft = words("left", "बाएं", "बायें", "बाएँ")
    private val dirRight = words("right", "दाएं", "दायें", "दाएँ")
    private val backWords = words("back", "पीछे", "वापस", "वापिस")
    private val homeWords = words("home", "होम")
    private val recentWords = words("recent", "recents", "रीसेंट", "रिसेंट")
    private val screenshotWords = words("screenshot", "स्क्रीनशॉट", "स्क्रीनशाट")
    private val runWords = words("run", "चलाओ", "चला", "चलाइए", "workflow", "वर्कफ्लो", "शुरू", "start", "execute", "routine")

    private val settingsAliases: Map<String, Set<String>> =
        SettingsKeys.aliases.mapValues { (_, v) -> words(*v.toTypedArray()) }

    private class AppAlias(val tokens: List<String>, val key: String)

    private val appAliases: List<AppAlias> = KnownApps.all
        .flatMap { app -> (app.aliases + app.displayName + app.key).map { a -> AppAlias(a.split(" ").map { norm(it) }.filter { it.isNotEmpty() }, app.key) } }
        .filter { it.tokens.isNotEmpty() }
        .sortedByDescending { it.tokens.size }

    // ---- public API ----
    fun parse(raw: String, workflows: List<WorkflowTrigger> = emptyList()): ParsedCommand {
        val text = raw.trim()
        if (text.isEmpty()) return fail(raw, ParseError.EMPTY)
        val toks = text.split(Regex("\\s+"))
            .map { Tok(stripEdges(it), norm(it)) }
            .filter { it.norm.isNotEmpty() }
        if (toks.isEmpty()) return fail(raw, ParseError.EMPTY)
        return matchWorkflow(raw, toks, workflows)
            ?: parseSend(raw, toks)
            ?: parseCall(raw, toks)
            ?: parseShare(raw, toks)
            ?: parseType(raw, toks)
            ?: parseClick(raw, toks)
            ?: parseScroll(raw, toks)
            ?: parseNav(raw, toks)
            ?: parseOpen(raw, toks)
            ?: fail(raw, ParseError.UNRECOGNIZED)
    }

    // ---- individual parsers (return null when the command is not of that kind) ----
    private fun matchWorkflow(raw: String, toks: List<Tok>, wfs: List<WorkflowTrigger>): ParsedCommand? {
        if (wfs.isEmpty()) return null
        val key = cleanForWorkflow(toks.map { it.norm })
        if (key.isEmpty()) return null
        for (w in wfs) {
            val hit = w.phrases.any { p ->
                val c = cleanForWorkflow(p.trim().split(Regex("\\s+")).map { norm(it) }.filter { it.isNotEmpty() })
                c.isNotEmpty() && c == key
            }
            if (hit) return ParsedCommand(raw, CommandType.RUN_WORKFLOW, mapOf(Args.WORKFLOW_ID to w.id))
        }
        return null
    }

    private fun cleanForWorkflow(ws: List<String>) = ws.filter { it !in fillers && it !in runWords }.joinToString(" ")

    private fun parseSend(raw: String, toks: List<Tok>): ParsedCommand? {
        val si = toks.indexOfFirst { it.norm in sendWords }
        if (si < 0) return null
        // "Send पर क्लिक करो" is a click on a Send button, not a message command.
        if (toks.any { it.norm in clickWords }) return null
        val app = findApp(toks) ?: return fail(raw, ParseError.NO_APP)
        val (appStart, appLen, appKey) = app
        val drop = HashSet<Int>()
        drop += si
        for (k in appStart until appStart + appLen) drop += k
        if (appStart - 1 >= 0 && toks[appStart - 1].norm in connectors) drop += appStart - 1
        if (appStart + appLen < toks.size && toks[appStart + appLen].norm in connectors) drop += appStart + appLen
        var j = si + 1
        while (j < toks.size && toks[j].norm in fillers) { drop += j; j++ }
        val rest = toks.filterIndexed { i, _ -> i !in drop }

        val koIdx = rest.indexOfFirst { it.norm in ko }
        val nameToks: List<Tok>
        var msgToks: List<Tok>
        if (koIdx >= 1) {
            nameToks = rest.subList(0, koIdx)
            msgToks = rest.subList(koIdx + 1, rest.size)
        } else {
            val toIdx = rest.indexOfLast { it.norm == "to" }
            if (toIdx >= 1 && toIdx < rest.size - 1) {
                msgToks = rest.subList(0, toIdx)
                nameToks = rest.subList(toIdx + 1, rest.size)
            } else return fail(raw, ParseError.NO_CONTACT)
        }
        if (msgToks.isNotEmpty() && msgToks.first().norm in messageWords) msgToks = msgToks.drop(1)
        if (nameToks.isEmpty()) return fail(raw, ParseError.NO_CONTACT)
        val message = msgToks.joinToString(" ") { it.orig }.trim().trim('"', '\'', '“', '”', '‘', '’')
        if (message.isBlank()) return fail(raw, ParseError.NO_MESSAGE)
        val contact = nameToks.joinToString(" ") { it.orig }
        return ParsedCommand(raw, CommandType.SEND_MESSAGE, mapOf(Args.APP to appKey, Args.CONTACT to contact, Args.MESSAGE to message))
    }

    private fun parseCall(raw: String, toks: List<Tok>): ParsedCommand? {
        if (toks.none { it.norm in callWords } || toks.any { it.norm in open }) return null
        val name = toks.filter { it.norm !in callWords && it.norm !in ko && it.norm !in fillers }
            .joinToString(" ") { it.orig }.trim()
        if (name.isEmpty()) return fail(raw, ParseError.NO_CONTACT)
        val compact = name.replace(" ", "").replace("-", "")
        return if (Regex("^\\+?\\d{3,15}$").matches(compact)) {
            ParsedCommand(raw, CommandType.CALL, mapOf(Args.NUMBER to compact, Args.CONTACT to compact))
        } else {
            ParsedCommand(raw, CommandType.CALL, mapOf(Args.CONTACT to name))
        }
    }

    private fun parseShare(raw: String, toks: List<Tok>): ParsedCommand? {
        val i = toks.indexOfFirst { it.norm in shareWords }
        if (i < 0) return null
        val text = textAround(toks, i)
        return if (text.isBlank()) fail(raw, ParseError.NO_TEXT) else ParsedCommand(raw, CommandType.SHARE, mapOf(Args.TEXT to text))
    }

    private fun parseType(raw: String, toks: List<Tok>): ParsedCommand? {
        val i = toks.indexOfFirst { it.norm in typeWords }
        if (i < 0) return null
        val text = textAround(toks, i)
        return if (text.isBlank()) fail(raw, ParseError.NO_TEXT) else ParsedCommand(raw, CommandType.TYPE, mapOf(Args.TEXT to text))
    }

    private fun parseClick(raw: String, toks: List<Tok>): ParsedCommand? {
        val i = toks.indexOfFirst { it.norm in clickWords }
        if (i < 0) return null
        val target = textAround(toks, i)
        return if (target.isBlank()) fail(raw, ParseError.NO_TARGET) else ParsedCommand(raw, CommandType.CLICK, mapOf(Args.TEXT to target))
    }

    private fun parseScroll(raw: String, toks: List<Tok>): ParsedCommand? {
        if (toks.none { it.norm in scrollWords }) return null
        val dir = when {
            toks.any { it.norm in dirUp } -> "up"
            toks.any { it.norm in dirLeft } -> "left"
            toks.any { it.norm in dirRight } -> "right"
            else -> "down"
        }
        return ParsedCommand(raw, CommandType.SCROLL, mapOf(Args.DIRECTION to dir))
    }

    private fun parseNav(raw: String, toks: List<Tok>): ParsedCommand? = when {
        toks.any { it.norm in screenshotWords } -> ParsedCommand(raw, CommandType.SCREENSHOT)
        toks.any { it.norm in recentWords } -> ParsedCommand(raw, CommandType.RECENTS)
        toks.any { it.norm in backWords } -> ParsedCommand(raw, CommandType.BACK)
        toks.any { it.norm in homeWords } -> ParsedCommand(raw, CommandType.HOME)
        else -> null
    }

    private fun parseOpen(raw: String, toks: List<Tok>): ParsedCommand? {
        if (toks.none { it.norm in open }) return null
        val rest = toks.filter { it.norm !in open && it.norm !in fillers }
        if (rest.isEmpty()) return fail(raw, ParseError.NO_TARGET)
        settingsKey(rest)?.let { return ParsedCommand(raw, CommandType.OPEN_SETTINGS, mapOf(Args.SETTING to it)) }
        findApp(rest)?.let { return ParsedCommand(raw, CommandType.OPEN_APP, mapOf(Args.APP to it.third)) }
        // Unknown name: resolved against installed app labels at execution time.
        return ParsedCommand(raw, CommandType.OPEN_APP, mapOf(Args.APP to rest.joinToString(" ") { it.orig }))
    }

    // ---- helpers ----
    private fun settingsKey(rest: List<Tok>): String? {
        for ((key, aliases) in settingsAliases) {
            if (key != "general" && rest.any { it.norm in aliases }) return key
        }
        val general = settingsAliases["general"].orEmpty()
        return if (rest.any { it.norm in general }) "general" else null
    }

    private fun findApp(toks: List<Tok>): Triple<Int, Int, String>? {
        for (i in toks.indices) {
            for (alias in appAliases) {
                val n = alias.tokens.size
                if (i + n <= toks.size && (0 until n).all { toks[i + it].norm == alias.tokens[it] }) {
                    return Triple(i, n, alias.key)
                }
            }
        }
        return null
    }

    /** Text of the command with the trigger word (and adjacent connector/filler words) removed. */
    private fun textAround(toks: List<Tok>, i: Int): String {
        val drop = HashSet<Int>()
        drop += i
        if (i > 0 && toks[i - 1].norm in connectors) drop += i - 1
        var j = i + 1
        while (j < toks.size && (toks[j].norm in fillers || (j == i + 1 && toks[j].norm in connectors))) { drop += j; j++ }
        return toks.filterIndexed { k, _ -> k !in drop }.joinToString(" ") { it.orig }.trim()
    }

    private fun fail(raw: String, e: ParseError) = ParsedCommand(raw, CommandType.UNKNOWN, error = e)

    private fun stripEdges(s: String): String = s.trim { it in "\"'“”‘’,.!?।;:()[]" }

    /** Lower-case, unify Devanagari nukta/chandrabindu, drop punctuation and joiners. */
    private fun norm(s: String): String {
        var t = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        t = t.replace("\u093C", "").replace("\u0901", "\u0902").replace("\u200C", "").replace("\u200D", "")
        t = Normalizer.normalize(t, Normalizer.Form.NFC)
        return t.filter {
            val type = Character.getType(it)
            it.isLetterOrDigit() || it == '+' ||
                type == Character.NON_SPACING_MARK.toInt() || type == Character.COMBINING_SPACING_MARK.toInt()
        }
    }
}
