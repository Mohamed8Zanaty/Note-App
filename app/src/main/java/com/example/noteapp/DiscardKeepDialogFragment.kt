package com.example.noteapp

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.noteapp.databinding.FragmentDiscardKeepDialogBinding
import com.example.noteapp.databinding.FragmentHomeBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DiscardKeepDialogFragment: DialogFragment() {
    private var _binding : FragmentDiscardKeepDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var keep : MaterialButton
    private lateinit var discard : MaterialButton
    private lateinit var db: NoteDbHelper
    private lateinit var note : Note

    companion object {
        fun newInstance(note: Note): DiscardKeepDialogFragment {
            val args = Bundle().apply {
                putLong("note_id", note.id)
                putString("note_title", note.title)
                putString("note_content", note.content)
            }
            return DiscardKeepDialogFragment().apply {
                arguments = args
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            note = Note(
                it.getLong("note_id"),
                it.getString("note_title") ?: "",
                it.getString("note_content") ?: ""
            )
        }
        db = NoteDbHelper.getInstance(requireContext())
        isCancelable = true
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscardKeepDialogBinding.inflate(inflater, container, false)
        bindViews()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        keepButtonSetup()
        discardButtonSetup()
    }
    override fun onStart() {
        super.onStart()

        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog?.window?.setDimAmount(0f)
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setGravity(Gravity.CENTER)
        dialog?.setCanceledOnTouchOutside(true)

        val overlayView = view?.findViewById<View>(R.id.overlay_view_discard)
        overlayView?.setOnClickListener {
            dismiss() // Tap outside dismisses
        }

        val dialogLayout = view?.findViewById<LinearLayout>(R.id.dialog_layout_discard)
        dialogLayout?.setOnClickListener {
            // Consume the click so it doesn't dismiss
        }
    }

    private fun bindViews() {
        keep = binding.keep
        discard = binding.discard
    }
    private fun discardButtonSetup() {
        discard.setOnClickListener {
            val navController = parentFragment?.findNavController() ?: findNavController()
            navController.navigate(R.id.action_note_edit_mode_to_home)
            dismiss()
        }
    }
    private fun keepButtonSetup() {
        keep.setOnClickListener {

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {

                    if (note.id != -1L) {
                        // Update existing note
                        db.updateNote(note.id, note.title, note.content)
                    } else {
                        // Add new note
                        db.addNote(note.title, note.content)
                    }
                }
                val navController = parentFragment?.findNavController() ?: findNavController()
                navController.navigate(R.id.action_note_edit_mode_to_home)
                dismiss()
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