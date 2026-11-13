package io.ukata.athena.spike.mermaid

class SeqLayout(val xs: List<Float>, val headW: List<Float>, val headH: Float, val ys: List<Float>, val width: Float, val height: Float)

fun layoutSequence(s: Sequence, measure: (String) -> Pair<Float, Float>): SeqLayout {
    val heads = s.participants.map { measure(it).first + 24f }
    val headH = (s.participants.maxOfOrNull { measure(it).second } ?: 14f) + 16f
    // Column spacing: wide enough for the widest message label between neighbours.
    val gap = FloatArray(maxOf(s.participants.size - 1, 0)) { 0f }
    for (m in s.messages) {
        val lo = minOf(m.from, m.to); val hi = maxOf(m.from, m.to)
        if (lo == hi) continue
        val per = (measure(m.text).first + 24f) / (hi - lo)
        for (g in lo until hi) gap[g] = maxOf(gap[g], per)
    }
    val xs = ArrayList<Float>(); var cx = 0f
    for (i in s.participants.indices) {
        if (i == 0) cx = heads[0] / 2
        else cx += maxOf(gap[i - 1], (heads[i - 1] + heads[i]) / 2 + 16f)
        xs += cx
    }
    val ys = ArrayList<Float>(); var cy = headH + 32f
    for (m in s.messages) { val lh = measure(m.text).second; ys += cy + lh; cy += lh + 36f }
    val w = (xs.lastOrNull() ?: 0f) + (heads.lastOrNull() ?: 0f) / 2
    return SeqLayout(xs, heads, headH, ys, w, cy)
}
