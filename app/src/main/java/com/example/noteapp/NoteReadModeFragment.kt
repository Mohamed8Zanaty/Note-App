package com.example.noteapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.noteapp.databinding.FragmentHomeBinding
import com.example.noteapp.databinding.FragmentNoteReadModeBinding

class NoteReadModeFragment : Fragment() {
    private var _binding: FragmentNoteReadModeBinding? = null
    private val binding get() = _binding!!
    private lateinit var noteTitle : TextView
    private lateinit var noteText : TextView
    private val args: NoteReadModeFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteReadModeBinding.inflate(inflater, container, false)
        setupViews()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindData(receivedData())
        editModeBtnSetup()
        backBtnSetup()

    }
    private fun setupViews() {
        noteTitle = binding.noteTitle
        noteText = binding.noteText
    }

    private fun editModeBtnSetup() {
        binding.btnEdit.setOnClickListener {
                // Get the actual note ID from arguments
                val noteId = args.noteId.toLongOrNull() ?: 0L
                val note = Note(
                    id = noteId,  // Include the ID here
                    title = noteTitle.text.toString(),
                    content = noteText.text.toString()
                )
                navigateToEditMode(note)
        }
    }

    private fun navigateToEditMode(note:Note) {
        val action = NoteReadModeFragmentDirections
            .actionNoteReadModeToNoteEditMode(
                noteId = note.id.toString(),  // Pass the actual ID
                noteTitle = note.title,
                noteText = note.content
            )
        findNavController().navigate(action)
    }
    private fun bindData(note : Pair<String?, String>) {
        noteTitle.text = note.first
        noteText.text = note.second
    }
    private fun receivedData() : Pair<String?, String> {
        return args.noteTitle to args.noteText
    }
    private fun backBtnSetup() {
        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_note_read_mode_to_home)
        }
    }
}