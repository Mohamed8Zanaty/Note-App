package com.example.noteapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.icu.text.CaseMap.Title
import androidx.annotation.ContentView

class NoteDbHelper(context : Context) :
SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(
            """
            CREATE TABLE $TABLE_NOTES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT,
                $COL_CONTENT TEXT NOT NULL,
                $COL_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (db == null) return


        if (oldVersion < 2) {

            db.execSQL(
                "ALTER TABLE $TABLE_NOTES ADD COLUMN $COL_CREATED_AT INTEGER NOT NULL DEFAULT 0"
            )

        }

    }

    fun addNote(title: String?, content: String): Long {
        val values = values(title, content, System.currentTimeMillis())
        val db = writableDatabase
        val result = db.insert(TABLE_NOTES, null, values)
        // Don't close the database here
        return result
    }

    fun updateNote(id: Long, title: String?, content: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, title)
            put(COL_CONTENT, content)
        }
        val result = db.update(
            TABLE_NOTES,
            values,
            "$COL_ID = ?",
            arrayOf(id.toString())
        )
        // Don't close the database here
        return result
    }

    fun deleteNote(id: Long): Int {
        val db = writableDatabase
        val result = db.delete(TABLE_NOTES, "$COL_ID = ?", arrayOf(id.toString()))
        // Don't close the database here
        return result
    }

    fun getAllNotes(): List<Note> {
        val notes = mutableListOf<Note>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NOTES,
            arrayOf(COL_ID, COL_TITLE, COL_CONTENT, COL_CREATED_AT),
            null, null, null, null,
            "$COL_CREATED_AT DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COL_ID))
                val title = it.getString(it.getColumnIndexOrThrow(COL_TITLE))
                val content = it.getString(it.getColumnIndexOrThrow(COL_CONTENT))
                val createdAt = it.getLong(it.getColumnIndexOrThrow(COL_CREATED_AT))
                notes += Note(id = id, title = title, content = content, createdAt = createdAt)
            }
        }
        // Don't close the database here
        return notes
    }

    fun getNote(id: Long): Note? {
        val db = readableDatabase
        val c = db.query(
            TABLE_NOTES,
            arrayOf(COL_ID, COL_TITLE, COL_CONTENT, COL_CREATED_AT),
            "$COL_ID = ?",
            arrayOf(id.toString()),
            null, null, null, null
        )
        c.use {
            return if (it.moveToFirst()) {
                val nid = it.getLong(it.getColumnIndexOrThrow(COL_ID))
                val title = it.getString(it.getColumnIndexOrThrow(COL_TITLE))
                val content = it.getString(it.getColumnIndexOrThrow(COL_CONTENT))
                val createdAt = it.getLong(it.getColumnIndexOrThrow(COL_CREATED_AT))
                Note(id = nid, title = title, content = content, createdAt = createdAt)
            } else {
                null
            }
        }
    }

    private fun values(title: String?, content: String, createdAt: Long?): ContentValues {
        return ContentValues().apply {
            put(COL_TITLE, title)
            put(COL_CONTENT, content)
            createdAt?.let { put(COL_CREATED_AT, it) }
        }
    }
    fun closeDb() {
        close()
    }
    companion object {
        @Volatile
        private var INSTANCE: NoteDbHelper? = null

        fun getInstance(context: Context): NoteDbHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NoteDbHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
        const val DB_NAME = "notes.db"
        // Bump DB version to 2 because we added createdAt in schema.
        const val DB_VERSION = 2

        const val TABLE_NOTES = "notes"
        const val COL_ID = "_id"
        const val COL_TITLE = "title"
        const val COL_CONTENT = "content"
        const val COL_CREATED_AT = "createdAt"
    }
}