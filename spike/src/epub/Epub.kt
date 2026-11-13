package io.ukata.athena.spike.epub

import java.io.Closeable
import java.util.zip.ZipFile
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants.*
import javax.xml.stream.XMLStreamReader

/** Flat, layout-agnostic content model. A chapter is a list of [Block]s; a location is (chapter, block, offset). */
enum class Kind { Heading, Paragraph, ListItem, Quote, Pre, Image }

const val BOLD = 1
const val ITALIC = 2
const val CODE = 4
const val LINK = 8
const val SCRIPT = 16 // sup/sub

class Span(val start: Int, val end: Int, val flags: Int, val href: String? = null)

class Block(val kind: Kind, val text: String, val spans: List<Span> = emptyList(), val level: Int = 0, val src: String? = null)

class Chapter(val href: String, val title: String?, val blocks: List<Block>)

class SpineItem(val href: String, val title: String?)

/**
 * EPUB reader on the JDK only (zip + StAX). Opening parses just container/OPF/TOC;
 * chapters are parsed lazily by [chapter] so memory stays proportional to what is being read.
 */
class EpubBook private constructor(
    private val zip: ZipFile,
    val title: String?,
    val author: String?,
    val spine: List<SpineItem>,
    private val base: String,
) : Closeable {
    fun chapter(index: Int): Chapter {
        val item = spine[index]
        val bytes = read(item.href) ?: error("missing ${item.href}")
        return Chapter(item.href, item.title, parseXhtml(bytes))
    }

    fun resource(href: String): ByteArray? = read(href)

    private fun read(href: String): ByteArray? =
        zip.getEntry(href)?.let { zip.getInputStream(it).use { s -> s.readBytes() } }

    override fun close() = zip.close()

    companion object {
        fun open(path: String): EpubBook {
            val zip = ZipFile(path)
            try {
                fun entry(n: String) = zip.getEntry(n)?.let { zip.getInputStream(it).use { s -> s.readBytes() } }
                val container = entry("META-INF/container.xml") ?: error("not an EPUB: no container.xml")
                val opfPath = firstAttr(container, "rootfile", "full-path") ?: error("no rootfile")
                val opf = entry(opfPath) ?: error("missing $opfPath")
                val dir = opfPath.substringBeforeLast('/', "")
                val pkg = parseOpf(opf)

                val titles = HashMap<String, String>()
                val tocHref = pkg.navHref ?: pkg.ncxHref
                if (tocHref != null) entry(resolve(dir, tocHref))?.let { bytes ->
                    val tocDir = resolve(dir, tocHref).substringBeforeLast('/', "")
                    parseToc(bytes, pkg.navHref != null).forEach { (h, t) ->
                        titles.putIfAbsent(resolve(tocDir, h.substringBefore('#')), t)
                    }
                }
                val spine = pkg.spine.mapNotNull { id -> pkg.manifest[id] }.map {
                    val full = resolve(dir, it)
                    SpineItem(full, titles[full])
                }
                return EpubBook(zip, pkg.title, pkg.author, spine, dir)
            } catch (e: Throwable) { zip.close(); throw e }
        }

        /** Resolves [rel] against [dir], handling `..` and percent-encoding. */
        fun resolve(dir: String, rel: String): String {
            val parts = ArrayList<String>()
            if (dir.isNotEmpty()) parts.addAll(dir.split('/'))
            for (p in java.net.URLDecoder.decode(rel.replace("+", "%2B"), "UTF-8").split('/')) when (p) {
                "", "." -> {}
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
                else -> parts.add(p)
            }
            return parts.joinToString("/")
        }
    }
}

private val xif: XMLInputFactory = XMLInputFactory.newFactory().apply {
    setProperty(XMLInputFactory.SUPPORT_DTD, false)
    setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
    setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false)
}

private fun reader(bytes: ByteArray): XMLStreamReader = xif.createXMLStreamReader(bytes.inputStream())

private fun firstAttr(bytes: ByteArray, tag: String, attr: String): String? {
    val r = reader(bytes)
    while (r.hasNext()) if (r.next() == START_ELEMENT && r.localName == tag) return r.getAttributeValue(null, attr)
    return null
}

private class Pkg(
    val title: String?, val author: String?, val manifest: Map<String, String>,
    val spine: List<String>, val navHref: String?, val ncxHref: String?,
)

private fun parseOpf(bytes: ByteArray): Pkg {
    val r = reader(bytes)
    var title: String? = null
    var author: String? = null
    val manifest = LinkedHashMap<String, String>()
    val spine = ArrayList<String>()
    var nav: String? = null
    var ncx: String? = null
    while (r.hasNext()) {
        if (r.next() != START_ELEMENT) continue
        when (r.localName) {
            "title" -> if (title == null) title = r.elementText.trim()
            "creator" -> if (author == null) author = r.elementText.trim()
            "item" -> {
                val id = r.getAttributeValue(null, "id") ?: continue
                val href = r.getAttributeValue(null, "href") ?: continue
                manifest[id] = href
                if (r.getAttributeValue(null, "properties")?.split(' ')?.contains("nav") == true) nav = href
                if (r.getAttributeValue(null, "media-type") == "application/x-dtbncx+xml") ncx = href
            }
            "itemref" -> if (r.getAttributeValue(null, "linear") != "no") r.getAttributeValue(null, "idref")?.let(spine::add)
        }
    }
    return Pkg(title, author, manifest, spine, nav, ncx)
}

/** Returns (href, label) pairs from an EPUB3 nav document or an NCX. */
private fun parseToc(bytes: ByteArray, isNav: Boolean): List<Pair<String, String>> {
    val out = ArrayList<Pair<String, String>>()
    val r = reader(bytes.preprocessXhtml())
    if (isNav) {
        var inNav = false
        while (r.hasNext()) when (r.next()) {
            START_ELEMENT -> when (r.localName) {
                "nav" -> inNav = r.getAttributeValue(null, "type") == "toc" ||
                    (0 until r.attributeCount).any { r.getAttributeLocalName(it) == "type" && r.getAttributeValue(it) == "toc" }
                "a" -> if (inNav) {
                    val href = r.getAttributeValue(null, "href")
                    val label = r.elementText.trim().replace(Regex("\\s+"), " ")
                    if (href != null && label.isNotEmpty()) out += href to label
                }
            }
            END_ELEMENT -> if (r.localName == "nav") inNav = false
        }
    } else {
        var label: String? = null
        while (r.hasNext()) if (r.next() == START_ELEMENT) when (r.localName) {
            "text" -> label = r.elementText.trim()
            "content" -> r.getAttributeValue(null, "src")?.let { s -> label?.let { out += s to it }; label = null }
        }
    }
    return out
}

private val entities = mapOf(
    "nbsp" to " ", "mdash" to "—", "ndash" to "–", "hellip" to "…", "lsquo" to "‘", "rsquo" to "’",
    "ldquo" to "“", "rdquo" to "”", "copy" to "©", "laquo" to "«", "raquo" to "»", "times" to "×",
    "middot" to "·", "bull" to "•", "eacute" to "é", "egrave" to "è", "agrave" to "à", "ccedil" to "ç",
)

/** XHTML in the wild uses HTML named entities without a DTD we can load; map the common ones to chars. */
private fun ByteArray.preprocessXhtml(): ByteArray {
    val s = String(this, Charsets.UTF_8)
    if (s.indexOf('&') < 0) return this
    return Regex("&([A-Za-z][A-Za-z0-9]*);").replace(s) { m ->
        val n = m.groupValues[1]
        if (n in setOf("amp", "lt", "gt", "quot", "apos")) m.value else entities[n] ?: " "
    }.toByteArray()
}

private val blockTags = mapOf(
    "p" to Kind.Paragraph, "li" to Kind.ListItem, "blockquote" to Kind.Quote, "pre" to Kind.Pre,
    "h1" to Kind.Heading, "h2" to Kind.Heading, "h3" to Kind.Heading, "h4" to Kind.Heading,
    "h5" to Kind.Heading, "h6" to Kind.Heading, "dt" to Kind.Paragraph, "dd" to Kind.Paragraph,
    "figcaption" to Kind.Paragraph, "td" to Kind.Paragraph, "th" to Kind.Paragraph,
)
private val containerTags = setOf("div", "section", "article", "ul", "ol", "table", "tr", "dl", "figure", "body", "aside", "header", "footer", "nav", "hr", "br")
private val skipTags = setOf("head", "script", "style", "svg", "math")

internal fun parseXhtml(bytes: ByteArray): List<Block> {
    val r = reader(bytes.preprocessXhtml())
    val blocks = ArrayList<Block>()
    val sb = StringBuilder()
    val spans = ArrayList<Span>()
    // Open inline elements: (start offset, flags, href)
    class Open(val start: Int, val flags: Int, val href: String?)
    val open = ArrayList<Open>()
    val stack = ArrayList<Pair<String, Kind?>>()
    var kind = Kind.Paragraph
    var level = 0
    var skip = 0
    var pre = false

    fun flush() {
        // Close-and-reopen any inline spans that straddle a block boundary.
        var end = sb.length
        while (end > 0 && sb[end - 1] == ' ' && !pre) end--
        if (end > 0) {
            val text = sb.substring(0, end)
            val sp = spans.mapNotNull { if (it.start >= end) null else Span(it.start, minOf(it.end, end), it.flags, it.href) }
            blocks += Block(kind, text, sp, level)
        }
        sb.setLength(0); spans.clear()
        for (i in open.indices) open[i] = Open(0, open[i].flags, open[i].href)
    }

    fun append(t: String) {
        if (pre) { sb.append(t); return }
        for (c in t) {
            if (c.isWhitespace() && c != ' ') { if (sb.isNotEmpty() && sb[sb.length - 1] != ' ') sb.append(' ') }
            else sb.append(c)
        }
    }

    fun inlineFlags(n: String) = when (n) {
        "b", "strong" -> BOLD; "i", "em", "cite", "dfn" -> ITALIC; "code", "tt", "kbd", "samp" -> CODE
        "a" -> LINK; "sup", "sub" -> SCRIPT; else -> 0
    }

    while (r.hasNext()) {
        when (r.next()) {
            START_ELEMENT -> {
                val n = r.localName.lowercase()
                if (skip > 0 || n in skipTags) { skip++; continue }
                val bk = blockTags[n]
                when {
                    bk != null -> {
                        flush()
                        kind = if (bk == Kind.Paragraph && stack.any { it.first == "blockquote" }) Kind.Quote else bk
                        level = if (bk == Kind.Heading) n[1] - '0' else 0
                        if (n == "pre") pre = true
                        stack += n to bk
                    }
                    n == "img" -> {
                        flush()
                        val alt = r.getAttributeValue(null, "alt") ?: ""
                        blocks += Block(Kind.Image, alt, src = r.getAttributeValue(null, "src"))
                        stack += n to null
                    }
                    n in containerTags -> { flush(); kind = Kind.Paragraph; level = 0; stack += n to null }
                    else -> {
                        val f = inlineFlags(n)
                        if (f != 0) open += Open(sb.length, f, if (n == "a") r.getAttributeValue(null, "href") else null)
                        stack += n to null
                    }
                }
            }
            END_ELEMENT -> {
                if (skip > 0) { skip--; continue }
                val n = r.localName.lowercase()
                val top = stack.removeLastOrNull() ?: continue
                when {
                    top.second != null || n in containerTags -> {
                        flush(); if (n == "pre") pre = false
                        // Restore the enclosing block kind (e.g. a <p> inside <blockquote> ends, quote continues).
                        val outer = stack.lastOrNull { it.second != null }
                        kind = outer?.second ?: Kind.Paragraph
                        level = if (kind == Kind.Heading) outer!!.first[1] - '0' else 0
                    }
                    else -> if (inlineFlags(n) != 0) {
                        val o = open.removeLastOrNull()
                        if (o != null && sb.length > o.start) spans += Span(o.start, sb.length, o.flags, o.href)
                    }
                }
            }
            CHARACTERS, CDATA, SPACE -> if (skip == 0) append(r.text)
            ENTITY_REFERENCE -> if (skip == 0) append(r.text ?: "")
        }
    }
    flush()
    return blocks
}
