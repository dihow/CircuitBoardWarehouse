package com.example.android.circuitboardwarehouse.activity

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.android.circuitboardwarehouse.R
import com.example.android.circuitboardwarehouse.viewmodel.PcbComponentsViewModel
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AddComponentDialogFragment(
    private val pcbId: Long,
    private val onComponentAdded: () -> Unit
) : DialogFragment() {
    private lateinit var viewModel: PcbComponentsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(requireActivity())[PcbComponentsViewModel::class.java]

        val view = inflater.inflate(R.layout.dialog_add_component, container, false)
        val spinner = view.findViewById<Spinner>(R.id.component_spinner)
        val quantityEditText = view.findViewById<TextInputEditText>(R.id.quantity_edit_text)
        val coordinatesEditText = view.findViewById<TextInputEditText>(R.id.coordinates_edit_text)
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item)

        viewModel.allComponents.observe(viewLifecycleOwner) { components ->
            adapter.clear()
            adapter.addAll(components.map { "${it.name} (${it.type})" })
            adapter.notifyDataSetChanged()
        }

        spinner.adapter = adapter

        view.findViewById<Button>(R.id.save_button).setOnClickListener {
            val selectedPos = spinner.selectedItemPosition
            val quantity = quantityEditText.text.toString().toIntOrNull() ?: 0
            val coordinates = coordinatesEditText.text.toString()

            if (selectedPos != -1 && quantity > 0 && coordinates.isNotBlank()) {
                val component = viewModel.allComponents.value?.get(selectedPos)
                    ?: return@setOnClickListener

                lifecycleScope.launch {
                    val success = viewModel.updateComponentOnPcb(
                        context = requireContext(),
                        pcbId = pcbId,
                        componentId = component.id,
                        newCount = quantity,
                        newCoordinates = coordinates
                    )

                    if (success) {
                        onComponentAdded()
                        dismiss()
                    }
                }
            } else {
                Toast.makeText(context, "Заполните все поля корректно", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle("Добавить компонент")
        return dialog
    }
}