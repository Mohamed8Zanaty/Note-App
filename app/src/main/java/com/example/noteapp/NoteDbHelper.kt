package com.example.noteapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class NoteDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT,
                $COL_CONTENT TEXT NOT NULL,
                $COL_CREATED_AT INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // simple incremental migrations; do NOT drop user data
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COL_CREATED_AT INTEGER NOT NULL DEFAULT 0")
        }
        // future migrations: if (oldVersion < 3) { ... }
    }

    suspend fun addNote(title: String?, content: String): Long = withContext(Dispatchers.IO) {
        val createdAt = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(COL_TITLE, title)
            put(COL_CONTENT, content)
            put(COL_CREATED_AT, createdAt)
        }
        writableDatabase.insert(TABLE_NOTES, null, values)
    }

    suspend fun updateNote(id: Long, title: String?, content: String): Int = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COL_TITLE, title)
            put(COL_CONTENT, content)
        }
        writableDatabase.update(TABLE_NOTES, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    suspend fun deleteNote(id: Long): Int = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_NOTES, "$COL_ID = ?", arrayOf(id.toString()))
    }

    suspend fun getAllNotes(): List<Note> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Note>()
        val cursor = readableDatabase.query(
            TABLE_NOTES,
            arrayOf(COL_ID, COL_TITLE, COL_CONTENT, COL_CREATED_AT),
            null, null, null, null,
            "$COL_CREATED_AT DESC"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                do {
                    val nid = c.getLong(c.getColumnIndexOrThrow(COL_ID))
                    val title = c.getString(c.getColumnIndexOrThrow(COL_TITLE)) ?: ""
                    val content = c.getString(c.getColumnIndexOrThrow(COL_CONTENT)) ?: ""
                    val createdAt = c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT))
                    list.add(Note(nid, title, content, createdAt))
                } while (c.moveToNext())
            }
        }
        list
    }

    suspend fun getNote(id: Long): Note? = withContext(Dispatchers.IO) {
        val cursor = readableDatabase.query(
            TABLE_NOTES,
            arrayOf(COL_ID, COL_TITLE, COL_CONTENT, COL_CREATED_AT),
            "$COL_ID = ?",
            arrayOf(id.toString()),
            null, null, null
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                val nid = c.getLong(c.getColumnIndexOrThrow(COL_ID))
                val title = c.getString(c.getColumnIndexOrThrow(COL_TITLE)) ?: ""
                val content = c.getString(c.getColumnIndexOrThrow(COL_CONTENT)) ?: ""
                val createdAt = c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT))
                return@withContext Note(nid, title, content, createdAt)
            }
            null
        }
    }

    fun closeHelper() {
        try {
            close()
        } catch (e: Exception) { /* ignore */ }
    }

    companion object {
        @Volatile
        private var INSTANCE: NoteDbHelper? = null

        fun getInstance(context: Context): NoteDbHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NoteDbHelper(context.applicationContext).also { INSTANCE = it }
            }
        }

        private const val DB_NAME = "notes.db"
        private const val DB_VERSION = 2

        const val TABLE_NOTES = "notes"
        const val COL_ID = "_id"
        const val COL_TITLE = "title"
        const val COL_CONTENT = "content"
        const val COL_CREATED_AT = "createdAt"
    }
}
