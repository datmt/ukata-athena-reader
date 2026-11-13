package io.ukata.athena.spike.pdf

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_CHAR
import java.lang.foreign.ValueLayout.JAVA_DOUBLE
import java.lang.foreign.ValueLayout.JAVA_FLOAT
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG
import java.lang.invoke.MethodHandle

/** Minimal PDFium binding over the Java FFM API (no JNI, no extra libraries). */
class Pdfium(libPath: String) : AutoCloseable {
    private val arena = Arena.ofShared()
    private val linker = Linker.nativeLinker()
    private val lookup = SymbolLookup.libraryLookup(libPath, arena)

    private fun fn(name: String, ret: java.lang.foreign.MemoryLayout?, vararg args: java.lang.foreign.MemoryLayout): MethodHandle {
        val d = if (ret == null) FunctionDescriptor.ofVoid(*args) else FunctionDescriptor.of(ret, *args)
        return linker.downcallHandle(lookup.find(name).orElseThrow { IllegalStateException("missing $name") }, d)
    }

    private val init = fn("FPDF_InitLibrary", null)
    private val destroy = fn("FPDF_DestroyLibrary", null)
    private val loadDoc = fn("FPDF_LoadDocument", ADDRESS, ADDRESS, ADDRESS)
    private val closeDoc = fn("FPDF_CloseDocument", null, ADDRESS)
    private val pageCount = fn("FPDF_GetPageCount", JAVA_INT, ADDRESS)
    private val loadPage = fn("FPDF_LoadPage", ADDRESS, ADDRESS, JAVA_INT)
    private val closePage = fn("FPDF_ClosePage", null, ADDRESS)
    private val pageW = fn("FPDF_GetPageWidthF", JAVA_FLOAT, ADDRESS)
    private val pageH = fn("FPDF_GetPageHeightF", JAVA_FLOAT, ADDRESS)
    private val bmpCreate = fn("FPDFBitmap_Create", ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT)
    private val bmpFill = fn("FPDFBitmap_FillRect", null, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_LONG)
    private val bmpBuf = fn("FPDFBitmap_GetBuffer", ADDRESS, ADDRESS)
    private val bmpDestroy = fn("FPDFBitmap_Destroy", null, ADDRESS)
    private val render = fn("FPDF_RenderPageBitmap", null, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT)
    private val textLoad = fn("FPDFText_LoadPage", ADDRESS, ADDRESS)
    private val textClose = fn("FPDFText_ClosePage", null, ADDRESS)
    private val textCount = fn("FPDFText_CountChars", JAVA_INT, ADDRESS)
    private val textGet = fn("FPDFText_GetText", JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS)
    private val charBox = fn("FPDFText_GetCharBox", JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS)

    init { init.invokeExact() }

    inner class Document internal constructor(private val handle: MemorySegment) : AutoCloseable {
        val pageCount: Int get() = this@Pdfium.pageCount.invokeExact(handle) as Int

        fun <T> page(index: Int, block: (Page) -> T): T {
            val p = loadPage.invokeExact(handle, index) as MemorySegment
            check(p != MemorySegment.NULL) { "cannot load page $index" }
            try { return block(Page(p)) } finally { closePage.invokeExact(p) }
        }

        override fun close() { closeDoc.invokeExact(handle) }
    }

    class Bgra(val width: Int, val height: Int, val pixels: ByteArray)

    inner class Page internal constructor(private val handle: MemorySegment) {
        val width: Float get() = pageW.invokeExact(handle) as Float
        val height: Float get() = pageH.invokeExact(handle) as Float

        /** Renders to a BGRA buffer. White background. */
        fun render(w: Int, h: Int): Bgra {
            val bmp = bmpCreate.invokeExact(w, h, 1) as MemorySegment // 1 = BGRA with alpha
            try {
                bmpFill.invokeExact(bmp, 0, 0, w, h, 0xFFFFFFFFL)
                render.invokeExact(bmp, handle, 0, 0, w, h, 0, 0x10) // 0x10 = FPDF_LCD_TEXT
                val buf = (bmpBuf.invokeExact(bmp) as MemorySegment).reinterpret(w.toLong() * h * 4)
                return Bgra(w, h, buf.toArray(JAVA_BYTE))
            } finally { bmpDestroy.invokeExact(bmp) }
        }

        fun text(): String = withText { tp ->
            val n = textCount.invokeExact(tp) as Int
            if (n <= 0) return@withText ""
            Arena.ofConfined().use { a ->
                val out = a.allocate((n + 1L) * 2)
                val got = textGet.invokeExact(tp, 0, n, out) as Int
                val sb = StringBuilder(got)
                for (i in 0 until got - 1) sb.append(out.getAtIndex(JAVA_CHAR, i.toLong()))
                sb.toString()
            }
        }

        /** Char box in PDF user space: left, right, bottom, top. */
        fun charBox(i: Int): DoubleArray = withText { tp ->
            Arena.ofConfined().use { a ->
                val v = a.allocate(32)
                charBox.invokeExact(tp, i, v, v.asSlice(8), v.asSlice(16), v.asSlice(24)) as Int
                DoubleArray(4) { v.getAtIndex(JAVA_DOUBLE, it.toLong()) }
            }
        }

        private fun <T> withText(block: (MemorySegment) -> T): T {
            val tp = textLoad.invokeExact(handle) as MemorySegment
            try { return block(tp) } finally { textClose.invokeExact(tp) }
        }
    }

    fun open(path: String): Document = Arena.ofConfined().use { a ->
        val h = loadDoc.invokeExact(a.allocateFrom(path), MemorySegment.NULL) as MemorySegment
        check(h != MemorySegment.NULL) { "cannot open $path" }
        Document(h)
    }

    override fun close() { destroy.invokeExact(); arena.close() }
}
