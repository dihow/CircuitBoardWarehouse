package com.example.android.circuitboardwarehouse.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.android.circuitboardwarehouse.DateUtils
import com.example.android.circuitboardwarehouse.R
import com.example.android.circuitboardwarehouse.database.Order
import com.example.android.circuitboardwarehouse.viewmodel.OrderEditViewModel
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OrderEditActivity : AppCompatActivity() {
    private lateinit var viewModel: OrderEditViewModel
    private var orderId: Long? = null
    private var isEditMode = false
    private var currentClient: String? = null
    private var currentStatus: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_order_edit)

        viewModel = ViewModelProvider(this)[OrderEditViewModel::class.java]
        orderId = intent.getLongExtra(EXTRA_ORDER_ID, -1).takeIf { it != -1L }
        isEditMode = orderId != null

        setupSpinner()

        setupSaveButton()
        setupCartButton()

        setupDateTimePicker()

        if (isEditMode) {
            loadOrderData()
        }
    }

    private fun setupSpinner() {
        val clientSpinner = findViewById<Spinner>(R.id.client_spinner)
        val statusSpinner = findViewById<Spinner>(R.id.status_spinner)

        val clientAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item)
        clientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        clientSpinner.adapter = clientAdapter

        val statusAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.status_spinner_items,
            android.R.layout.simple_spinner_item
        )
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        statusSpinner.adapter = statusAdapter

        lifecycleScope.launch {
            val clients = viewModel.getAllClientNames()
            clientAdapter.clear()
            clientAdapter.addAll(clients)
            clientAdapter.notifyDataSetChanged()
        }

        clientSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentClient = parent?.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentStatus = parent?.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSaveButton() {
        findViewById<Button>(R.id.save_button).setOnClickListener {
            saveOrder()
        }
    }

    private fun setupCartButton() {
        findViewById<Button>(R.id.cart_button).setOnClickListener {
            orderId?.let { id ->
                val intent = CartActivity.newIntent(this, id)
                startActivity(intent)
            } ?: run {
                Toast.makeText(this, "Сначала создайте заказ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupDateTimePicker() {
        val dateEditText = findViewById<TextInputEditText>(R.id.shipping_date_edit_text)

        dateEditText.setOnClickListener {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = DateUtils.utcToLocal(System.currentTimeMillis())
            }

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)

                    val timePickerDialog = TimePickerDialog(
                        this,
                        { _, hourOfDay, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)

                            dateEditText.setText(
                                SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                    .format(calendar.time)
                            )
                        },
                        hour,
                        minute,
                        true
                    )
                    timePickerDialog.show()
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }
    }

    private fun saveOrder() {
        val shippingCompany = findViewById<TextInputEditText>(R.id.shipping_company_edit_text)
            .text?.toString()?.trim().takeIf { !it.isNullOrEmpty() }

        val shippingDateText = findViewById<TextInputEditText>(R.id.shipping_date_edit_text)
            .text?.toString()?.trim()
        val shippingDate = shippingDateText?.let { DateUtils.parseFromDisplay(it) }

        lifecycleScope.launch {
            val clientId = viewModel.getClientId(currentClient!!)
            if (clientId == null) {
                Toast.makeText(
                    this@OrderEditActivity,
                    "Ошибка: не удалось определить клиента",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val totalAmount = if (isEditMode) {
                viewModel.getOrderById(orderId!!)?.totalAmount ?: 0.0
            } else {
                0.0
            }

            val order = Order(
                id = orderId ?: 0,
                clientId = clientId,
                registrationDate = if (isEditMode) {
                    viewModel.getOrderLiveData(orderId!!).value?.registrationDate
                        ?: System.currentTimeMillis()
                } else {
                    System.currentTimeMillis()
                },
                status = currentStatus.toString(),
                totalAmount = totalAmount,
                shippingDate = shippingDate,
                shippingCompany = shippingCompany
            )

            try {
                if (isEditMode) {
                    viewModel.updateOrder(order)
                } else {
                    viewModel.addOrder(order)
                }
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@OrderEditActivity,
                    "Ошибка при сохранении: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("OrderEditActivity", "Error saving order", e)
            }

            if (currentStatus.toString() == "Готов" || currentStatus.toString() == "Отправлен") {
                viewModel.runInTransaction {
                    val orderItems = viewModel.getOrderItemsByOrderId(order.id)
                    orderItems.forEach { item ->
                        val pcb = viewModel.getPcbById(item.pcbId)!!
                        viewModel.updatePcb(
                            pcb.copy(
                                totalStock = pcb.totalStock - item.quantity,
                                orderedQuantity = pcb.orderedQuantity - item.quantity
                            )
                        )
                    }
                }
            }
        }
    }

    private fun loadOrderData() {
        val order = viewModel.getOrderLiveData(orderId!!)
        order.observe(this) {
            if (it?.status == "Готов" || it?.status == "Отправлен") {
                findViewById<Button>(R.id.save_button).isEnabled = false
                findViewById<Spinner>(R.id.client_spinner).isEnabled = false
                findViewById<Spinner>(R.id.status_spinner).isEnabled = false
                findViewById<TextInputEditText>(R.id.shipping_company_edit_text).isEnabled = false
                findViewById<TextInputEditText>(R.id.shipping_date_edit_text).isEnabled = false
            }
            lifecycleScope.launch {
                val name = viewModel.getClientNameById(it!!.clientId)
                setSpinnerItem(findViewById<Spinner>(R.id.client_spinner), name)
                setSpinnerItem(findViewById<Spinner>(R.id.status_spinner), it.status)
                findViewById<TextInputEditText>(R.id.shipping_company_edit_text)
                    .setText(it.shippingCompany)
                findViewById<TextInputEditText>(R.id.shipping_date_edit_text).setText(
                    it.shippingDate?.let { DateUtils.formatForDisplay(it) } ?: ""
                )
            }
        }
    }

    private fun setSpinnerItem(spinner: Spinner, item: String?) {
        if (item == null) {
            return
        }
        val adapter = spinner.adapter as ArrayAdapter<String>
        val position = adapter.getPosition(item)
        spinner.setSelection(position)
    }

    companion object {
        const val EXTRA_ORDER_ID = "order_id"

        fun newIntent(packageContext: Context, orderId: Long? = null) =
            Intent(packageContext, OrderEditActivity::class.java).apply {
                putExtra(EXTRA_ORDER_ID, orderId)
            }
    }
}