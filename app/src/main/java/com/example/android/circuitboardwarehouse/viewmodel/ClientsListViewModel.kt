package com.example.android.circuitboardwarehouse.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.android.circuitboardwarehouse.database.Client
import com.example.android.circuitboardwarehouse.database.ClientInfo
import com.example.android.circuitboardwarehouse.database.PhysicalPerson
import androidx.lifecycle.viewModelScope
import com.example.android.circuitboardwarehouse.WarehouseRepository
import com.example.android.circuitboardwarehouse.database.LegalEntity
import kotlinx.coroutines.launch

class ClientsListViewModel : ViewModel() {
    private val warehouseRepository = WarehouseRepository.Companion.get()
    val clientsListLiveData: LiveData<List<ClientInfo>> = warehouseRepository.getAllClientInfo()
}