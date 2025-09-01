package com.example.noteapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.noteapp.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: NoteDbHelper
    private lateinit var adapter: NoteAdapter
    private lateinit var recyclerView: RecyclerView

    private val notes = mutableListOf<Note>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        addButtonSetup()

        infoButtonSetup()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setup()
        observeSaveNote()

    }
    private fun setup() {
        db = NoteDbHelper.getInstance(requireContext())
        recyclerView = binding.notesRecycler

        adapter = NoteAdapter(
            onItemClick = { note ->
                val action = HomeFragmentDirections.actionHomeToReadNote(
                    noteId = note.id.toString(),
                    noteTitle = note.title,
                    noteText = note.content
                )
                findNavController().navigate(action)
            },
            onDelete = { note, pos ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val deleted = withContext(Dispatchers.IO) { db.deleteNote(note.id) }
                    withContext(Dispatchers.Main) {
                        if (deleted > 0) adapter.removeAt(pos)
                        else Toast.makeText(requireContext(), "Failed to delete", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@HomeFragment.adapter
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) { db.getAllNotes() }
            adapter.updateList(loaded)
        }


    }

    private fun navigationToEditNote() {
        val action = HomeFragmentDirections.actionHomeToNote(
            "", ""
        )
        findNavController().navigate(action)
    }

    private fun addButtonSetup() {
        binding.btnAdd.setOnClickListener {
            navigationToEditNote()
        }
    }


    private fun infoButtonSetup() {
        binding.btnInfo.setOnClickListener{
            val dialog = InfoFragment()
            dialog.show(parentFragmentManager, "infoFragment")
        }
    }

    private fun observeSaveNote() {
        val navController = findNavController()
        val savedHandle = navController.currentBackStackEntry?.savedStateHandle ?: return

        savedHandle.getLiveData<Long>("note_added_id").observe(viewLifecycleOwner) { newId ->
            if (newId == null) return@observe
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val newNote = withContext(Dispatchers.IO) { db.getNote(newId) }
                    if (newNote != null) {
                        notes.add(0, newNote)
                        adapter.notifyItemInserted(0)
                        recyclerView.scrollToPosition(0)
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to load added note: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    savedHandle.remove<Long>("note_added_id")
                }
            }
        }

        savedHandle.getLiveData<Long>("note_updated_id").observe(viewLifecycleOwner) { updatedId ->
            if (updatedId == null) return@observe
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val updatedNote = withContext(Dispatchers.IO) { db.getNote(updatedId) }
                    if (updatedNote != null) {
                        adapter.updateNoteById(updatedNote)
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to load updated note: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    savedHandle.remove<Long>("note_updated_id")
                }
            }
        }

        savedHandle.getLiveData<Boolean>("refresh_notes").observe(viewLifecycleOwner) { refresh ->
            if (refresh == true) {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val loaded = withContext(Dispatchers.IO) { db.getAllNotes() }
                        notes.clear()
                        notes.addAll(loaded)
                        adapter.notifyDataSetChanged()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Refresh failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        savedHandle.remove<Boolean>("refresh_notes")
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            db.close()
        } catch (ignored: Exception) { }
    }

}
