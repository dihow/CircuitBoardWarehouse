package com.example.android.circuitboardwarehouse.viewmodel

import androidx.lifecycle.ViewModel
import com.example.android.circuitboardwarehouse.WarehouseRepository

class MovementsViewModel : ViewModel() {
    val warehouseRepository = WarehouseRepository.get()

    val movementsListLiveData = warehouseRepository.getAllMovements()
}