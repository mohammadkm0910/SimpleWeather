package com.mohammadkk.simpleweather.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.mohammadkk.simpleweather.R

class WarningDialog(private val callback:()->Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialog = AlertDialog.Builder(requireContext())
        val view = requireActivity().layoutInflater.inflate(R.layout.warning_dialog, requireActivity().findViewById(R.id.rootWarningDialog))
        val btnCancelDialog: Button = view.findViewById(R.id.btnCancelDialog)
        val btnConfirmationDialog: Button = view.findViewById(R.id.btnConfirmationDialog)
        alertDialog.setView(view)
        val dialog = alertDialog.create()
        btnCancelDialog.setOnClickListener {
            dialog.dismiss()
        }
        btnConfirmationDialog.setOnClickListener {
            callback()
            dialog.dismiss()
        }
        return dialog
    }
}