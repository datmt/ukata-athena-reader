package io.ukata.athena.spike.selection

/** A point in the document: [offset] chars into block [block] (document-order index). */
data class Anchor(val block: Int, val offset: Int) : Comparable<Anchor> {
    override fun compareTo(other: Anchor) =
        compareValuesBy(this, other, Anchor::block, Anchor::offset)
}

/** Selection with a fixed [anchor] and a moving [focus]; either may come first in the document. */
data class Selection(val anchor: Anchor, val focus: Anchor) {
    val start get() = minOf(anchor, focus)
    val end get() = maxOf(anchor, focus)
    val isEmpty get() = anchor == focus

    /** Selected char range inside [block], or null when the block is outside the selection. */
    fun rangeIn(block: Int, length: Int): IntRange? {
        if (isEmpty || block < start.block || block > end.block) return null
        val from = if (block == start.block) start.offset else 0
        val to = if (block == end.block) end.offset else length
        return if (to > from) from until to else null
    }
}

/** Pure text-side logic of selection; layout/hit-testing lives in the Compose layer. */
class SelectionModel(private val blocks: List<String>) {
    val size get() = blocks.size

    fun clamp(a: Anchor): Anchor {
        val b = a.block.coerceIn(0, blocks.lastIndex)
        return Anchor(b, a.offset.coerceIn(0, blocks[b].length))
    }

    fun wordAt(a: Anchor): Selection {
        val t = blocks[a.block]
        if (t.isEmpty()) return Selection(a, a)
        var s = a.offset.coerceIn(0, t.length - 1)
        // Caret at the very end of the block (click past the last char) picks the last word.
        if (a.offset >= t.length && !isWordChar(t[s]) && s > 0 && isWordChar(t[s - 1])) s--
        if (!isWordChar(t[s])) return Selection(Anchor(a.block, s), Anchor(a.block, s + 1))
        var e = s
        while (s > 0 && isWordChar(t[s - 1])) s--
        while (e < t.length && isWordChar(t[e])) e++
        return Selection(Anchor(a.block, s), Anchor(a.block, e))
    }

    fun paragraphAt(block: Int) = Selection(Anchor(block, 0), Anchor(block, blocks[block].length))

    /** Extend [base] so that it covers [to] keeping the original anchor. */
    fun extend(base: Selection, to: Anchor) = Selection(base.anchor, clamp(to))

    fun text(sel: Selection): String {
        if (sel.isEmpty) return ""
        return (sel.start.block..sel.end.block).mapNotNull { b ->
            sel.rangeIn(b, blocks[b].length)?.let { blocks[b].substring(it.first, it.last + 1) }
        }.joinToString("\n")
    }

    private fun isWordChar(c: Char) = c.isLetterOrDigit() || c == '\'' || c == '’' || c == '-'
}
