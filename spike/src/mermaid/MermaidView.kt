package io.ukata.athena.spike.mermaid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Renders Mermaid source natively; on unsupported/invalid input shows the source with a label (source never altered). */
@Composable
fun MermaidView(source: String, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    val style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    val line = MaterialTheme.colorScheme.onSurface
    val fill = MaterialTheme.colorScheme.surfaceVariant
    val density = LocalDensity.current
    val parsed = remember(source) { runCatching { parseMermaid(source) } }
    val measure: (String) -> Pair<Float, Float> = { t -> tm.measure(t, style).size.let { it.width.toFloat() to it.height.toFloat() } }
    val d = parsed.getOrNull()
    when (d) {
        is Flowchart -> {
            val lay = remember(d, density) { layoutFlowchart(d, measure) }
            Canvas(modifier.size(with(density) { (lay.width + 16).toDp() }, with(density) { (lay.height + 16).toDp() })) {
                drawFlow(lay, tm, style, line, fill)
            }
        }
        is Sequence -> {
            val lay = remember(d, density) { layoutSequence(d, measure) }
            Canvas(modifier.size(with(density) { (lay.width + 16).toDp() }, with(density) { (lay.height + 16).toDp() })) {
                drawSeq(d, lay, tm, style, line, fill)
            }
        }
        else -> Text(
            (parsed.exceptionOrNull()?.let { if (it is MermaidUnsupported) "Preview not available: unsupported ${it.message}" else "Diagram error: ${it.message}" } ?: "") + "\n\n" + source,
            modifier, style = TextStyle(fontSize = 13.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
        )
    }
}

private fun DrawScope.arrowHead(from: Offset, to: Offset, color: Color) {
    val a = atan2(to.y - from.y, to.x - from.x); val s = 9f
    val p = Path().apply {
        moveTo(to.x, to.y)
        lineTo(to.x - s * cos(a - 0.4f), to.y - s * sin(a - 0.4f))
        lineTo(to.x - s * cos(a + 0.4f), to.y - s * sin(a + 0.4f)); close()
    }
    drawPath(p, color)
}

private fun DrawScope.label(tm: TextMeasurer, t: String, style: TextStyle, cx: Float, cy: Float, bg: Color? = null) {
    val r = tm.measure(t, style)
    val tl = Offset(cx - r.size.width / 2, cy - r.size.height / 2)
    if (bg != null) drawRect(bg, tl - Offset(3f, 1f), Size(r.size.width + 6f, r.size.height + 2f))
    drawText(r, topLeft = tl)
}

private fun DrawScope.drawFlow(lay: FlowLayout, tm: TextMeasurer, style: TextStyle, line: Color, fill: Color) {
    val o = Offset(8f, 8f)
    for (e in lay.edges) {
        val pts = e.pts.map { Offset(it.first, it.second) + o }
        if (pts.size < 2) continue
        val path = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
        drawPath(path, line, style = Stroke(if (e.edge.thick) 3f else 1.5f, pathEffect = if (e.edge.dotted) PathEffect.dashPathEffect(floatArrayOf(6f, 5f)) else null))
        if (e.edge.arrow) arrowHead(pts[pts.size - 2], pts.last(), line)
        e.edge.label?.let { val m = pts[pts.size / 2].let { a -> val b = pts[(pts.size - 1) / 2]; Offset((a.x + b.x) / 2, (a.y + b.y) / 2) }; label(tm, it, style, m.x, m.y, Color.White.copy(alpha = 0.85f)) }
    }
    for (n in lay.nodes) {
        val tl = Offset(n.x, n.y) + o; val sz = Size(n.w, n.h); val c = tl + Offset(n.w / 2, n.h / 2)
        when (n.node.shape) {
            Shape.Rect -> { drawRect(fill, tl, sz); drawRect(line, tl, sz, style = Stroke(1.5f)) }
            Shape.Round, Shape.Stadium -> {
                val r = androidx.compose.ui.geometry.CornerRadius(if (n.node.shape == Shape.Stadium) n.h / 2 else 10f)
                drawRoundRect(fill, tl, sz, r); drawRoundRect(line, tl, sz, r, style = Stroke(1.5f))
            }
            Shape.Circle -> { drawCircle(fill, n.w / 2, c); drawCircle(line, n.w / 2, c, style = Stroke(1.5f)) }
            Shape.Diamond -> {
                val p = Path().apply { moveTo(c.x, tl.y); lineTo(tl.x + n.w, c.y); lineTo(c.x, tl.y + n.h); lineTo(tl.x, c.y); close() }
                drawPath(p, fill); drawPath(p, line, style = Stroke(1.5f))
            }
        }
        label(tm, n.node.label, style, c.x, c.y)
    }
}

private fun DrawScope.drawSeq(s: Sequence, lay: SeqLayout, tm: TextMeasurer, style: TextStyle, line: Color, fill: Color) {
    val o = Offset(8f, 8f)
    s.participants.forEachIndexed { i, name ->
        val w = lay.headW[i]; val x = lay.xs[i]
        drawLine(line.copy(alpha = 0.5f), Offset(x, lay.headH) + o, Offset(x, lay.height) + o, 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)))
        drawRect(fill, Offset(x - w / 2, 0f) + o, Size(w, lay.headH)); drawRect(line, Offset(x - w / 2, 0f) + o, Size(w, lay.headH), style = Stroke(1.5f))
        label(tm, name, style, x + o.x, lay.headH / 2 + o.y)
    }
    s.messages.forEachIndexed { i, m ->
        val y = lay.ys[i] + o.y; val x1 = lay.xs[m.from] + o.x; val x2 = lay.xs[m.to] + o.x
        val eff = if (m.dashed) PathEffect.dashPathEffect(floatArrayOf(6f, 5f)) else null
        if (m.from == m.to) {
            val p = Path().apply { moveTo(x1, y - 8f); lineTo(x1 + 30f, y - 8f); lineTo(x1 + 30f, y + 8f); lineTo(x1, y + 8f) }
            drawPath(p, line, style = Stroke(1.5f, pathEffect = eff)); if (m.arrow) arrowHead(Offset(x1 + 30f, y + 8f), Offset(x1, y + 8f), line)
            label(tm, m.text, style, x1 + 34f + tm.measure(m.text, style).size.width / 2, y)
        } else {
            drawLine(line, Offset(x1, y), Offset(x2, y), 1.5f, pathEffect = eff)
            if (m.arrow) arrowHead(Offset(x1, y), Offset(x2, y), line)
            label(tm, m.text, style, (x1 + x2) / 2, y - 12f)
        }
    }
}
