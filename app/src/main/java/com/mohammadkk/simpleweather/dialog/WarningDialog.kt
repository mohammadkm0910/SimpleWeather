package com.mohammadkk.simpleweather.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.mohammadkk.simpleweather.databinding.WarningDialogBinding

class WarningDialog(private val callback:()->Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialog = AlertDialog.Builder(requireContext())
        val binding = WarningDialogBinding.inflate(requireActivity().layoutInflater)
        alertDialog.setView(binding.root)
        val dialog = alertDialog.create()
        binding.btnCancelDialog.setOnClickListener {
            dialog.dismiss()
        }
        binding.btnConfirmationDialog.setOnClickListener {
            callback()
            dialog.dismiss()
        }
        return dialog
    }
}