package io.ukata.athena.spike.math

import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.JLabel

class MathImage(val image: BufferedImage, val width: Int, val height: Int, val baseline: Int)

/** Renders LaTeX to an ARGB image. `display` = display style (large operators); inline otherwise. Throws on invalid LaTeX. */
fun renderMath(latex: String, sizePx: Float, display: Boolean, fg: Color = Color.BLACK): MathImage {
    requireBalanced(latex)
    val style = if (display) TeXConstants.STYLE_DISPLAY else TeXConstants.STYLE_TEXT
    val icon = TeXFormula(latex).createTeXIcon(style, sizePx)
    icon.insets = java.awt.Insets(2, 2, 2, 2)
    val w = icon.iconWidth; val h = icon.iconHeight
    val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    icon.setForeground(fg)
    icon.paintIcon(JLabel(), g, 0, 0)
    g.dispose()
    return MathImage(img, w, h, h - icon.iconDepth)
}

/** JLaTeXMath silently accepts unclosed groups; reject them so the UI can show the error beside the source. */
internal fun requireBalanced(latex: String) {
    var depth = 0; var i = 0
    while (i < latex.length) {
        when (latex[i]) {
            '\\' -> i++
            '{' -> depth++
            '}' -> if (--depth < 0) throw IllegalArgumentException("unmatched '}'")
        }
        i++
    }
    if (depth != 0) throw IllegalArgumentException("unclosed '{'")
}
