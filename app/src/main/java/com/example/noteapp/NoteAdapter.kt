package com.example.noteapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class NoteAdapter(
    private val onItemClick: (Note) -> Unit,
    private val onDelete: (Note, Int) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(DIFF) {

    private val selectedIds = mutableSetOf<Long>()

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean =
                oldItem.title == newItem.title && oldItem.content == newItem.content
        }
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View = itemView.findViewById(R.id.root)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvBody: TextView = itemView.findViewById(R.id.tvBody)
        val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.note, parent, false)
        return NoteViewHolder(v)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)

        holder.tvTitle.text = note.title
        holder.tvBody.text = note.content

        val selected = selectedIds.contains(note.id)
        holder.itemView.isActivated = selected

        // show/hide views based on selection
        holder.tvTitle.visibility = if (selected) View.GONE else View.VISIBLE
        holder.tvBody.visibility = if (selected) View.GONE else View.VISIBLE
        holder.ivDelete.visibility = if (selected) View.VISIBLE else View.GONE

        // Long-click toggles selection
        holder.itemView.setOnLongClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) toggleSelectionById(note.id)
            true
        }

        // Clicks either toggle selection when in selection mode, or open item
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            if (selectedIds.isNotEmpty()) {
                toggleSelectionById(note.id)
            } else {
                onItemClick(note)
            }
        }

        holder.ivDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onDelete(note, pos)
            }
        }
    }

    private fun toggleSelectionById(noteId: Long) {
        if (selectedIds.contains(noteId)) {
            selectedIds.remove(noteId)
        } else {
            selectedIds.add(noteId)
        }
        // find changed index(es) and notify
        // simpler: just refresh whole list for selection changes:
        notifyItemRangeChanged(0, itemCount) // selection visual changes only
    }

    fun clearSelection() {
        if (selectedIds.isEmpty()) return
        selectedIds.clear()
        notifyItemRangeChanged(0, itemCount)
    }

    // remove by position: submit a new list and keep selection safe (ids remain meaningful)
    fun removeAt(position: Int) {
        if (position !in 0 until currentList.size) return
        val newList = currentList.toMutableList()
        val removed = newList.removeAt(position)
        selectedIds.remove(removed.id)
        submitList(newList)
    }

    fun updateList(newItems: List<Note>) {
        // clear selection when a full new list arrives (optional)
        selectedIds.clear()
        submitList(newItems.toList())
    }

    fun updateNoteById(note: Note): Int {
        val newList = currentList.toMutableList()
        val idx = newList.indexOfFirst { it.id == note.id }
        val pos = if (idx >= 0) {
            newList[idx] = note
            idx
        } else {
            newList.add(0, note)
            0
        }
        submitList(newList)
        return pos
    }
}
