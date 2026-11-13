import io.ukata.athena.spike.pdf.Pdfium
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class PdfiumTest {
    private val lib = "/usr/lib/libreoffice/program/libpdfiumlo.so"
    private val pdf = "/usr/share/doc/shared-mime-info/shared-mime-info-spec.pdf"

    @Test
    fun opensRendersAndExtractsText() {
        if (!File(lib).exists() || !File(pdf).exists()) return
        Pdfium(lib).use { p ->
            p.open(pdf).use { doc ->
                assertTrue(doc.pageCount > 0)
                doc.page(0) { page ->
                    assertTrue(page.width > 100f)
                    val t0 = System.nanoTime()
                    val img = page.render((page.width * 1.5f).toInt(), (page.height * 1.5f).toInt())
                    val ms = (System.nanoTime() - t0) / 1_000_000
                    println("render ${img.width}x${img.height} in ${ms}ms")
                    assertTrue(img.pixels.any { it != 0xFF.toByte() }, "page is not blank")
                    val text = page.text()
                    println("text: " + text.take(80).replace('\n', ' '))
                    assertTrue(text.isNotBlank())
                    val box = page.charBox(0)
                    assertTrue(box[1] > box[0] && box[3] > box[2])
                }
            }
        }
    }
}
