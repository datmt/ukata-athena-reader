package io.ukata.athena.spikedb

import androidx.room.*
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

@Entity(tableName = "note")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
)

@Dao
interface NoteDao {
    @Insert suspend fun insert(n: Note): Long
    @Query("SELECT * FROM note WHERE id = :id") suspend fun get(id: Long): Note?
    @Query("UPDATE note SET body = :body WHERE id = :id") suspend fun setBody(id: Long, body: String)
    @Query("DELETE FROM note WHERE id = :id") suspend fun delete(id: Long)
    // Room cannot verify FTS5 tables at compile time, so the search goes through @RawQuery.
    @RawQuery suspend fun searchRaw(q: RoomRawQuery): List<Note>
}

suspend fun NoteDao.search(q: String, limit: Int = 50): List<Note> = searchRaw(
    RoomRawQuery(
        "SELECT note.* FROM note_fts JOIN note ON note.id = note_fts.rowid " +
            "WHERE note_fts MATCH ? ORDER BY bm25(note_fts, 5.0, 1.0) LIMIT ?",
    ) { it.bindText(1, q); it.bindLong(2, limit.toLong()) },
)

@Database(entities = [Note::class], version = 1, exportSchema = true)
abstract class AppDb : RoomDatabase() {
    abstract fun notes(): NoteDao
}

/** FTS5 external-content index kept in sync by triggers; created with raw SQL (Room only knows FTS3/4). */
object Fts5Callback : RoomDatabase.Callback() {
    override fun onCreate(connection: SQLiteConnection) {
        connection.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS note_fts USING fts5(title, body, content='note', content_rowid='id', tokenize='unicode61 remove_diacritics 2')")
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS note_ai AFTER INSERT ON note BEGIN INSERT INTO note_fts(rowid, title, body) VALUES (new.id, new.title, new.body); END")
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS note_ad AFTER DELETE ON note BEGIN INSERT INTO note_fts(note_fts, rowid, title, body) VALUES('delete', old.id, old.title, old.body); END")
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS note_au AFTER UPDATE ON note BEGIN INSERT INTO note_fts(note_fts, rowid, title, body) VALUES('delete', old.id, old.title, old.body); INSERT INTO note_fts(rowid, title, body) VALUES (new.id, new.title, new.body); END")
    }

    override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
    }
}

fun openDb(path: String): AppDb =
    Room.databaseBuilder<AppDb>(path)
        .setDriver(BundledSQLiteDriver())
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .addCallback(Fts5Callback)
        .build()
