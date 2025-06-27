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
import com.example.android.circuitboardwarehouse.database.Movement
import com.example.android.circuitboardwarehouse.database.OrderInfo
import com.example.android.circuitboardwarehouse.viewmodel.MovementsViewModel
import com.example.android.circuitboardwarehouse.viewmodel.OrdersListViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MovementsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MovementAdapter

    private val viewModel: MovementsViewModel by lazy {
        ViewModelProvider(this)[MovementsViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_movements)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.movement_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = MovementAdapter()
        recyclerView.adapter = adapter

        viewModel.movementsListLiveData.observe(this) { orders ->
            adapter.updateItems(orders)
        }
    }

    private inner class MovementAdapter : GenericRecyclerAdapter<Movement, MovementViewHolder>(
        items = emptyList(),
        viewHolderCreator = { view -> MovementViewHolder(view) },
        onBindViewHolder = { holder, movement -> holder.bind(movement) },
        getItemLayoutRes = { R.layout.movement_card }
    )

    private inner class MovementViewHolder(view: View) : GenericViewHolder<Movement>(view) {
        private val movementTypeTextView: TextView = view.findViewById(R.id.movement_type_text_view)
        private val descriptionTextView: TextView = view.findViewById(R.id.description_text_view)
        private val dateTextView: TextView = view.findViewById(R.id.date_text_view)

        override fun bind(item: Movement) {
            movementTypeTextView.text = item.movementType
            descriptionTextView.text = item.description

            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("Europe/Moscow")
            dateTextView.text = dateFormat.format(Date(item.date))
        }
    }

    companion object {
        fun newIntent(packageContext: Context) =
            Intent(packageContext, MovementsActivity::class.java)
    }
}