package io.ukata.athena.spike.selection

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SpikeBlock(val text: String, val style: TextStyle, val rich: androidx.compose.ui.text.AnnotatedString? = null)

private const val H_PAD = 24f      // dp, must match padding below
private const val V_GAP = 6        // dp between blocks (inside item so hit-test has no gaps)
private const val EDGE = 40f       // px band that triggers auto-scroll while dragging

/**
 * Cross-block selection: ONE pointer handler on the container, a [SelectionModel] for text logic,
 * and per-block [TextLayoutResult]s only for hit-testing/highlight of *visible* blocks.
 */
@Composable
fun SelectableColumn(
    blocks: List<SpikeBlock>,
    selection: Selection?,
    onSelection: (Selection?) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
) {
    val model = remember(blocks) { SelectionModel(blocks.map { it.text }) }
    val layouts = remember(blocks) { arrayOfNulls<TextLayoutResult>(blocks.size) }
    val scope = rememberCoroutineScope()
    val sel by rememberUpdatedState(selection)
    val emit by rememberUpdatedState(onSelection)

    fun hit(p: Offset, density: Float): Anchor? {
        val items = state.layoutInfo.visibleItemsInfo
        if (items.isEmpty()) return null
        val item = items.firstOrNull { p.y < it.offset + it.size } ?: items.last()
        val layout = layouts[item.index] ?: return null
        val local = Offset(p.x - H_PAD * density, (p.y - item.offset).coerceIn(0f, layout.size.height.toFloat() - 1))
        return Anchor(item.index, layout.getOffsetForPosition(local))
    }

    Box(
        modifier
            .pointerInput(blocks) {
                val d = density
                var lastDown = 0L; var lastPos = Offset.Zero; var clicks = 0
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val now = down.uptimeMillis
                    clicks = if (now - lastDown < 400 && (down.position - lastPos).getDistance() < 8f) clicks + 1 else 1
                    lastDown = now; lastPos = down.position
                    val start = hit(down.position, d) ?: return@awaitEachGesture
                    val extendFrom = if (clicks == 1 && currentEvent.keyboardModifiers.isShiftPressed) sel else null
                    val base = when {
                        extendFrom != null -> model.extend(extendFrom, start)
                        clicks == 2 -> model.wordAt(start)
                        clicks >= 3 -> model.paragraphAt(start.block)
                        else -> Selection(start, start)
                    }
                    emit(if (clicks == 1 && extendFrom == null) null else base)
                    var pointer = down.position
                    var scroller: kotlinx.coroutines.Job? = null
                    fun update() {
                        val a = hit(pointer, d) ?: return
                        val s = if (clicks == 1) Selection(extendFrom?.anchor ?: start, a) else {
                            // Word/paragraph drag: grow by whole units on either side.
                            val unit = if (clicks == 2) model.wordAt(a) else model.paragraphAt(a.block)
                            val lo = minOf(base.start, unit.start); val hi = maxOf(base.end, unit.end)
                            if (a < base.start) Selection(hi, lo) else Selection(lo, hi)
                        }
                        emit(s)
                    }
                    do {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.first()
                        pointer = ch.position
                        if (ev.type == PointerEventType.Move && ch.pressed) {
                            update(); ch.consume()
                            val dir = when { pointer.y < EDGE -> -1; pointer.y > size.height - EDGE -> 1; else -> 0 }
                            if (dir == 0) { scroller?.cancel(); scroller = null }
                            else if (scroller == null) scroller = scope.launch {
                                while (isActive) { state.scrollBy(dir * 24f * d.coerceAtLeast(1f)); update(); delay(16) }
                            }
                        }
                    } while (ch.pressed)
                    scroller?.cancel()
                }
            },
    ) {
        LazyColumn(state = state, userScrollEnabled = true) {
            itemsIndexed(blocks, key = { i, _ -> i }) { i, b ->
                Box(Modifier.padding(horizontal = H_PAD.dp, vertical = (V_GAP / 2).dp)) {
                    BasicText(
                        b.rich ?: androidx.compose.ui.text.AnnotatedString(b.text), style = b.style,
                        onTextLayout = { layouts[i] = it },
                        modifier = Modifier.drawBehind {
                            val r = sel?.rangeIn(i, b.text.length) ?: return@drawBehind
                            val path = layouts[i]?.getPathForRange(r.first, r.last + 1) ?: return@drawBehind
                            drawPath(path, Color(0x663B82F6))
                        },
                    )
                }
            }
        }
    }
}
