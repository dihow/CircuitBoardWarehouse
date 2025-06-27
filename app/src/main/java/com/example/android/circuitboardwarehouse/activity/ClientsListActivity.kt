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
import com.example.android.circuitboardwarehouse.database.ClientInfo
import com.example.android.circuitboardwarehouse.viewmodel.ClientsListViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ClientsListActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClientAdapter

    private val viewModel: ClientsListViewModel by lazy {
        ViewModelProvider(this)[ClientsListViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_clients_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.client_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ClientAdapter()
        recyclerView.adapter = adapter

        viewModel.clientsListLiveData.observe(this@ClientsListActivity) { clients ->
            adapter.updateItems(clients)
        }

        findViewById<FloatingActionButton>(R.id.add_client_button).setOnClickListener {
            val intent = ClientEditActivity.newIntent(this)
            startActivity(intent)
        }
    }

    private inner class ClientAdapter
        : GenericRecyclerAdapter<ClientInfo, ClientViewHolder>(
        items = emptyList(),
        viewHolderCreator = { view -> ClientViewHolder(view) },
        onBindViewHolder = { holder, clientInfo -> holder.bind(clientInfo) },
        getItemLayoutRes = { R.layout.client_card }
    )

    private inner class ClientViewHolder(view: View) : GenericViewHolder<ClientInfo>(view) {
        private val nameTextView: TextView = view.findViewById(R.id.client_name_text_view)
        private val typeTextView: TextView = view.findViewById(R.id.client_type_text_view)
        private val phoneTextView: TextView = view.findViewById(R.id.client_phone_text_view)
        private val emailTextView: TextView = view.findViewById(R.id.client_email_text_view)

        override fun bind(item: ClientInfo) {
            nameTextView.text = item.displayName
            typeTextView.text = item.type
            phoneTextView.text = "Телефон: ${item.phone}"
            emailTextView.text = "Эл. почта: ${item.email}"

            itemView.setOnClickListener {
                val intent = ClientEditActivity.newIntent(itemView.context, item.id)
                itemView.context.startActivity(intent)
            }
        }
    }

    companion object {
        fun newIntent(packageContext: Context) =
            Intent(packageContext, ClientsListActivity::class.java)
    }
}