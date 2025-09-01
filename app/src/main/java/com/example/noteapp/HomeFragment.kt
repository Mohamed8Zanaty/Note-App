package com.example.noteapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
        db = NoteDbHelper(requireContext())
        recyclerView = binding.notesRecycler

        // create adapter first (avoid notify on null adapter)
        adapter = NoteAdapter(notes, findNavController()) { note, pos ->
            lifecycleScope.launch(Dispatchers.IO) {
                val deleted = db.deleteNote(note.id)
                withContext(Dispatchers.Main) {
                    if (deleted > 0) {
                        adapter.removeAt(pos)
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@HomeFragment.adapter
        }

        // load notes asynchronously and update adapter
        lifecycleScope.launch(Dispatchers.IO) {
            val loaded = db.getAllNotes()
            withContext(Dispatchers.Main) {
                notes.clear()
                notes.addAll(loaded)
                adapter.notifyDataSetChanged()
            }
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

    /**
     * Observe savedStateHandle keys coming from NoteEditModeFragment:
     * - "note_added_id" (Long) -> fetch and insert at top
     * - "note_updated_id" (Long) -> fetch and replace existing note in list
     */
    private fun observeSaveNote() {
        val navController = findNavController()
        val savedHandle = navController.currentBackStackEntry?.savedStateHandle ?: return

        savedHandle.getLiveData<Long>("note_added_id").observe(viewLifecycleOwner) { newId ->
            if (newId == null) return@observe
            lifecycleScope.launch(Dispatchers.IO) {
                val newNote = db.getNote(newId)
                if (newNote != null) {
                    withContext(Dispatchers.Main) {
                        notes.add(0, newNote)
                        adapter.notifyItemInserted(0)
                        recyclerView.scrollToPosition(0)
                    }
                }
                // remove the value so future observers don't re-handle it
                savedHandle.remove<Long>("note_added_id")
            }
        }

        savedHandle.getLiveData<Long>("note_updated_id").observe(viewLifecycleOwner) { updatedId ->
            if (updatedId == null) return@observe
            lifecycleScope.launch(Dispatchers.IO) {
                val updatedNote = db.getNote(updatedId)
                if (updatedNote != null) {
                    withContext(Dispatchers.Main) {
                        // replace existing note if present, or insert at top
                        adapter.updateNoteById(updatedNote)
                    }
                }
                savedHandle.remove<Long>("note_updated_id")
            }
        }

        // Optional: generic refresh flag (if some other screen sends it)
        savedHandle.getLiveData<Boolean>("refresh_notes").observe(viewLifecycleOwner) { refresh ->
            if (refresh == true) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val loaded = db.getAllNotes()
                    withContext(Dispatchers.Main) {
                        notes.clear()
                        notes.addAll(loaded)
                        adapter.notifyDataSetChanged()
                    }
                }
                savedHandle.remove<Boolean>("refresh_notes")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
