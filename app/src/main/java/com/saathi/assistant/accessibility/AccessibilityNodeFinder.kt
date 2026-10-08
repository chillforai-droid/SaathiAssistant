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
        val candidates = allNodes(root).filter { it.isVisibleToUser && !it.isEditable && !it.isPassword && labels(it).isNotEmpty() }
        candidates.firstOrNull { n -> labels(n).any { it.equals(q, ignoreCase = true) } }?.let { return NodeMatch.Found(it) }
        val partial = candidates.filter { n -> labels(n).any { it.contains(q, ignoreCase = true) } }
        return when (partial.size) {
            0 -> NodeMatch.NotFound
            1 -> NodeMatch.Found(partial[0])
            else -> NodeMatch.Ambiguous
        }
    }

    fun findById(root: AccessibilityNodeInfo, viewId: String): AccessibilityNodeInfo? =
        root.findAccessibilityNodeInfosByViewId(viewId).firstOrNull { it.isVisibleToUser }

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
