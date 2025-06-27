package com.example.android.circuitboardwarehouse.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.android.circuitboardwarehouse.WarehouseRepository
import com.example.android.circuitboardwarehouse.database.OrderInfo

class OrdersListViewModel : ViewModel() {
    private val warehouseRepository = WarehouseRepository.Companion.get()
    val ordersListLiveData: LiveData<List<OrderInfo>> = warehouseRepository.getAllOrderInfo()
}