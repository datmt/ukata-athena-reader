package io.ukata.athena.spike.mermaid

private val MSG = Regex("""^(\S+?)\s*(-->>|->>|-->|->|--x|-x|--\)|-\))\s*([+-]?)(\S+?)\s*:\s*(.*)$""")
private val DECL = Regex("""^(participant|actor)\s+(\S+)(?:\s+as\s+(.+))?$""")
private val IGNORED = setOf("autonumber", "activate", "deactivate", "title")
private val BLOCKS = setOf("loop", "alt", "else", "opt", "par", "and", "critical", "break", "rect", "end", "Note", "note", "box")

internal fun parseSequence(lines: List<String>): Sequence {
    val ids = ArrayList<String>(); val names = HashMap<String, String>()
    fun idx(id: String): Int { var i = ids.indexOf(id); if (i < 0) { ids += id; i = ids.lastIndex }; return i }
    val msgs = ArrayList<Message>()
    for (l in lines.drop(1)) {
        val kw = l.split(Regex("\\s+"))[0]
        DECL.find(l)?.let { m ->
            idx(m.groupValues[2]); m.groups[3]?.let { names[m.groupValues[2]] = it.value.trim() }
            continue
        }
        if (kw in IGNORED) continue
        if (kw in BLOCKS) throw MermaidUnsupported("'$kw'")
        val m = MSG.find(l) ?: throw IllegalArgumentException("cannot parse '$l'")
        val op = m.groupValues[2]
        msgs += Message(idx(m.groupValues[1]), idx(m.groupValues[4]), m.groupValues[5], op.startsWith("--"), op.endsWith(">") || op.endsWith(")"))
    }
    return Sequence(ids.map { names[it] ?: it }, msgs)
}
