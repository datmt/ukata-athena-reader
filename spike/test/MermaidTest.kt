import io.ukata.athena.spike.mermaid.*
import kotlin.test.*

class MermaidTest {
    private val measure: (String) -> Pair<Float, Float> = { it.length * 8f to 16f }

    @Test fun flowchartParse() {
        val f = parseMermaid("""
            flowchart LR
              A[Start] --> B{Is it?}
              B -->|yes| C(Done); B -- no --> D((Loop))
              D -.-> A
              C ==> E
        """.trimIndent()) as Flowchart
        assertEquals(Dir.LR, f.dir)
        assertEquals(listOf("A", "B", "C", "D", "E"), f.nodes.map { it.id })
        assertEquals(Shape.Diamond, f.nodes[1].shape)
        assertEquals("Is it?", f.nodes[1].label)
        assertEquals(Shape.Circle, f.nodes[3].shape)
        assertEquals(5, f.edges.size)
        assertEquals("yes", f.edges[1].label)
        assertEquals("no", f.edges[2].label)
        assertTrue(f.edges[3].dotted); assertTrue(f.edges[4].thick)
    }

    @Test fun chainAndBareNodes() {
        val f = parseMermaid("graph TD\nA --> B --> C\nA --- C") as Flowchart
        assertEquals(3, f.edges.size)
        assertFalse(f.edges[2].arrow)
    }

    @Test fun layoutLayersAndNoOverlap() {
        val f = parseMermaid("graph TD\nA-->B\nA-->C\nB-->D\nC-->D\nA-->D") as Flowchart
        val l = layoutFlowchart(f, measure)
        val by = l.nodes.associateBy { it.node.id }
        assertTrue(by["A"]!!.y < by["B"]!!.y && by["B"]!!.y < by["D"]!!.y)
        assertEquals(by["B"]!!.y, by["C"]!!.y)
        for (a in l.nodes) for (b in l.nodes) if (a !== b) {
            val sep = a.x + a.w <= b.x || b.x + b.w <= a.x || a.y + a.h <= b.y || b.y + b.h <= a.y
            assertTrue(sep, "${a.node.id} overlaps ${b.node.id}")
        }
        // A->D spans 2 layers: path has a dummy waypoint.
        assertEquals(3, l.edges.first { it.edge.from == "A" && it.edge.to == "D" }.pts.size)
        assertTrue(l.nodes.all { it.x >= 0 && it.y >= 0 && it.x + it.w <= l.width + 0.01f })
    }

    @Test fun cycleAndHorizontal() {
        val f = parseMermaid("graph LR\nA-->B\nB-->C\nC-->A") as Flowchart
        val l = layoutFlowchart(f, measure)
        val by = l.nodes.associateBy { it.node.id }
        assertTrue(by["A"]!!.x < by["B"]!!.x && by["B"]!!.x < by["C"]!!.x)
        val back = l.edges.first { it.edge.from == "C" }.pts   // reversed edge points C -> A
        assertTrue(back.first().first > back.last().first)
    }

    @Test fun sequence() {
        val s = parseMermaid("""
            sequenceDiagram
              participant A as Alice
              actor B
              A->>B: Hello
              B-->>A: Hi back
              A->>A: think
              C-)A: async
        """.trimIndent()) as Sequence
        assertEquals(listOf("Alice", "B", "C"), s.participants)
        assertEquals(4, s.messages.size)
        assertTrue(s.messages[1].dashed); assertTrue(s.messages[1].arrow)
        assertEquals(s.messages[2].from, s.messages[2].to)
        val l = layoutSequence(s, measure)
        assertTrue(l.xs.zipWithNext().all { (a, b) -> a < b })
        assertTrue(l.ys.zipWithNext().all { (a, b) -> a < b })
    }

    @Test fun unsupportedAndErrors() {
        assertFailsWith<MermaidUnsupported> { parseMermaid("pie\n\"a\": 1") }
        assertFailsWith<MermaidUnsupported> { parseMermaid("graph TD\nsubgraph x\nA-->B\nend") }
        assertFailsWith<MermaidUnsupported> { parseMermaid("sequenceDiagram\nA->>B: x\nloop y\nend") }
        assertFailsWith<IllegalArgumentException> { parseMermaid("graph TD\nA[oops --> B") }
    }
}
