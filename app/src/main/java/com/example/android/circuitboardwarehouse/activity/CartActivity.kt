package com.example.android.circuitboardwarehouse.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.android.circuitboardwarehouse.GenericRecyclerAdapter
import com.example.android.circuitboardwarehouse.GenericViewHolder
import com.example.android.circuitboardwarehouse.R
import com.example.android.circuitboardwarehouse.database.OrderItemInfo
import com.example.android.circuitboardwarehouse.viewmodel.CartViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class CartActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OrderItemAdapter
    private var orderId: Long = -1

    private val viewModel: CartViewModel by lazy {
        ViewModelProvider(this)[CartViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cart)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        orderId = intent.getLongExtra(EXTRA_ORDER_ID, -1)
        if (orderId == -1L) {
            finish()
            return
        }

        recyclerView = findViewById(R.id.cart_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = OrderItemAdapter()
        recyclerView.adapter = adapter

        viewModel.orderItemLiveData(orderId).observe(this) { orderItems ->
            adapter.updateItems(orderItems)
        }

        findViewById<FloatingActionButton>(R.id.add_order_item_button).setOnClickListener {
            showAddEditPcbDialog()
        }

        lifecycleScope.launch {
            val order = viewModel.getOrderById(orderId)
            if (order?.status == "Готов") {
                findViewById<FloatingActionButton>(R.id.add_order_item_button).isEnabled = false
            }
        }
    }

    private fun showAddEditPcbDialog(orderItemId: Long? = null) {
        val dialog = EditOrderItemDialogFragment.newInstance(orderId, orderItemId)
        dialog.show(supportFragmentManager, "AddEditPcbDialog")
    }

    private inner class OrderItemAdapter : GenericRecyclerAdapter<OrderItemInfo, OrderItemViewHolder>(
        items = emptyList(),
        viewHolderCreator = { view -> OrderItemViewHolder(view) },
        onBindViewHolder = { holder, order -> holder.bind(order) },
        getItemLayoutRes = { R.layout.order_item_card }
    )

    private inner class OrderItemViewHolder(view: View) : GenericViewHolder<OrderItemInfo>(view) {
        private val pcbTextView: TextView = view.findViewById(R.id.pcb_text_view)
        private val quantityTextView: TextView = view.findViewById(R.id.quantity_text_view)
        private val priceTextView: TextView = view.findViewById(R.id.price_text_view)

        override fun bind(item: OrderItemInfo) {
            pcbTextView.text = item.pcb
            quantityTextView.text = "Количество: ${item.quantity} шт."
            priceTextView.text = "Стоимость: ${item.price} руб./шт."

            lifecycleScope.launch {
                itemView.isEnabled = viewModel.getOrderById(orderId)?.status != "Готов" &&
                        viewModel.getOrderById(orderId)?.status != "Отправлен"
            }

            itemView.setOnClickListener {
                showAddEditPcbDialog(item.id)
            }
        }
    }

    companion object {
        const val EXTRA_ORDER_ID = "order_id"

        fun newIntent(packageContext: Context, orderId: Long) =
            Intent(packageContext,CartActivity::class.java).apply {
                putExtra(EXTRA_ORDER_ID, orderId)
            }
    }
}