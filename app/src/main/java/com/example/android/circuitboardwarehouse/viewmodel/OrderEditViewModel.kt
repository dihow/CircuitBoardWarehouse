package com.example.android.circuitboardwarehouse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.circuitboardwarehouse.WarehouseRepository
import com.example.android.circuitboardwarehouse.database.Order
import com.example.android.circuitboardwarehouse.database.Pcb
import kotlinx.coroutines.withContext

class OrderEditViewModel : ViewModel() {
    private val warehouseRepository = WarehouseRepository.get()

    suspend fun getClientId(client: String): Long? =
        withContext(viewModelScope.coroutineContext) {
            try {
                warehouseRepository.getClientIdByName(client)
            } catch (e: Exception) {
                Log.e("ClientRepository", "Error getting client ID by name: ${e.message}")
                null
            }
        }

    suspend fun updateOrder(order: Order) = warehouseRepository.updateOrder(order)

    suspend fun addOrder(order: Order) = warehouseRepository.addOrder(order)

    fun getOrderLiveData(id: Long) = warehouseRepository.getOrderLiveDataById(id)

    suspend fun getOrderById(id: Long) = warehouseRepository.getOrderById(id)

    suspend fun getClientNameById(id: Long) =
        withContext(viewModelScope.coroutineContext) {
            try {
                warehouseRepository.getClientNameById(id)
            } catch (e: Exception) {
                Log.e("ClientRepository", "Error getting client name by ID: ${e.message}")
                null
            }
        }

    suspend fun getAllClientNames(): List<String> {
        return warehouseRepository.getAllClientNames()
    }

    suspend fun getOrderItemsByOrderId(id: Long) = warehouseRepository.getOrderItemsByOrderId(id)

    suspend fun getPcbById(id: Long) = warehouseRepository.getPcbById(id)

    suspend fun updatePcb(pcb: Pcb) = warehouseRepository.updatePcb(pcb)

    suspend fun <R> runInTransaction(block: suspend () -> R) =
        warehouseRepository.runInTransaction(block)
}