
package com.example.android.circuitboardwarehouse.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.android.circuitboardwarehouse.database.CapacitorDetails
import com.example.android.circuitboardwarehouse.database.ComponentDetails
import com.example.android.circuitboardwarehouse.database.DiodeDetails
import com.example.android.circuitboardwarehouse.R
import com.example.android.circuitboardwarehouse.database.ResistorDetails
import com.example.android.circuitboardwarehouse.database.ComponentSpecification
import com.example.android.circuitboardwarehouse.viewmodel.ComponentEditViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ComponentEditActivity : AppCompatActivity() {
    private var componentSpecificationsObserver: Observer<List<ComponentSpecification>>? = null

    private lateinit var viewModel: ComponentEditViewModel
    private var componentId: Long? = null
    private var isEditMode = false

    private lateinit var dynamicFieldsLayout: LinearLayout
    private lateinit var typeSpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var nameEditText: TextInputEditText
    private lateinit var manufacturerEditText: TextInputEditText
    private lateinit var priceEditText: TextInputEditText
    private lateinit var totalStockEditText: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_component_edit)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            val originalPaddingLeft = v.paddingLeft
            val originalPaddingTop = v.paddingTop
            val originalPaddingRight = v.paddingRight
            val originalPaddingBottom = v.paddingBottom

            v.setPadding(
                originalPaddingLeft + systemBars.left,
                originalPaddingTop + systemBars.top,
                originalPaddingRight + systemBars.right,
                originalPaddingBottom + systemBars.bottom
            )
            insets
        }

        dynamicFieldsLayout = findViewById(R.id.dynamic_fields_layout)
        typeSpinner = findViewById(R.id.component_type_spinner)
        saveButton = findViewById(R.id.save_button)
        nameEditText = findViewById(R.id.name_edit_text)
        manufacturerEditText = findViewById(R.id.manufacturer_edit_text)
        priceEditText = findViewById(R.id.component_price_edit_text)
        totalStockEditText = findViewById(R.id.component_total_stock_edit_text)

        viewModel = ViewModelProvider(this)[ComponentEditViewModel::class.java]
        componentId = intent.getLongExtra(EXTRA_COMPONENT_ID, -1).takeIf { it != -1L }
        isEditMode = componentId != null

        setupSpinner()
        observeComponentDetails()

        if (isEditMode) {
            viewModel.getComponent(componentId!!).observe(this) { component ->
                component?.let {
                    nameEditText.setText(it.name)
                    manufacturerEditText.setText(it.manufacturer)
                    priceEditText.setText(it.price.toString())
                    totalStockEditText.setText(it.stockQuantity.toString())

                    val adapter = typeSpinner.adapter as ArrayAdapter<String>
                    val position = adapter.getPosition(it.type)
                    typeSpinner.setSelection(position)
                }
            }

            componentSpecificationsObserver = Observer<List<ComponentSpecification>> { specifications ->
                specifications.let { specs ->
                    val details = when (typeSpinner.selectedItem.toString()) {
                        "Резистор" -> ResistorDetails().apply {
                            specs.find { it.specification == "Сопротивление" }?.let { resistance = it.specificationValue }
                            specs.find { it.specification == "Допуск" }?.let { tolerance = it.specificationValue }
                            specs.find { it.specification == "Мощность" }?.let { powerRating = it.specificationValue }
                        }

                        "Конденсатор" -> CapacitorDetails().apply {
                            specs.find { it.specification == "Ёмкость" }?.let { capacitance = it.specificationValue }
                            specs.find { it.specification == "Напряжение" }?.let { voltageRating = it.specificationValue }
                            specs.find { it.specification == "Максимальная температура" }?.let { maxTemperature = it.specificationValue }
                        }

                        "Диод" -> DiodeDetails().apply {
                            specs.find { it.specification == "Падение напряжения" }?.let { forwardVoltage = it.specificationValue }
                            specs.find { it.specification == "Обратное напряжение" }?.let { reverseVoltage = it.specificationValue }
                            specs.find { it.specification == "Прямой ток" }?.let { forwardCurrent = it.specificationValue }
                        }

                        else -> null
                    }
                    details?.let { viewModel.componentDetails.value = it }
                }
            }
            if (isEditMode) {
                viewModel.getComponentSpecifications(componentId!!).observe(this, componentSpecificationsObserver!!)
            }
        }

        saveButton.setOnClickListener {
            if (isEditMode) {
                viewModel.updateComponent(
                    componentId = componentId!!,
                    serialNumber = nameEditText.text.toString(),
                    manufacturer = manufacturerEditText.text.toString(),
                    price = priceEditText.text.toString(),
                    totalStock = totalStockEditText.text.toString()
                )
            } else {
                viewModel.saveComponent(
                    nameEditText.text.toString(),
                    manufacturerEditText.text.toString(),
                    priceEditText.text.toString(),
                    totalStockEditText.text.toString()
                )
            }
            finish()
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, viewModel.componentTypes)
        typeSpinner.adapter = adapter
        typeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                componentSpecificationsObserver?.let { observer ->
                    componentId?.let { componentIdNotNull ->
                        viewModel.getComponentSpecifications(componentIdNotNull).removeObserver(observer)
                    }
                }
                viewModel.onComponentTypeSelected(viewModel.componentTypes[position])

                componentSpecificationsObserver = Observer<List<ComponentSpecification>> { specifications ->
                    specifications.let { specs ->
                        val details = when (typeSpinner.selectedItem.toString()) {
                            "Резистор" -> ResistorDetails().apply {
                                specs.find { it.specification == "Сопротивление" }?.let { resistance = it.specificationValue }
                                specs.find { it.specification == "Допуск" }?.let { tolerance = it.specificationValue }
                                specs.find { it.specification == "Мощность" }?.let { powerRating = it.specificationValue }
                            }

                            "Конденсатор" -> CapacitorDetails().apply {
                                specs.find { it.specification == "Ёмкость" }?.let { capacitance = it.specificationValue }
                                specs.find { it.specification == "Напряжение" }?.let { voltageRating = it.specificationValue }
                                specs.find { it.specification == "Максимальная температура" }?.let { maxTemperature = it.specificationValue }
                            }

                            "Диод" -> DiodeDetails().apply {
                                specs.find { it.specification == "Падение напряжения" }?.let { forwardVoltage = it.specificationValue }
                                specs.find { it.specification == "Обратное напряжение" }?.let { reverseVoltage = it.specificationValue }
                                specs.find { it.specification == "Прямой ток" }?.let { forwardCurrent = it.specificationValue }
                            }

                            else -> null
                        }
                        details?.let { viewModel.componentDetails.value = it }
                    }
                }
                if (isEditMode) {
                    viewModel.getComponentSpecifications(componentId!!).observe(this@ComponentEditActivity, componentSpecificationsObserver!!)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        componentSpecificationsObserver?.let { observer ->
            componentId?.let { id ->
                viewModel.getComponentSpecifications(id).removeObserver(observer)
            }
        }
    }

    private fun observeComponentDetails() {
        viewModel.componentDetails.observe(this) { details ->
            dynamicFieldsLayout.removeAllViews()
            addDynamicFields(details)
        }
    }

    private fun addDynamicFields(details: ComponentDetails) {
        when (details) {
            is ResistorDetails -> {
                addTextField(dynamicFieldsLayout, "Сопротивление, ОМ", details.resistance) { newValue ->
                    details.resistance = newValue
                }
                addTextField(dynamicFieldsLayout, "Допуск, %", details.tolerance) { newValue ->
                    details.tolerance = newValue
                }
                addTextField(dynamicFieldsLayout, "Мощность, Вт", details.powerRating) { newValue ->
                    details.powerRating = newValue
                }
            }
            is CapacitorDetails -> {
                addTextField(dynamicFieldsLayout, "Ёмкость, Ф", details.capacitance) { newValue ->
                    details.capacitance = newValue
                }
                addTextField(dynamicFieldsLayout, "Напряжение, В", details.voltageRating) { newValue ->
                    details.voltageRating = newValue
                }
                addTextField(dynamicFieldsLayout, "Максимальная температура, °C", details.maxTemperature) { newValue ->
                    details.maxTemperature = newValue
                }
            }
            is DiodeDetails -> {
                addTextField(dynamicFieldsLayout, "Падение напряжения, В", details.forwardVoltage) { newValue ->
                    details.forwardVoltage = newValue
                }
                addTextField(dynamicFieldsLayout, "Обратное напряжение, В", details.reverseVoltage) { newValue ->
                    details.reverseVoltage = newValue
                }
                addTextField(dynamicFieldsLayout, "Прямой ток, А", details.forwardCurrent) { newValue ->
                    details.forwardCurrent = newValue
                }
            }
        }
    }

    private fun addTextField(layout: LinearLayout, hint: String, initialValue: String, onTextChanged: (String) -> Unit) {
        val textInputLayout = TextInputLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            this.hint = hint
        }

        val editText = TextInputEditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setText(initialValue)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    onTextChanged(s.toString())
                }
            })
        }

        textInputLayout.addView(editText)
        layout.addView(textInputLayout)
    }

    companion object {
        const val EXTRA_COMPONENT_ID = "component_id"

        fun newIntent(packageContext: Context, pcbId: Long? = null) =
            Intent(packageContext, ComponentEditActivity::class.java).apply {
                pcbId?.let { putExtra(EXTRA_COMPONENT_ID, it) }
            }
    }
}