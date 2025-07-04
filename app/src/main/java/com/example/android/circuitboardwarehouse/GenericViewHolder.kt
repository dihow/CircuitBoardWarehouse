package com.example.android.circuitboardwarehouse

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class GenericViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: T)
}