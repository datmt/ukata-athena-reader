package io.ukata.athena.spike.mermaid

class PNode(val node: GNode, var x: Float, var y: Float, val w: Float, val h: Float)  // x,y = top-left
class PEdge(val edge: GEdge, val pts: List<Pair<Float, Float>>)
class FlowLayout(val nodes: List<PNode>, val edges: List<PEdge>, val width: Float, val height: Float)

private const val LAYER_GAP = 48f
private const val NODE_GAP = 32f
private const val PAD = 12f

/** Layered (Sugiyama-style) layout: DFS cycle removal, longest-path layering, dummy nodes, barycenter ordering, neighbour-average x. */
fun layoutFlowchart(fc: Flowchart, measure: (String) -> Pair<Float, Float>): FlowLayout {
    val horizontal = fc.dir == Dir.LR || fc.dir == Dir.RL
    val n = fc.nodes.size
    if (n == 0) return FlowLayout(emptyList(), emptyList(), 0f, 0f)
    val index = fc.nodes.withIndex().associate { it.value.id to it.index }
    // Sizes in "flow space": layers stack along y, so swap for horizontal flow.
    val sw = FloatArray(n); val sh = FloatArray(n)
    fc.nodes.forEachIndexed { i, g ->
        val (tw, th) = measure(g.label)
        var w = tw + 2 * PAD; var h = th + 2 * PAD
        if (g.shape == Shape.Circle) { val d = maxOf(w, h); w = d; h = d }
        if (g.shape == Shape.Diamond) { w *= 1.5f; h *= 1.5f }
        if (horizontal) { sw[i] = h; sh[i] = w } else { sw[i] = w; sh[i] = h }
    }
    // Edges as index pairs (skip self loops), cycle removal by DFS.
    val raw = fc.edges.mapIndexedNotNull { k, e -> val a = index[e.from]!!; val b = index[e.to]!!; if (a == b) null else Triple(k, a, b) }
    val out = Array(n) { ArrayList<Int>() }
    raw.forEach { (_, a, b) -> out[a] += b }
    val state = IntArray(n); val back = HashSet<Pair<Int, Int>>()
    fun dfs(u: Int) { state[u] = 1; for (v in out[u]) { if (state[v] == 1) back += u to v else if (state[v] == 0) dfs(v) }; state[u] = 2 }
    for (i in 0 until n) if (state[i] == 0) dfs(i)
    val dag = raw.map { (k, a, b) -> if ((a to b) in back) Triple(k, b, a) else Triple(k, a, b) }
    // Longest-path layering.
    val layer = IntArray(n)
    val indeg = IntArray(n); dag.forEach { indeg[it.third]++ }
    val succ = Array(n) { ArrayList<Int>() }; dag.forEach { succ[it.second] += it.third }
    val q = ArrayDeque<Int>((0 until n).filter { indeg[it] == 0 })
    while (q.isNotEmpty()) { val u = q.removeFirst(); for (v in succ[u]) { layer[v] = maxOf(layer[v], layer[u] + 1); if (--indeg[v] == 0) q += v } }
    // Dummy nodes for long edges. Node ids >= n are dummies.
    val dl = ArrayList<Int>(); layer.forEach { dl += it }
    val dw = ArrayList<Float>(); val dh = ArrayList<Float>(); sw.forEach { dw += it }; sh.forEach { dh += it }
    val chains = ArrayList<Pair<Int, List<Int>>>()  // original edge idx -> chain in flow direction (top→bottom)
    val links = ArrayList<Pair<Int, Int>>()
    for ((k, a, b) in dag) {
        val chain = arrayListOf(a)
        for (l in layer[a] + 1 until layer[b]) { dl += l; dw += 8f; dh += 0f; chain += dl.lastIndex }
        chain += b
        chain.zipWithNext().forEach { links += it }
        chains += k to chain
    }
    val total = dl.size
    val up = Array(total) { ArrayList<Int>() }; val down = Array(total) { ArrayList<Int>() }
    links.forEach { (a, b) -> down[a] += b; up[b] += a }
    val layers = Array((dl.maxOrNull() ?: 0) + 1) { ArrayList<Int>() }
    for (i in 0 until total) layers[dl[i]] += i
    // Barycenter ordering sweeps.
    val pos = IntArray(total)
    fun renumber() { layers.forEach { l -> l.forEachIndexed { p, v -> pos[v] = p } } }
    renumber()
    repeat(6) { pass ->
        val rng = if (pass % 2 == 0) layers.indices.drop(1) else layers.indices.reversed().drop(1)
        for (li in rng) {
            val ref = if (pass % 2 == 0) up else down
            val l = layers[li]
            val bc = l.associateWith { v -> ref[v].takeIf { it.isNotEmpty() }?.map { pos[it] }?.average() ?: pos[v].toDouble() }
            l.sortBy { bc[it] }
            l.forEachIndexed { p, v -> pos[v] = p }
        }
    }
    // Coordinates: y per layer, x by sequential packing then neighbour-average relaxation.
    val x = FloatArray(total); val y = FloatArray(total)
    var cy = 0f
    val layerH = layers.map { l -> l.maxOfOrNull { dh[it] } ?: 0f }
    layers.forEachIndexed { li, l ->
        l.forEach { y[it] = cy + (layerH[li] - dh[it]) / 2 }
        cy += layerH[li] + LAYER_GAP
    }
    layers.forEach { l -> var cx = 0f; l.forEach { v -> x[v] = cx + dw[v] / 2; cx += dw[v] + NODE_GAP } }
    fun settle(l: List<Int>, nb: Array<ArrayList<Int>>) {
        val want = l.map { v -> if (nb[v].isEmpty()) x[v] else nb[v].map { x[it] }.average().toFloat() }
        var minX = -Float.MAX_VALUE
        l.forEachIndexed { i, v -> x[v] = maxOf(want[i], minX + (if (i > 0) dw[l[i - 1]] / 2 + dw[v] / 2 + NODE_GAP else 0f)).also { minX = it } }
    }
    repeat(4) {
        for (li in 1 until layers.size) settle(layers[li], up)
        for (li in layers.size - 2 downTo 0) settle(layers[li], down)
    }
    val minX = (0 until total).minOf { x[it] - dw[it] / 2 }
    for (i in 0 until total) x[i] -= minX
    val fw = (0 until total).maxOf { x[it] + dw[it] / 2 }
    val fh = cy - LAYER_GAP
    // Map out of flow space: transpose for horizontal; mirror for BT/RL.
    fun mx(fx: Float, fy: Float): Pair<Float, Float> {
        var px = fx; var py = fy
        if (fc.dir == Dir.BT || fc.dir == Dir.RL) py = fh - py
        return if (horizontal) py to px else px to py
    }
    val pnodes = fc.nodes.indices.map { i ->
        val (cx, cyy) = mx(x[i], y[i] + dh[i] / 2)
        val (w, h) = if (horizontal) sh[i] to sw[i] else sw[i] to sh[i]
        PNode(fc.nodes[i], cx - w / 2, cyy - h / 2, w, h)
    }
    val pedges = ArrayList<PEdge>()
    for ((k, chain) in chains) {
        val e = fc.edges[k]
        val pts = chain.mapIndexed { ci, v ->
            val fy = when (ci) { 0 -> y[v] + dh[v]; chain.lastIndex -> y[v]; else -> y[v] }
            mx(x[v], fy)
        }
        val reversed = (index[e.from]!! to index[e.to]!!) in back
        pedges += PEdge(e, if (reversed) pts.reversed() else pts)
    }
    val (tw, th) = if (horizontal) fh to fw else fw to fh
    return FlowLayout(pnodes, pedges, tw, th)
}
