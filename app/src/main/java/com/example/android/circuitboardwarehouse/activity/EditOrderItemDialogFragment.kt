import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.android.circuitboardwarehouse.R
import com.example.android.circuitboardwarehouse.WarehouseRepository
import com.example.android.circuitboardwarehouse.database.OrderItem
import com.example.android.circuitboardwarehouse.database.Pcb
import com.example.android.circuitboardwarehouse.viewmodel.CartViewModel
import kotlinx.coroutines.launch

class EditOrderItemDialogFragment : DialogFragment() {
    private lateinit var viewModel: CartViewModel

    private var orderId: Long = -1
    private var orderItemId: Long = -1
    private var currentPcbId: Long = -1
    private var currentQuantity: Int = 1
    private var pcbs: List<Pcb> = emptyList()
    private var pcbNames: List<String> = emptyList()

    private lateinit var pcbSpinner: Spinner
    private lateinit var quantityEditText: EditText
    private lateinit var priceTextView: TextView
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            orderId = it.getLong(ARG_ORDER_ID)
            orderItemId = it.getLong(ARG_ORDER_ITEM_ID, -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(requireActivity())[CartViewModel::class.java]

        val view = inflater.inflate(R.layout.dialog_edit_order_item, container, false)

        pcbSpinner = view.findViewById(R.id.pcb_spinner)
        quantityEditText = view.findViewById(R.id.quantity_edit_text)
        priceTextView = view.findViewById(R.id.price_text_view)
        saveButton = view.findViewById(R.id.save_button)
        deleteButton = view.findViewById(R.id.delete_button)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUi()
        loadData()
    }

    private fun setupUi() {
        quantityEditText.setText("1")

        pcbSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentPcbId = pcbs[position].id
                updatePriceDisplay()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        saveButton.setOnClickListener { saveOrderItem() }
        deleteButton.setOnClickListener { deleteOrderItem() }

        deleteButton.visibility = if (orderItemId != -1L) View.VISIBLE else View.GONE
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                pcbs = viewModel.getPcbsSuspend()

                pcbNames = pcbs.map { it.name }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    pcbNames
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                pcbSpinner.adapter = adapter

                if (orderItemId != -1L) {
                    loadExistingOrderItem()
                }
            } catch (e: Exception) {
                Log.e("EditOrderItemDialog", "Error loading data: ${e.message}", e)
                Toast.makeText(context, "Ошибка при загрузке данных: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun loadExistingOrderItem() {
        try {
            val orderItem = viewModel.getOrderItemById(orderItemId) ?: return

            currentPcbId = orderItem.pcbId
            currentQuantity = orderItem.quantity

            val position = pcbs.indexOfFirst { it.id == orderItem.pcbId }
            if (position >= 0) {
                pcbSpinner.setSelection(position)
            }

            quantityEditText.setText(orderItem.quantity.toString())

            updatePriceDisplay()
        } catch (e: Exception) {
            Log.e("EditOrderItemDialog", "Error loading existing order item: ${e.message}", e)
            Toast.makeText(context, "Ошибка при загрузке товара: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updatePriceDisplay() {
        val selectedPcb = pcbs.find { it.id == currentPcbId }
        selectedPcb?.let {
            priceTextView.text = "${it.price} руб./1 шт."
        }
    }

    private fun saveOrderItem() {
        val quantityText = quantityEditText.text.toString()
        if (quantityText.isEmpty()) {
            Toast.makeText(context, "Введите количество", Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = quantityText.toIntOrNull() ?: 1
        if (quantity <= 0) {
            Toast.makeText(context, "Количество должно быть больше 0", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedPcb = pcbs.find { it.id == currentPcbId } ?: return

        val available = selectedPcb.totalStock - selectedPcb.orderedQuantity
        if (quantity > available) {
            Toast.makeText(
                context,
                "Доступно только $available шт. на складе",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        lifecycleScope.launch {
            try {
                WarehouseRepository.get().runInTransaction {
                    if (orderItemId != -1L) {
                        val existing = viewModel.getOrderItemById(orderItemId)!!
                        val pcb = viewModel.getPcbById(existing.pcbId)!!

                        val quantityDifference = quantity - existing.quantity

                        viewModel.updatePcb(
                            pcb.copy(orderedQuantity = pcb.orderedQuantity + quantityDifference)
                        )

                        if (existing.pcbId != currentPcbId) {
                            val newPcb = WarehouseRepository.get().getPcbById(currentPcbId)!!
                            WarehouseRepository.get().updatePcb(
                                newPcb.copy(orderedQuantity = newPcb.orderedQuantity + quantity)
                            )
                        }

                        val updatedOrderItem = existing.copy(
                            pcbId = currentPcbId,
                            quantity = quantity,
                            pricePerPcb = selectedPcb.price
                        )
                        viewModel.updateOrderItem(updatedOrderItem)
                        true
                    } else {
                        val existingItem = viewModel.getOrderItemByOrderAndPcb(orderId, currentPcbId)

                        if (existingItem != null) {
                            val pcb = viewModel.getPcbById(existingItem.pcbId)!!
                            val newQuantity = existingItem.quantity + quantity

                            val available = pcb.totalStock - (pcb.orderedQuantity - existingItem.quantity)
                            if (quantity > available) {
                                requireActivity().runOnUiThread {
                                    Toast.makeText(
                                        context,
                                        "Можно добавить только $available шт. (уже есть ${existingItem.quantity} в заказе)",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                return@runInTransaction
                            }

                            viewModel.updatePcb(
                                pcb.copy(orderedQuantity = pcb.orderedQuantity + quantity)
                            )

                            val updatedOrderItem = existingItem.copy(
                                quantity = newQuantity,
                                pricePerPcb = selectedPcb.price
                            )
                            viewModel.updateOrderItem(updatedOrderItem)
                            true
                        } else {
                            viewModel.updatePcb(
                                selectedPcb.copy(orderedQuantity = selectedPcb.orderedQuantity + quantity)
                            )

                            viewModel.addOrderItem(
                                OrderItem(
                                    orderId = orderId,
                                    pcbId = currentPcbId,
                                    quantity = quantity,
                                    pricePerPcb = selectedPcb.price
                                )
                            )
                            true
                        }
                    }

                    val totalAmount = viewModel.calculateOrderTotal(orderId)
                    viewModel.updateOrder(
                        viewModel.getOrderById(orderId)!!.copy(totalAmount = totalAmount)
                    )
                }
                dismiss()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Ошибка при сохранении: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun deleteOrderItem() {
        if (orderItemId == -1L) return

        AlertDialog.Builder(requireContext())
            .setTitle("Удаление товара")
            .setMessage("Вы уверены, что хотите удалить этот товар из заказа?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    try {
                        WarehouseRepository.get().runInTransaction {
                            val orderItem = viewModel.getOrderItemById(orderItemId) ?: return@runInTransaction
                            val pcb = viewModel.getPcbById(orderItem.pcbId) ?: return@runInTransaction

                            viewModel.updatePcb(
                                pcb.copy(orderedQuantity = pcb.orderedQuantity - orderItem.quantity)
                            )

                            viewModel.deleteOrderItem(orderItem)

                            val totalAmount = viewModel.calculateOrderTotal(orderId)
                            viewModel.updateOrder(
                                viewModel.getOrderById(orderId)!!.copy(totalAmount = totalAmount)
                            )
                        }
                        dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Ошибка при удалении: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    companion object {
        private const val ARG_ORDER_ID = "order_id"
        private const val ARG_ORDER_ITEM_ID = "order_item_id"

        fun newInstance(orderId: Long, orderItemId: Long? = null): EditOrderItemDialogFragment {
            val args = Bundle().apply {
                putLong(ARG_ORDER_ID, orderId)
                orderItemId?.let { putLong(ARG_ORDER_ITEM_ID, it) }
            }
            return EditOrderItemDialogFragment().apply {
                arguments = args
            }
        }
    }
}