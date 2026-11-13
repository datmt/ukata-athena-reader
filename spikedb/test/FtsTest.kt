import io.ukata.athena.spikedb.*
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.*

class FtsTest {
    @Test fun fts5SearchStaysInSync() = runBlocking {
        val f = File.createTempFile("spikedb", ".db"); f.delete()
        val db = openDb(f.path)
        try {
            val d = db.notes()
            val a = d.insert(Note(title = "Graph layout", body = "Layered graphs use Sugiyama crossing minimisation"))
            val b = d.insert(Note(title = "Café", body = "Résumé of the novel, a quiet story"))
            assertEquals(listOf(a), d.search("sugiyama").map { it.id })
            assertEquals(listOf(a), d.search("layo*").map { it.id }, "prefix")
            assertEquals(listOf(b), d.search("cafe").map { it.id }, "diacritics folded")
            assertEquals(listOf(a), d.search("title:graph").map { it.id }, "column filter")
            d.setBody(a, "now about selection")
            assertTrue(d.search("sugiyama").isEmpty(), "update removed old terms")
            assertEquals(listOf(a), d.search("selection").map { it.id })
            d.delete(b)
            assertTrue(d.search("cafe").isEmpty(), "delete synced")
            // Throughput check
            repeat(5000) { d.insert(Note(title = "t$it", body = "lorem ipsum dolor sit amet token$it")) }
            val t0 = System.nanoTime()
            val hits = d.search("token4321")
            println("search over 5k notes: ${(System.nanoTime() - t0) / 1000} us, hits=${hits.size}")
            assertEquals(1, hits.size)
        } finally { db.close(); File(f.path + "-wal").delete(); File(f.path + "-shm").delete(); f.delete() }
    }
}
