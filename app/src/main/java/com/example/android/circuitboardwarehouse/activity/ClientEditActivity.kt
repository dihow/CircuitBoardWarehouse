package com.example.android.circuitboardwarehouse.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.android.circuitboardwarehouse.R
import com.example.android.circuitboardwarehouse.database.Client
import com.example.android.circuitboardwarehouse.database.LegalEntity
import com.example.android.circuitboardwarehouse.database.PhysicalPerson
import com.example.android.circuitboardwarehouse.viewmodel.ClientEditViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClientEditActivity : AppCompatActivity() {
    private lateinit var viewModel: ClientEditViewModel
    private var clientId: Long? = null
    private var isEditMode = false
    private var currentClientType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_edit)

        viewModel = ViewModelProvider(this)[ClientEditViewModel::class.java]
        clientId = intent.getLongExtra(EXTRA_CLIENT_ID, -1).takeIf { it != -1L }
        isEditMode = clientId != null

        setupSpinner()
        setupSaveButton()

        if (isEditMode) {
            loadClientData()
        }
    }

    private fun setupSpinner() {
        val spinner = findViewById<Spinner>(R.id.client_type_spinner)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val type = parent?.getItemAtPosition(position).toString()
                currentClientType = type
                updateUiForType()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateUiForType() {
        val isPhysical = currentClientType == getString(R.string.physical_person)

        findViewById<TextInputLayout>(R.id.full_name_input_layout).visibility = if (isPhysical) View.VISIBLE else View.GONE
        findViewById<TextInputLayout>(R.id.address_input_layout).visibility = if (isPhysical) View.VISIBLE else View.GONE
        findViewById<TextInputLayout>(R.id.age_input_layout).visibility = if (isPhysical) View.VISIBLE else View.GONE

        findViewById<TextInputLayout>(R.id.name_input_layout).visibility = if (!isPhysical) View.VISIBLE else View.GONE
        findViewById<TextInputLayout>(R.id.inn_input_layout).visibility = if (!isPhysical) View.VISIBLE else View.GONE
        findViewById<TextInputLayout>(R.id.contact_person_input_layout).visibility = if (!isPhysical) View.VISIBLE else View.GONE
        findViewById<TextInputLayout>(R.id.legal_address_input_layout).visibility = if (!isPhysical) View.VISIBLE else View.GONE
        findViewById<TextInputLayout>(R.id.actual_address_input_layout).visibility = if (!isPhysical) View.VISIBLE else View.GONE
    }

    private fun setupSaveButton() {
        findViewById<Button>(R.id.save_button).setOnClickListener {
            saveClient()
        }
    }

    private fun saveClient() {
        val type = findViewById<Spinner>(R.id.client_type_spinner).selectedItem.toString()
        val phone = findViewById<TextInputEditText>(R.id.phone_edit_text).text.toString()
        val email = findViewById<TextInputEditText>(R.id.email_edit_text).text.toString()

        if (type.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Заполните обязательные поля", Toast.LENGTH_SHORT).show()
            return
        }

        val isPhysical = currentClientType == getString(R.string.physical_person)
        val newIsPhysical = type == getString(R.string.physical_person)

        if (isEditMode) {
            if (newIsPhysical) {
                val fullName = findViewById<TextInputEditText>(R.id.full_name_edit_text).text.toString()
                val address = findViewById<TextInputEditText>(R.id.address_edit_text).text.toString()
                val age = findViewById<TextInputEditText>(R.id.age_edit_text).text.toString().toIntOrNull() ?: 0

                viewModel.updateClientType(
                    clientId!!,
                    type,
                    phone,
                    email,
                    isPhysical,
                    fullName,
                    address,
                    age
                )
            } else {
                val name = findViewById<TextInputEditText>(R.id.name_edit_text).text.toString()
                val inn = findViewById<TextInputEditText>(R.id.inn_edit_text).text.toString()
                val contactPerson = findViewById<TextInputEditText>(R.id.contact_person_edit_text).text.toString()
                val legalAddress = findViewById<TextInputEditText>(R.id.legal_address_edit_text).text.toString()
                val actualAddress = findViewById<TextInputEditText>(R.id.actual_address_edit_text).text.toString()

                viewModel.updateClientType(
                    clientId!!,
                    type,
                    phone,
                    email,
                    isPhysical,
                    name = name,
                    inn = inn,
                    contactPerson = contactPerson,
                    legalAddress = legalAddress,
                    actualAddress = actualAddress
                )
            }
            finish()
        } else {
            if (isPhysical) {
                val fullName = findViewById<TextInputEditText>(R.id.full_name_edit_text).text.toString()
                val address = findViewById<TextInputEditText>(R.id.address_edit_text).text.toString()
                val age = findViewById<TextInputEditText>(R.id.age_edit_text).text.toString().toIntOrNull() ?: 0

                if (fullName.isEmpty() || address.isEmpty() || age == 0) {
                    Toast.makeText(this, "Заполните все поля для физического лица", Toast.LENGTH_SHORT).show()
                    return
                }

                CoroutineScope(Dispatchers.Main).launch {
                    if (isEditMode) {
                        viewModel.updatePhysicalClient(
                            clientId!!,
                            Client(id = clientId!!, type = type, phone = phone, email = email),
                            PhysicalPerson(clientId!!, fullName, address, age)
                        )
                    } else {
                        viewModel.addPhysicalClient(fullName, address, age, phone, email)
                    }
                    finish()
                }
            } else {
                val name = findViewById<TextInputEditText>(R.id.name_edit_text).text.toString()
                val inn = findViewById<TextInputEditText>(R.id.inn_edit_text).text.toString()
                val contactPerson = findViewById<TextInputEditText>(R.id.contact_person_edit_text).text.toString()
                val legalAddress = findViewById<TextInputEditText>(R.id.legal_address_edit_text).text.toString()
                val actualAddress = findViewById<TextInputEditText>(R.id.actual_address_edit_text).text.toString()

                if (name.isEmpty() || inn.isEmpty() || contactPerson.isEmpty() ||
                    legalAddress.isEmpty() || actualAddress.isEmpty()) {
                    Toast.makeText(this, "Заполните все поля для юридического лица", Toast.LENGTH_SHORT).show()
                    return
                }

                CoroutineScope(Dispatchers.Main).launch {
                    if (isEditMode) {
                        viewModel.updateLegalClient(
                            clientId!!,
                            Client(id = clientId!!, type = type, phone = phone, email = email),
                            LegalEntity(clientId!!, name, inn, contactPerson, legalAddress, actualAddress)
                        )
                    } else {
                        viewModel.addLegalClient(name, inn, contactPerson, legalAddress, actualAddress, phone, email)
                    }
                    finish()
                }
            }
        }
    }

    private fun loadClientData() {
        viewModel.getClient(clientId!!).observe(this@ClientEditActivity) { client ->
            client?.let {
                val spinner = findViewById<Spinner>(R.id.client_type_spinner)
                val adapter = spinner.adapter as ArrayAdapter<String>
                val position = adapter.getPosition(it.type)
                spinner.setSelection(position)

                findViewById<TextInputEditText>(R.id.phone_edit_text).setText(it.phone)
                findViewById<TextInputEditText>(R.id.email_edit_text).setText(it.email)

                if (it.type == getString(R.string.physical_person)) {
                    viewModel.getPhysicalPerson(clientId!!).observe(this@ClientEditActivity) { physicalPerson ->
                        physicalPerson?.let {
                            findViewById<TextInputEditText>(R.id.full_name_edit_text).setText(it.fullName)
                            findViewById<TextInputEditText>(R.id.address_edit_text).setText(it.address)
                            findViewById<TextInputEditText>(R.id.age_edit_text).setText(it.age.toString())
                        }
                    }
                } else {
                    viewModel.getLegalEntity(clientId!!).observe(this@ClientEditActivity) { legalEntity ->
                        legalEntity?.let {
                            findViewById<TextInputEditText>(R.id.name_edit_text).setText(it.name)
                            findViewById<TextInputEditText>(R.id.inn_edit_text).setText(it.inn)
                            findViewById<TextInputEditText>(R.id.contact_person_edit_text).setText(it.contactPerson)
                            findViewById<TextInputEditText>(R.id.legal_address_edit_text).setText(it.legalAddress)
                            findViewById<TextInputEditText>(R.id.actual_address_edit_text).setText(it.actualAddress)
                        }
                    }
                }
                currentClientType = it.type
                updateUiForType()
            }
        }
    }

    companion object {
        const val EXTRA_CLIENT_ID = "client_id"

        fun newIntent(packageContext: Context, clientId: Long? = null) =
            Intent(packageContext, ClientEditActivity::class.java).apply {
                clientId?.let { putExtra(EXTRA_CLIENT_ID, it) }
            }
    }
}