package io.ukata.athena.spike.mermaid

enum class Shape { Rect, Round, Stadium, Circle, Diamond }
enum class Dir { TD, LR, BT, RL }

data class GNode(val id: String, var label: String, var shape: Shape = Shape.Rect)
data class GEdge(val from: String, val to: String, val label: String?, val dotted: Boolean, val thick: Boolean, val arrow: Boolean)
data class Flowchart(val dir: Dir, val nodes: List<GNode>, val edges: List<GEdge>)

data class Message(val from: Int, val to: Int, val text: String, val dashed: Boolean, val arrow: Boolean)
data class Sequence(val participants: List<String>, val messages: List<Message>)

class MermaidUnsupported(msg: String) : Exception(msg)

/** Parses a Mermaid source. Throws [MermaidUnsupported] (type/syntax outside the subset) or IllegalArgumentException (malformed). */
fun parseMermaid(src: String): Any {
    val lines = src.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("%%") }
    val head = lines.firstOrNull() ?: throw IllegalArgumentException("empty diagram")
    val kw = head.split(Regex("\\s+"))[0]
    return when {
        kw == "flowchart" || kw == "graph" -> parseFlowchart(lines)
        kw == "sequenceDiagram" -> parseSequence(lines)
        else -> throw MermaidUnsupported("diagram type '$kw'")
    }
}
