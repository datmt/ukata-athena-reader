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
import io.ukata.athena.spike.epub.toSpikeBlocks

private val words = "the quick brown fox jumps over a lazy dog while reading about layered graph layout and selection models".split(' ')

fun demoBlocks(n: Int = 400): List<SpikeBlock> = List(n) { i ->
    val rnd = java.util.Random(i.toLong())
    when {
        i % 25 == 0 -> SpikeBlock("Chapter ${i / 25 + 1}", TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold))
        i % 7 == 3 -> SpikeBlock("• " + List(6 + rnd.nextInt(8)) { words[rnd.nextInt(words.size)] }.joinToString(" "), TextStyle(fontSize = 17.sp))
        else -> SpikeBlock(List(30 + rnd.nextInt(90)) { words[rnd.nextInt(words.size)] }.joinToString(" ") + ".", TextStyle(fontSize = 17.sp, lineHeight = 26.sp))
    }
}

fun main(args: Array<String>) {
    if (args.firstOrNull() == "mermaid") return io.ukata.athena.spike.mermaid.mermaidDemo(args.getOrNull(1))
    if (args.firstOrNull() == "math") return io.ukata.athena.spike.math.mathDemo()
    if (args.firstOrNull() == "selection") return reader(args.drop(1).toTypedArray())
    launcher()
}

/** Default entry (packaged app): tabs for every spike demo. */
fun launcher() = application {
    Window(onCloseRequest = ::exitApplication, title = "Athena spikes") {
        MaterialTheme {
            var tab by remember { mutableStateOf(0) }
            var epubPath by remember { mutableStateOf<String?>(null) }
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Selection", "EPUB", "Mermaid", "Math").forEachIndexed { i, t ->
                        androidx.compose.material3.FilterChip(tab == i, { tab = i }, { Text(t) })
                    }
                    if (tab == 1) androidx.compose.material3.TextButton({
                        val d = java.awt.FileDialog(null as java.awt.Frame?, "Open EPUB", java.awt.FileDialog.LOAD)
                        d.isVisible = true
                        d.file?.let { epubPath = java.io.File(d.directory, it).path }
                    }) { Text("Open EPUB…") }
                }
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        0 -> ReaderContent(null)
                        1 -> ReaderContent(epubPath)
                        2 -> io.ukata.athena.spike.mermaid.MermaidDemoContent()
                        else -> io.ukata.athena.spike.math.MathDemoContent()
                    }
                }
            }
        }
    }
}

fun reader(args: Array<String>) = application {
    Window(onCloseRequest = ::exitApplication, title = "spike: selection") { MaterialTheme { ReaderContent(args.firstOrNull()) } }
}

@Composable
fun ReaderContent(path: String?) {
    val epub = remember(path) { path?.let { io.ukata.athena.spike.epub.EpubBook.open(it) } }
    var chapterIdx by remember(path) { mutableStateOf(0) }
    run {
        run {
            val blocks = remember(chapterIdx) { epub?.chapter(chapterIdx)?.toSpikeBlocks() ?: demoBlocks() }
            val model = remember { SelectionModel(blocks.map { it.text }) }
            var sel by remember { mutableStateOf<Selection?>(null) }
            Column(Modifier.fillMaxSize()) {
                if (epub != null) Row(Modifier.fillMaxWidth().padding(4.dp)) {
                    androidx.compose.material3.TextButton({ if (chapterIdx > 0) { chapterIdx--; sel = null } }) { Text("◀") }
                    Text("${epub.title ?: "?"} — ${epub.spine[chapterIdx].title ?: "chapter ${chapterIdx + 1}"} (${chapterIdx + 1}/${epub.spine.size})", Modifier.weight(1f).padding(8.dp))
                    androidx.compose.material3.TextButton({ if (chapterIdx < epub.spine.lastIndex) { chapterIdx++; sel = null } }) { Text("▶") }
                }
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
