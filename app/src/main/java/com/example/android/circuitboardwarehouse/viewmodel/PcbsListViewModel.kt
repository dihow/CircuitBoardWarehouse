package com.example.android.circuitboardwarehouse.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.android.circuitboardwarehouse.WarehouseRepository
import com.example.android.circuitboardwarehouse.database.Pcb

class PcbsListViewModel : ViewModel() {
    private val warehouseRepository = WarehouseRepository.Companion.get()
    val pcbsListLiveData: LiveData<List<Pcb>> = warehouseRepository.getPcbs()
}