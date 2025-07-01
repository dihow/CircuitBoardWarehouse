package com.example.android.circuitboardwarehouse.activity

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
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
import com.example.android.circuitboardwarehouse.database.Pcb
import com.example.android.circuitboardwarehouse.viewmodel.PcbsListViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PcbsListActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PcbAdapter
    private lateinit var searchEditText: EditText

    private val viewModel: PcbsListViewModel by lazy {
        ViewModelProvider(this)[PcbsListViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pcbs_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        searchEditText = findViewById(R.id.search_edit_text)
        recyclerView = findViewById(R.id.pcb_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = PcbAdapter()
        recyclerView.adapter = adapter

        viewModel.pcbsListLiveData.observe(this) { pcbs ->
            val searchText = searchEditText.text.toString()
            if (searchText.isNotEmpty()) {
                filterPcbs(pcbs, searchText)
            } else {
                adapter.updateItems(pcbs)
            }
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val searchText = s.toString().trim()
                viewModel.pcbsListLiveData.value?.let { pcbs ->
                    filterPcbs(pcbs, searchText)
                }
            }
        })

        findViewById<FloatingActionButton>(R.id.add_pcb_button).setOnClickListener {
            val intent = PcbEditActivity.newIntent(this)
            startActivity(intent)
        }
    }

    private fun filterPcbs(pcbs: List<Pcb>, searchText: String) {
        if (searchText.isEmpty()) {
            adapter.updateItems(pcbs)
            return
        }

        val filteredList = pcbs.filter { pcb ->
            pcb.name.contains(searchText, ignoreCase = true)
        }
        adapter.updateItems(filteredList)
    }

    private inner class PcbAdapter : GenericRecyclerAdapter<Pcb, PcbViewHolder>(
        items = emptyList(),
        viewHolderCreator = { view -> PcbViewHolder(view) },
        onBindViewHolder = { holder, pcb -> holder.bind(pcb) },
        getItemLayoutRes = { R.layout.pcb_card }
    )

    private inner class PcbViewHolder(view: View) : GenericViewHolder<Pcb>(view) {
        private val nameTextView: TextView = view.findViewById(R.id.pcb_name_text_view)
        private val descriptionTextView: TextView = view.findViewById(R.id.pcb_description_text_view)
        private val priceTextView: TextView = view.findViewById(R.id.pcb_price_text_view)
        private val stockTextView: TextView = view.findViewById(R.id.pcb_stock_text_view)
        private val imageView: ImageView = view.findViewById(R.id.pcb_image_view)

        override fun bind(item: Pcb) {
            nameTextView.text = item.name
            descriptionTextView.text = item.description ?: "Описание отсутствует"
            priceTextView.text = "Стоимость: ${item.price} руб."
            stockTextView.text = "Всего на складе: ${item.totalStock} шт."
            item.imagePath?.let { path ->
                try {
                    val imageUri = Uri.parse(path)
                    val inputStream = itemView.context.contentResolver.openInputStream(imageUri)
                    if (inputStream != null) {
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 4
                        }
                        val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                        inputStream.close()

                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap)
                        } else {
                            Log.e("PcbViewHolder", "Bitmap decoding failed")
                            imageView.setImageResource(R.drawable.circuit_placeholder)
                        }
                    } else {
                        Log.e("PcbViewHolder", "InputStream is null")
                        imageView.setImageResource(R.drawable.circuit_placeholder)
                    }

                } catch (e: Exception) {
                    Log.e("PcbsListActivity", "Error loading image: $path", e)
                    imageView.setImageResource(R.drawable.circuit_placeholder)
                }
            } ?: run {
                imageView.setImageResource(R.drawable.circuit_placeholder)
            }

            itemView.setOnClickListener {
                val intent = PcbEditActivity.newIntent(itemView.context, item.id)
                itemView.context.startActivity(intent)
            }
        }
    }

    companion object {
        fun newIntent(packageContext: Context) =
            Intent(packageContext, PcbsListActivity::class.java)
    }
}