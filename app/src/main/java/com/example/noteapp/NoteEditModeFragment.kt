package com.example.noteapp

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.noteapp.databinding.FragmentNoteEditModeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class NoteEditModeFragment : Fragment() {

    private var _binding: FragmentNoteEditModeBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: NoteDbHelper
    private lateinit var scrollView: NestedScrollView
    private lateinit var noteText: CustomEditText
    private lateinit var title: EditText

    private var keyboardHeight = 0
    private var edited = false

    private val args: NoteEditModeFragmentArgs by navArgs()
    private val noteId: Long by lazy { args.noteId.toLongOrNull() ?: -1L }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteEditModeBinding.inflate(inflater, container, false)
        bindViews()
        backButtonSetup()
        readBtnSetup()

        bindData(receivedData())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = NoteDbHelper.getInstance(requireContext())

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            keyboardHeight = imeInsets.bottom

            noteText.setTextColor(Color.WHITE)
            noteText.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.hint))

            scrollView.setPadding(
                scrollView.paddingLeft,
                scrollView.paddingTop,
                scrollView.paddingRight,
                if (keyboardHeight > 0) keyboardHeight + dpToPx(8, requireContext()) else 0
            )
            insets
        }

        noteText.setOnFocusChangeListener { _, _ -> noteText.setTextColor(Color.WHITE) }
        ViewCompat.requestApplyInsets(view)

        saveButtonSetup()
    }

    private fun bindViews() {
        scrollView = binding.scrollView
        noteText = binding.noteText
        title = binding.noteTitle
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

    private fun dpToPx(dp: Int, ctx: Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), ctx.resources.displayMetrics
        ).roundToInt()
    }

    private fun bindData(note: Note) {
        title.setText(note.title)
        noteText.setText(note.content)
    }

    private fun receivedData(): Note {
        return Note(noteId, args.noteTitle, args.noteText)
    }

    private fun navigateToReadMode(note: Note) {
        val action = NoteEditModeFragmentDirections
            .actionNoteEditModeToNoteReadMode(
                note.title,
                note.content,
            )
        findNavController().navigate(action)
    }

    private fun readBtnSetup() {
        setEdited()
        binding.btnRead.setOnClickListener {
            navigateToReadMode(
                Note(
                    title = title.text.toString(),
                    content = noteText.text.toString()
                )
            )
        }
    }

    private fun setEdited() {
        title.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { edited = true }
        })
        noteText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { edited = true }
        })
    }

    private fun saveButtonSetup() {
        binding.btnSave.setDebouncedOnClickListener {
            val sTitle = title.text?.toString().orEmpty()
            val sContent = noteText.text?.toString().orEmpty()
            if (sContent.isBlank() && sTitle.isBlank()) {
                Toast.makeText(requireContext(), "Enter something", Toast.LENGTH_SHORT).show()
                return@setDebouncedOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val (status, returnedId) = if (noteId != -1L) {

                        val existing = db.getNote(noteId)
                        if (existing == null) {
                            "not_found" to -1L
                        } else {
                            val updatedRows = db.updateNote(noteId, sTitle, sContent)
                            if (updatedRows > 0) "updated" to noteId else "update_failed" to -1L
                        }
                    } else {
                        // add flow
                        val newId = db.addNote(sTitle, sContent)
                        if (newId != -1L) "added" to newId else "add_failed" to -1L
                    }

                    when (status) {
                        "added" -> {
                            Toast.makeText(requireContext(), "Note saved", Toast.LENGTH_SHORT).show()
                            findNavController().previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("note_added_id", returnedId)
                            findNavController().popBackStack()
                        }
                        "updated" -> {
                            Toast.makeText(requireContext(), "Note updated", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_note_edit_mode_to_home)
                        }
                        "not_found" -> {
                            Toast.makeText(requireContext(), "Cannot update: note not found", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun backButtonSetup() {
        binding.btnBack.setOnClickListener {
            val currentTitle = title.text.toString()
            val currentContent = noteText.text.toString()
            val idForDialog = if (noteId != -1L) noteId else -1L
            val dialog = DiscardKeepDialogFragment.newInstance(
                Note(
                    id = idForDialog,
                    title = currentTitle,
                    content = currentContent
                )
            )
            dialog.show(parentFragmentManager, "discardFragment")
        }
    }

    private fun View.setDebouncedOnClickListener(interval: Long = 600L, action: (View) -> Unit) {
        var lastClick = 0L
        setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastClick > interval) {
                lastClick = now
                action(it)
            }
        }
    }
}
