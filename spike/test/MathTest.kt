import io.ukata.athena.spike.math.*
import kotlin.test.*

class MathTest {
    @Test fun inlineAndDisplay() {
        System.setProperty("java.awt.headless", "true")
        val inl = renderMath("\\sum_{i=1}^n i = \\frac{n(n+1)}{2}", 18f, display = false)
        val dsp = renderMath("\\sum_{i=1}^n i = \\frac{n(n+1)}{2}", 18f, display = true)
        assertTrue(inl.width > 40 && inl.height > 10)
        assertTrue(dsp.height > inl.height, "display style taller: ${dsp.height} vs ${inl.height}")
        assertTrue(inl.baseline in 1..inl.height)
        val ink = (0 until inl.width).any { x -> (0 until inl.height).any { y -> (inl.image.getRGB(x, y) ushr 24) > 0 } }
        assertTrue(ink)
    }
    @Test fun complexAndInvalid() {
        renderMath("\\int_0^\\infty e^{-x^2}\\,dx=\\frac{\\sqrt{\\pi}}{2},\\quad \\begin{pmatrix}a&b\\\\c&d\\end{pmatrix}", 20f, true)
        assertFails { renderMath("\\frac{1}{", 18f, false) }
        assertFails { renderMath("\\notacommand{x}", 18f, false) }
    }
    @Test fun timing() {
        renderMath("x", 18f, false)
        val t = System.nanoTime()
        repeat(50) { renderMath("\\frac{a_$it}{b}+\\sqrt{x^2+y^2}", 18f, false) }
        val ms = (System.nanoTime() - t) / 50e6
        println("avg render ms: $ms")
        assertTrue(ms < 50, "slow: $ms ms")
    }
}
