package io.ukata.athena.spike

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ukata.athena.spike.selection.*

private val words = "the quick brown fox jumps over a lazy dog while reading about layered graph layout and selection models".split(' ')

fun demoBlocks(n: Int = 400): List<SpikeBlock> = List(n) { i ->
    val rnd = java.util.Random(i.toLong())
    when {
        i % 25 == 0 -> SpikeBlock("Chapter ${i / 25 + 1}", TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold))
        i % 7 == 3 -> SpikeBlock("• " + List(6 + rnd.nextInt(8)) { words[rnd.nextInt(words.size)] }.joinToString(" "), TextStyle(fontSize = 17.sp))
        else -> SpikeBlock(List(30 + rnd.nextInt(90)) { words[rnd.nextInt(words.size)] }.joinToString(" ") + ".", TextStyle(fontSize = 17.sp, lineHeight = 26.sp))
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "spike: selection") {
        MaterialTheme {
            val blocks = remember { demoBlocks() }
            val model = remember { SelectionModel(blocks.map { it.text }) }
            var sel by remember { mutableStateOf<Selection?>(null) }
            Column(Modifier.fillMaxSize()) {
                SelectableColumn(blocks, sel, { sel = it }, Modifier.weight(1f))
                val s = sel
                Text(
                    s?.let { "${it.start}..${it.end} • ${model.text(it).length} chars: ${model.text(it).take(120).replace('\n', '⏎')}" } ?: "no selection",
                    Modifier.fillMaxWidth().padding(8.dp), maxLines = 2,
                )
            }
        }
    }
}
