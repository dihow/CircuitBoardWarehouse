package com.example.android.circuitboardwarehouse.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.android.circuitboardwarehouse.GenericRecyclerAdapter
import com.example.android.circuitboardwarehouse.GenericViewHolder
import com.example.android.circuitboardwarehouse.OrderStatusWorker
import com.example.android.circuitboardwarehouse.R
import com.example.android.circuitboardwarehouse.database.OrderInfo
import com.example.android.circuitboardwarehouse.viewmodel.OrdersListViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class OrdersListActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OrderAdapter

    private val viewModel: OrdersListViewModel by lazy {
        ViewModelProvider(this)[OrdersListViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_orders_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.order_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = OrderAdapter()
        recyclerView.adapter = adapter

        viewModel.ordersListLiveData.observe(this) { orders ->
            adapter.updateItems(orders)
        }

        findViewById<FloatingActionButton>(R.id.add_order_button).setOnClickListener {
            val intent = OrderEditActivity.newIntent(this)
            startActivity(intent)
        }

        findViewById<Button>(R.id.check_orders_button).setOnClickListener {
            startOrderStatusCheck()
        }
    }

    private fun startOrderStatusCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<OrderStatusWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueue(oneTimeWorkRequest)
    }

    private inner class OrderAdapter : GenericRecyclerAdapter<OrderInfo, OrderViewHolder>(
        items = emptyList(),
        viewHolderCreator = { view -> OrderViewHolder(view) },
        onBindViewHolder = { holder, order -> holder.bind(order) },
        getItemLayoutRes = { R.layout.order_card }
    )

    private inner class OrderViewHolder(view: View) : GenericViewHolder<OrderInfo>(view) {
        private val clientTextView: TextView = view.findViewById(R.id.client_text_view)
        private val statusTextView: TextView = view.findViewById(R.id.status_text_view)
        private val totalAmountTextView: TextView = view.findViewById(R.id.total_amount_text_view)
        private val registrationDateTextView: TextView = view.findViewById(R.id.registration_date_text_view)
        private val shippingDateTextView: TextView = view.findViewById(R.id.shipping_date_text_view)

        override fun bind(item: OrderInfo) {
            clientTextView.text = "Заказчик: ${item.client}"
            statusTextView.text = "Статус: ${item.status}"
            totalAmountTextView.text = "Стоимость товаров: ${item.totalAmount} руб."

            val timeZone = TimeZone.getTimeZone("Europe/Moscow")

            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            dateFormat.timeZone = timeZone

            registrationDateTextView.text = "Дата регистрации ${dateFormat.format(Date(item.registrationDate))}"
            shippingDateTextView.text = "Дата отгрузки: " + (if (item.shippingDate != null)
                dateFormat.format(Date(item.shippingDate)) else "-")

            itemView.setOnClickListener {
                val intent = OrderEditActivity.newIntent(itemView.context, item.id)
                itemView.context.startActivity(intent)
            }
        }
    }

    companion object {
        fun newIntent(packageContext: Context) =
            Intent(packageContext, OrdersListActivity::class.java)
    }
}