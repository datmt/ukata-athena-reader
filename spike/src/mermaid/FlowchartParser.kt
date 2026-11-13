package io.ukata.athena.spike.mermaid

private val EDGE_LABELED = Regex("""^(--|==|-\.)\s+(.+?)\s+(-->|---|==>|===|\.->|\.-)""")
private val EDGE_PLAIN = Regex("""^(-{2,}>|-{3,}|-\.+->|-\.+-|={2,}>|={3,})(?:\|([^|]*)\|)?""")
private val ID = Regex("""^[A-Za-z0-9_]+""")
private val OPENERS = listOf("((" to "))", "([" to "])", "{{" to "}}", "[[" to "]]", "[" to "]", "(" to ")", "{" to "}")

internal fun parseFlowchart(lines: List<String>): Flowchart {
    val h = lines[0].split(Regex("\\s+"))
    val dir = when (h.getOrNull(1)?.removeSuffix(";")) {
        null, "TD", "TB" -> Dir.TD; "LR" -> Dir.LR; "BT" -> Dir.BT; "RL" -> Dir.RL
        else -> throw IllegalArgumentException("bad direction '${h[1]}'")
    }
    val nodes = LinkedHashMap<String, GNode>()
    val edges = ArrayList<GEdge>()
    for (line in lines.drop(1)) for (stmt in line.split(';')) {
        val s = stmt.trim()
        if (s.isEmpty()) continue
        val first = s.split(Regex("\\s+"))[0]
        if (first in setOf("subgraph", "end", "classDef", "class", "style", "linkStyle", "click"))
            throw MermaidUnsupported("'$first'")
        parseChain(s, nodes, edges)
    }
    return Flowchart(dir, nodes.values.toList(), edges)
}

private fun parseChain(s: String, nodes: MutableMap<String, GNode>, edges: MutableList<GEdge>) {
    var rest = s
    fun node(): GNode {
        val id = ID.find(rest)?.value ?: throw IllegalArgumentException("expected node at '${rest.take(20)}'")
        rest = rest.substring(id.length)
        var shape: Shape? = null
        var label: String? = null
        for ((o, c) in OPENERS) if (rest.startsWith(o)) {
            val end = rest.indexOf(c, o.length)
            if (end < 0) throw IllegalArgumentException("unclosed '$o' in node $id")
            label = rest.substring(o.length, end).trim().removeSurrounding("\"").replace("<br/>", "\n").replace("<br>", "\n")
            shape = when (o) {
                "((" -> Shape.Circle; "([" -> Shape.Stadium; "{", "{{" -> Shape.Diamond; "(" -> Shape.Round; else -> Shape.Rect
            }
            rest = rest.substring(end + c.length)
            break
        }
        val n = nodes.getOrPut(id) { GNode(id, id) }
        if (label != null) { n.label = label; n.shape = shape!! }
        return n
    }
    var prev = node()
    while (true) {
        rest = rest.trimStart()
        if (rest.isEmpty()) return
        var op: String; var label: String?
        val m1 = EDGE_LABELED.find(rest)
        if (m1 != null) { op = m1.groupValues[3]; label = m1.groupValues[2]; rest = rest.substring(m1.value.length) }
        else {
            val m2 = EDGE_PLAIN.find(rest) ?: throw IllegalArgumentException("unexpected '${rest.take(20)}'")
            op = m2.groupValues[1]; label = m2.groups[2]?.value?.trim(); rest = rest.substring(m2.value.length)
        }
        rest = rest.trimStart()
        val next = node()
        edges += GEdge(prev.id, next.id, label?.ifEmpty { null }, '.' in op, '=' in op, op.endsWith(">"))
        prev = next
    }
}
