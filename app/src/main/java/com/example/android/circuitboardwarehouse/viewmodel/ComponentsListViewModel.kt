package com.example.android.circuitboardwarehouse.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.android.circuitboardwarehouse.WarehouseRepository
import com.example.android.circuitboardwarehouse.database.Component

class ComponentsListViewModel : ViewModel() {
    private val warehouseRepository = WarehouseRepository.Companion.get()
    val componentsListLiveData: LiveData<List<Component>> = warehouseRepository.getComponents()
}