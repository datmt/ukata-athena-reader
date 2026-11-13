package io.ukata.athena.spike.selection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SelectionModelTest {
    private val m = SelectionModel(listOf("Hello brave world", "", "Second block here", "Third"))

    @Test fun `word at offset`() {
        assertEquals("brave", m.text(m.wordAt(Anchor(0, 8))))
    }

    @Test fun `word just past end picks last word`() {
        assertEquals("Third", m.text(m.wordAt(Anchor(3, 5))))
    }

    @Test fun `punctuation click selects single char`() {
        val mm = SelectionModel(listOf("a, b"))
        assertEquals(",", mm.text(mm.wordAt(Anchor(0, 1))))
    }

    @Test fun `paragraph`() = assertEquals("Second block here", m.text(m.paragraphAt(2)))

    @Test fun `cross block forward`() {
        val s = Selection(Anchor(0, 6), Anchor(3, 3))
        assertEquals("brave world\nSecond block here\nThi", m.text(s))
    }

    @Test fun `cross block backward equals forward`() {
        val f = Selection(Anchor(0, 6), Anchor(3, 3))
        val b = Selection(Anchor(3, 3), Anchor(0, 6))
        assertEquals(m.text(f), m.text(b))
        assertEquals(f.start, b.start)
    }

    @Test fun `rangeIn per block`() {
        val s = Selection(Anchor(0, 6), Anchor(3, 3))
        assertEquals(6 until 17, s.rangeIn(0, 17))
        assertNull(s.rangeIn(1, 0))
        assertEquals(0 until 17, s.rangeIn(2, 17))
        assertEquals(0 until 3, s.rangeIn(3, 5))
        assertNull(s.rangeIn(4, 5))
    }

    @Test fun `extend keeps anchor and clamps`() {
        val base = m.wordAt(Anchor(0, 0))
        val e = m.extend(base, Anchor(99, 99))
        assertEquals(base.anchor, e.anchor)
        assertEquals(Anchor(3, 5), e.focus)
    }

    @Test fun `empty selection has no text`() {
        assertTrue(m.text(Selection(Anchor(0, 2), Anchor(0, 2))).isEmpty())
    }
}
