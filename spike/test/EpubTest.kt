import io.ukata.athena.spike.epub.*
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.*

class EpubTest {
    private fun makeEpub(): String {
        val f = File.createTempFile("epubtest", ".epub"); f.deleteOnExit()
        ZipOutputStream(f.outputStream()).use { z ->
            fun put(n: String, s: String) { z.putNextEntry(ZipEntry(n)); z.write(s.toByteArray()); z.closeEntry() }
            put("mimetype", "application/epub+zip")
            put("META-INF/container.xml", """<container xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/content.opf"/></rootfiles></container>""")
            put("OEBPS/content.opf", """<package xmlns="http://www.idpf.org/2007/opf"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Test Book</dc:title><dc:creator>A. Writer</dc:creator></metadata>
              <manifest><item id="nav" href="nav.xhtml" properties="nav" media-type="application/xhtml+xml"/><item id="c1" href="text/ch%201.xhtml" media-type="application/xhtml+xml"/><item id="c2" href="text/ch2.xhtml" media-type="application/xhtml+xml"/></manifest>
              <spine><itemref idref="c1"/><itemref idref="c2"/></spine></package>""")
            put("OEBPS/nav.xhtml", """<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><body><nav epub:type="toc"><ol><li><a href="text/ch%201.xhtml">One</a></li><li><a href="text/ch2.xhtml#x">Two</a></li></ol></nav></body></html>""")
            put("OEBPS/text/ch 1.xhtml", """<html xmlns="http://www.w3.org/1999/xhtml"><head><title>x</title><style>p{}</style></head><body>
              <h1>Chapter  One</h1>
              <p>Hello&nbsp;<b>bold <i>and italic</i></b> world &mdash; <a href="ch2.xhtml">link</a>.
                 Second   line.</p>
              <blockquote><p>Quoted.</p></blockquote>
              <ul><li>one</li><li>two <code>x</code></li></ul>
              <div>Loose text<img src="../img/a.png" alt="pic"/></div>
              <pre>a  b
c</pre></body></html>""")
            put("OEBPS/text/ch2.xhtml", """<html xmlns="http://www.w3.org/1999/xhtml"><body><p>Only.</p></body></html>""")
        }
        return f.path
    }

    @Test fun metadataSpineAndToc() {
        EpubBook.open(makeEpub()).use { b ->
            assertEquals("Test Book", b.title); assertEquals("A. Writer", b.author)
            assertEquals(listOf("OEBPS/text/ch 1.xhtml", "OEBPS/text/ch2.xhtml"), b.spine.map { it.href })
            assertEquals(listOf("One", "Two"), b.spine.map { it.title })
        }
    }

    @Test fun blocksSpansAndWhitespace() {
        EpubBook.open(makeEpub()).use { b ->
            val bl = b.chapter(0).blocks
            assertEquals(listOf(Kind.Heading, Kind.Paragraph, Kind.Quote, Kind.ListItem, Kind.ListItem, Kind.Paragraph, Kind.Image, Kind.Pre), bl.map { it.kind })
            assertEquals("Chapter One", bl[0].text); assertEquals(1, bl[0].level)
            val p = bl[1]
            assertEquals("Hello bold and italic world — link. Second line.", p.text)
            val bold = p.spans.first { it.flags and BOLD != 0 }
            assertEquals("bold and italic", p.text.substring(bold.start, bold.end))
            assertEquals("and italic", p.spans.first { it.flags and ITALIC != 0 }.let { p.text.substring(it.start, it.end) })
            assertEquals("link", p.spans.first { it.flags and LINK != 0 }.let { p.text.substring(it.start, it.end) })
            assertEquals("Quoted.", bl[2].text)
            assertEquals("two x", bl[4].text)
            assertEquals("Loose text", bl[5].text)
            assertEquals("../img/a.png", bl[6].src); assertEquals("pic", bl[6].text)
            assertEquals("a  b\nc", bl[7].text)
        }
    }

    @Test fun resolvePaths() {
        assertEquals("OEBPS/img/a.png", EpubBook.resolve("OEBPS/text", "../img/a.png"))
        assertEquals("a b.html", EpubBook.resolve("", "a%20b.html"))
    }
}
