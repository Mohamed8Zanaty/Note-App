package com.example.noteapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
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
            val noteId = args.noteId.toLongOrNull() ?: -1L
            val note = Note(
                id = noteId,
                title = noteTitle.text.toString(),
                content = noteText.text.toString()
            )
            navigateToEditMode(note)
        }
    }

    private fun navigateToEditMode(note: Note) {
        val action = NoteReadModeFragmentDirections
            .actionNoteReadModeToNoteEditMode(
                noteId = note.id.toString(),
                noteTitle = note.title,
                noteText = note.content
            )
        findNavController().navigate(action)
    }

    private fun bindData(note: Note) {
        noteTitle.text = note.title
        noteText.text = note.content
    }

    private fun receivedData(): Note {

        val noteId = args.noteId.toLongOrNull() ?: -1L
        return Note(
            id = noteId,
            title = args.noteTitle,
            content = args.noteText
        )
    }

    private fun backBtnSetup() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
