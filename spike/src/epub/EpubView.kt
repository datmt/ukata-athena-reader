package io.ukata.athena.spike.epub

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import io.ukata.athena.spike.selection.SpikeBlock

private fun rich(b: Block): AnnotatedString = buildAnnotatedString {
    append(b.text)
    for (s in b.spans) {
        val f = s.flags
        addStyle(
            SpanStyle(
                fontWeight = if (f and BOLD != 0) FontWeight.Bold else null,
                fontStyle = if (f and ITALIC != 0) FontStyle.Italic else null,
                fontFamily = if (f and CODE != 0) FontFamily.Monospace else null,
                color = if (f and LINK != 0) Color(0xFF2563EB) else Color.Unspecified,
                baselineShift = if (f and SCRIPT != 0) BaselineShift.Superscript else null,
                fontSize = if (f and SCRIPT != 0) 12.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
            ),
            s.start, s.end,
        )
    }
}

/** Maps content blocks to display blocks. Images render as their alt text for now (spike). */
fun Chapter.toSpikeBlocks(): List<SpikeBlock> = blocks.map { b ->
    val body = TextStyle(fontSize = 17.sp, lineHeight = 26.sp)
    val style = when (b.kind) {
        Kind.Heading -> TextStyle(fontSize = (30 - 3 * b.level).coerceAtLeast(18).sp, fontWeight = FontWeight.Bold)
        Kind.Pre -> body.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        Kind.Quote -> body.copy(fontStyle = FontStyle.Italic, color = Color(0xFF555555))
        else -> body
    }
    when (b.kind) {
        Kind.ListItem -> SpikeBlock("• " + b.text, style, rich(Block(b.kind, "• " + b.text, b.spans.map { Span(it.start + 2, it.end + 2, it.flags, it.href) })))
        Kind.Image -> SpikeBlock("[image: ${b.text}]", style.copy(color = Color.Gray))
        else -> SpikeBlock(b.text, style, rich(b))
    }
}
