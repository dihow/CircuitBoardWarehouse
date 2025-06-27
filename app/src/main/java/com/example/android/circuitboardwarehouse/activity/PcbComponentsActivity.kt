package com.example.android.circuitboardwarehouse.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.android.circuitboardwarehouse.database.PcbComponentInfo
import com.example.android.circuitboardwarehouse.R
import com.example.android.circuitboardwarehouse.viewmodel.PcbComponentsViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.getValue

class PcbComponentsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PcbComponentAdapter
    private var pcbId: Long = -1

    private val viewModel: PcbComponentsViewModel by lazy {
        ViewModelProvider(this)[PcbComponentsViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pcb_components)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        pcbId = intent.getLongExtra(EXTRA_PCB_ID, -1)
        if (pcbId == -1L) {
            finish()
            return
        }

        viewModel.loadComponents(pcbId)

        recyclerView = findViewById(R.id.pcb_components_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = PcbComponentAdapter()
        recyclerView.adapter = adapter

        viewModel.componentsLiveData.observe(this) { components ->
            Log.d("PcbComponentsActivity", "Components received: $components")
            if (components != null) {
                adapter.updateItems(components)
            } else {
                Log.w("PcbComponentsActivity", "Components data is null!")
                adapter.updateItems(emptyList())
            }
        }

        findViewById<FloatingActionButton>(R.id.add_component_button).setOnClickListener {
            AddComponentDialogFragment(pcbId) {
                viewModel.loadComponents(pcbId)
            }.show(supportFragmentManager, "AddComponentDialog")
        }
    }

    private inner class PcbComponentAdapter : GenericRecyclerAdapter<PcbComponentInfo, PcbComponentViewHolder>(
        items = emptyList(),
        viewHolderCreator = { view -> PcbComponentViewHolder(view) },
        onBindViewHolder = { holder, component -> holder.bind(component) },
        getItemLayoutRes = { R.layout.pcb_component_card }
    )

    private inner class PcbComponentViewHolder(view: View) : GenericViewHolder<PcbComponentInfo>(view) {
        private val componentNameTextView: TextView = view.findViewById(R.id.component_name_text_view)
        private val componentTypeTextView: TextView = view.findViewById(R.id.component_type_text_view)
        private val countTextView: TextView = view.findViewById(R.id.component_count_text_view)
        private val coordinatesTextView: TextView = view.findViewById(R.id.coordinates_text_view)

        override fun bind(item: PcbComponentInfo) {
            componentNameTextView.text = "Наименование: ${item.name}"
            componentTypeTextView.text = item.type
            countTextView.text = "Количество на плате: ${item.count}"
            coordinatesTextView.text = "Координаты: ${item.coordinates ?: "N/A"}"

            itemView.setOnClickListener {
                val intent = ComponentEditActivity.newIntent(itemView.context, item.id)
                startActivity(intent)
            }
        }
    }

    companion object {
        const val EXTRA_PCB_ID = "pcb_id"

        fun newIntent(packageContext: Context, pcbId: Long) =
            Intent(packageContext, PcbComponentsActivity::class.java).apply {
                putExtra(EXTRA_PCB_ID, pcbId)
            }
    }
}