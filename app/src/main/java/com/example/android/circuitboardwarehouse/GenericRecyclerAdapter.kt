package com.example.android.circuitboardwarehouse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

abstract class GenericRecyclerAdapter<T, VH : RecyclerView.ViewHolder>(
    private var items: List<T>,
    private val viewHolderCreator: (View) -> VH,
    private val onBindViewHolder: (VH, T) -> Unit,
    private val getItemLayoutRes: () -> Int
) : RecyclerView.Adapter<VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(getItemLayoutRes(), parent, false)
        return viewHolderCreator(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        onBindViewHolder(holder, items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<T>) {
        items = newItems
        notifyDataSetChanged()
    }
}