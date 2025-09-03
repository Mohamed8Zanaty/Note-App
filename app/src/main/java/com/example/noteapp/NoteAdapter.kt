package com.example.noteapp

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.graphics.drawable.StateListDrawable

class NoteAdapter(
    private val context: Context,
    private val onItemClick: (Note) -> Unit,
    private val onDelete: (Note, Int) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(DIFF) {

    private val selectedIds = mutableSetOf<Long>()

    companion object {
        private const val PAYLOAD_SELECTION = "payload:selection"
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


    override fun onBindViewHolder(holder: NoteViewHolder, position: Int, payloads: List<Any?>) {
        if (payloads.isNotEmpty() && payloads.contains(PAYLOAD_SELECTION)) {
            val note = getItem(position)
            val selected = selectedIds.contains(note.id)
            holder.root.isActivated = selected
            holder.tvTitle.visibility = if (selected) View.GONE else View.VISIBLE
            holder.tvBody.visibility = if (selected) View.GONE else View.VISIBLE
            holder.ivDelete.visibility = if (selected) View.VISIBLE else View.GONE
            return
        }
        onBindViewHolder(holder, position)

    }
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        val selected = selectedIds.contains(note.id)
        val rootView = holder.root
        rootView.isActivated = selected
        holder.itemView.isActivated = selected

        holder.tvTitle.visibility = if (selected) View.GONE else View.VISIBLE
        holder.tvBody.visibility = if (selected) View.GONE else View.VISIBLE
        holder.ivDelete.visibility = if (selected) View.VISIBLE else View.GONE


        val ta = context.resources.obtainTypedArray(R.array.note_colors)
        val colorCount = ta.length()
        val colorId = ta.getResourceId(position % colorCount, R.color.note_0)
        ta.recycle()
        val colorInt = ContextCompat.getColor(context, colorId)

        try {
            val defaultDrawable = AppCompatResources.getDrawable(context, R.drawable.rounded_text_view)
            val activatedDrawable = AppCompatResources.getDrawable(context, R.drawable.note_delete_background)

            if (defaultDrawable is GradientDrawable) {
                val defaultCopy = (defaultDrawable.mutate() as GradientDrawable)
                defaultCopy.setColor(colorInt)

                val states = StateListDrawable()
                if (activatedDrawable != null)
                    states.addState(intArrayOf(android.R.attr.state_activated), activatedDrawable)
                states.addState(intArrayOf(), defaultCopy)

                rootView.background = states
            } else
                rootView.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)

        } catch (e: Exception) {
            rootView.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)
        }

        holder.tvTitle.text = note.title
        holder.tvBody.text = note.content



        holder.itemView.setOnLongClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) toggleSelectionById(note.id)
            true
        }
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            if (selectedIds.isNotEmpty()) toggleSelectionById(note.id) else onItemClick(note)
        }
        holder.ivDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onDelete(note, pos)
        }
    }


    private fun toggleSelectionById(noteId: Long) {
        val previouslySelected = selectedIds.toSet()

        if (selectedIds.contains(noteId)) selectedIds.remove(noteId)
        else selectedIds.add(noteId)

        val changedPositions = mutableSetOf<Int>()
        val toggledIdx = currentList.indexOfFirst { it.id == noteId }
        if (toggledIdx >= 0) changedPositions.add(toggledIdx)

        previouslySelected.forEach { id ->
            if (!selectedIds.contains(id)) {
                val idx = currentList.indexOfFirst { it.id == id }
                if (idx >= 0) changedPositions.add(idx)
            }
        }
        changedPositions.forEach { pos -> notifyItemChanged(pos, PAYLOAD_SELECTION) }
    }

    fun removeAt(position: Int) {
        if (position !in 0 until currentList.size) return
        val newList = currentList.toMutableList()
        val removed = newList.removeAt(position)
        selectedIds.remove(removed.id)
        submitList(newList)
    }

    fun updateList(newItems: List<Note>) {
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
