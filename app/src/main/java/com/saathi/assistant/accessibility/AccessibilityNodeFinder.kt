package com.saathi.assistant.accessibility

import android.view.accessibility.AccessibilityNodeInfo

sealed interface NodeMatch {
    data class Found(val node: AccessibilityNodeInfo) : NodeMatch
    data object NotFound : NodeMatch
    data object Ambiguous : NodeMatch
}

/** Read-only helpers that locate nodes in the currently visible window. */
object AccessibilityNodeFinder {
    private const val MAX_NODES = 3000

    fun allNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val out = ArrayList<AccessibilityNodeInfo>()
        fun walk(n: AccessibilityNodeInfo?) {
            if (n == null || out.size >= MAX_NODES) return
            out += n
            for (i in 0 until n.childCount) walk(n.getChild(i))
        }
        walk(root)
        return out
    }

    private fun labels(n: AccessibilityNodeInfo): List<String> =
        listOfNotNull(n.text?.toString(), n.contentDescription?.toString()).map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * Finds a clickable-label match. Editable and password fields are never matched (so typing a
     * query can't "find" itself). Exact match wins; otherwise a SINGLE partial match is accepted;
     * several partial matches are reported as ambiguous instead of guessing.
     */
    fun findBest(root: AccessibilityNodeInfo, query: String): NodeMatch {
        val q = query.trim()
        if (q.isEmpty()) return NodeMatch.NotFound
        val candidates = allNodes(root).filter {
            it.isVisibleToUser && !it.isEditable && !it.isPassword && labels(it).isNotEmpty()
        }
        // Prefer exact matches that can actually be clicked. Some apps expose a text label on a
        // parent and the real clickable node one level below/above it.
        candidates.firstOrNull { n ->
            n.isClickable && labels(n).any { it.equals(q, ignoreCase = true) }
        }?.let { return NodeMatch.Found(it) }
        candidates.firstOrNull { n ->
            labels(n).any { it.equals(q, ignoreCase = true) }
        }?.let { return NodeMatch.Found(it) }
        val partial = candidates.filter { n ->
            labels(n).any { it.contains(q, ignoreCase = true) }
        }
        val clickablePartial = partial.filter { it.isClickable }
        return when {
            clickablePartial.size == 1 -> NodeMatch.Found(clickablePartial[0])
            partial.size == 1 -> NodeMatch.Found(partial[0])
            partial.isEmpty() -> NodeMatch.NotFound
            else -> NodeMatch.Ambiguous
        }
    }

    fun findById(root: AccessibilityNodeInfo, viewId: String): AccessibilityNodeInfo? {
        val id = viewId.trim()
        if (id.isEmpty()) return null
        val exact = runCatching { root.findAccessibilityNodeInfosByViewId(id) }
            .getOrNull()
            ?.firstOrNull { it.isVisibleToUser }
        if (exact != null) return exact
        // Workflow exports often contain just `package:id/name` or `name`; accept a resource-id
        // suffix as a practical fallback across app versions.
        return allNodes(root).firstOrNull { n ->
            n.isVisibleToUser && n.viewIdResourceName?.let { it == id || it.endsWith(":$id") || it.endsWith("/$id") } == true
        }
    }

    /** Editable, non-password field: focused one first, else matching hint, else the first one. */
    fun findEditable(root: AccessibilityNodeInfo, hint: String): AccessibilityNodeInfo? {
        val editables = allNodes(root).filter { it.isEditable && it.isVisibleToUser }
        if (editables.isEmpty()) return null
        if (hint.isNotBlank()) {
            editables.firstOrNull { n ->
                (labels(n) + listOfNotNull(n.hintText?.toString())).any { it.contains(hint, ignoreCase = true) }
            }?.let { return it }
        }
        return editables.firstOrNull { it.isFocused } ?: editables.first()
    }

    fun findScrollable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        allNodes(root).firstOrNull { it.isScrollable && it.isVisibleToUser }

    fun clickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var cur: AccessibilityNodeInfo? = node
        var depth = 0
        while (cur != null && depth < 8) {
            if (cur.isClickable) return cur
            cur = cur.parent
            depth++
        }
        return null
    }
}
