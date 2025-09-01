package com.example.noteapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView

class NoteAdapter(
    private val notes : MutableList<Note>,
    private val navController: NavController,
    private val onDelete: (Note, Int) -> Unit
) :RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {
    private val selectedPositions = mutableSetOf<Int>()
    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val root: View = itemView.findViewById(R.id.root)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvBody: TextView = itemView.findViewById(R.id.tvBody)
        val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.note, parent, false)
        return NoteViewHolder(v)
    }

    override fun getItemCount(): Int = notes.size

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.tvTitle.text = note.title
        holder.tvBody.text = note.content
        val selected = selectedPositions.contains(position)
        holder.itemView.isActivated = selected
        if(selected) {
            holder.tvTitle.visibility = View.GONE
            holder.tvBody.visibility = View.GONE
            holder.ivDelete.visibility = View.VISIBLE
        }
        else {
            holder.tvTitle.visibility = View.VISIBLE
            holder.tvBody.visibility = View.VISIBLE
            holder.ivDelete.visibility = View.GONE
        }

        holder.itemView.setOnLongClickListener {
            toggleSelection(position)
            true
        }
        holder.itemView.setOnClickListener {
            if (selectedPositions.isNotEmpty()) {
                toggleSelection(position)
            } else {
                val action = HomeFragmentDirections.actionHomeToReadNote(
                    noteId = note.id.toString(),
                    noteTitle = note.title,
                    noteText = note.content
                )
                navController.navigate(action)
            }
        }
        holder.ivDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onDelete(note, pos)
            }
        }

    }
    private fun toggleSelection(position: Int) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position)
            notifyItemChanged(position)
        } else {
            selectedPositions.add(position)
            notifyItemChanged(position)
        }
    }
    private fun clearSelectionAndShift(removedIndex: Int) {
        val copy = selectedPositions.toList()
        selectedPositions.clear()
        for (pos in copy) {
            if (pos == removedIndex) continue
            val newPos = if (pos > removedIndex) pos - 1 else pos
            selectedPositions.add(newPos)
        }
    }
    fun removeAt(position: Int) {
        if (position in 0 until notes.size) {
            notes.removeAt(position)
            clearSelectionAndShift(position)
            notifyItemRemoved(position)
        }
    }

    fun updateList(newItems: List<Note>) {
        notes.clear()
        notes.addAll(newItems)
        selectedPositions.clear()
        notifyDataSetChanged()
    }

    /**
     * Replace existing note with same id, or insert at top if not found.
     * Returns the position changed/inserted.
     */
    fun updateNoteById(note: Note): Int {
        val idx = notes.indexOfFirst { it.id == note.id }
        return if (idx >= 0) {
            notes[idx] = note
            notifyItemChanged(idx)
            idx
        } else {
            notes.add(0, note)
            notifyItemInserted(0)
            0
        }
    }

}
