package io.ukata.athena.spike.math

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/** Math as a bitmap sized in dp (rendered at density-scaled px so it stays sharp at 150%/200%). Errors show the source. */
@Composable
fun MathView(latex: String, display: Boolean, fontSizeSp: Float = 18f, modifier: Modifier = Modifier) {
    val d = LocalDensity.current
    val fg = MaterialTheme.colorScheme.onSurface
    val r = remember(latex, display, fontSizeSp, d.density, fg) {
        runCatching {
            val m = renderMath(latex, fontSizeSp * d.density, display, java.awt.Color(fg.toArgb(), true))
            m to m.image.toComposeImageBitmap()
        }
    }
    r.fold(
        { (m, bmp) -> with(d) { Image(bmp, latex, modifier.size(m.width.toDp(), m.height.toDp())) } },
        { Text("math error: ${it.message}\n$latex", modifier, color = MaterialTheme.colorScheme.error) },
    )
}

/** Run: `./kotlin run -m spike -- math` */
fun mathDemo() = application {
    Window(onCloseRequest = ::exitApplication, title = "spike: math") {
        MaterialTheme {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Inline: Euler "); MathView("e^{i\\pi}+1=0", false); Text(" and "); MathView("\\sum_{k=1}^n k^2", false); Text(" in text.")
                }
                MathView("\\int_0^\\infty e^{-x^2}\\,dx=\\frac{\\sqrt{\\pi}}{2}", true, 24f)
                MathView("\\begin{pmatrix}a&b\\\\c&d\\end{pmatrix}\\begin{pmatrix}x\\\\y\\end{pmatrix}=\\begin{pmatrix}ax+by\\\\cx+dy\\end{pmatrix}", true, 22f)
                MathView("\\frac{1}{", true)
            }
        }
    }
}
