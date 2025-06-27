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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.android.circuitboardwarehouse.GenericRecyclerAdapter
import com.example.android.circuitboardwarehouse.GenericViewHolder
import com.example.android.circuitboardwarehouse.R
import com.example.android.circuitboardwarehouse.database.Component
import com.example.android.circuitboardwarehouse.viewmodel.ComponentsListViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ComponentsListActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ComponentAdapter

    private val viewModel: ComponentsListViewModel by lazy {
        ViewModelProvider(this)[ComponentsListViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_components_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.component_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ComponentAdapter()
        recyclerView.adapter = adapter

        viewModel.componentsListLiveData.observe(this) { components ->
            adapter.updateItems(components)
        }

        findViewById<FloatingActionButton>(R.id.add_component_button).setOnClickListener {
            val intent = ComponentEditActivity.newIntent(this)
            startActivity(intent)
        }
    }

    private inner class ComponentAdapter : GenericRecyclerAdapter<Component, ComponentViewHolder>(
        items = emptyList(),
        viewHolderCreator = { view -> ComponentViewHolder(view) },
        onBindViewHolder = { holder, pcb -> holder.bind(pcb) },
        getItemLayoutRes = { R.layout.component_card }
    )

    private inner class ComponentViewHolder(view: View) : GenericViewHolder<Component>(view) {
        private val typeTextView: TextView = view.findViewById(R.id.component_type_text_view)
        private val manufacturerTextView: TextView = view.findViewById(R.id.manufacturer_text_view)
        private val nameTextView: TextView = view.findViewById(R.id.name_text_view)
        private val stockTextView: TextView = view.findViewById(R.id.component_stock_text_view)

        override fun bind(item: Component) {
            typeTextView.text = item.type
            manufacturerTextView.text = "Производитель: ${item.manufacturer}"
            nameTextView.text = item.name
            stockTextView.text = "На складе: ${item.stockQuantity}"

            itemView.setOnClickListener {
                val intent = ComponentEditActivity.newIntent(itemView.context, item.id)
                itemView.context.startActivity(intent)
            }
        }
    }

    companion object {
        fun newIntent(packageContext: Context) =
            Intent(packageContext, ComponentsListActivity::class.java)
    }
}