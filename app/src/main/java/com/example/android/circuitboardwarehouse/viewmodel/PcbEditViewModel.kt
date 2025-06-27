package com.example.android.circuitboardwarehouse.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.circuitboardwarehouse.WarehouseRepository
import com.example.android.circuitboardwarehouse.database.Component
import com.example.android.circuitboardwarehouse.database.Pcb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.example.android.circuitboardwarehouse.database.Movement
import kotlin.math.abs

class PcbEditViewModel : ViewModel() {
    private val warehouseRepository = WarehouseRepository.Companion.get()

    private val _pcbId = MutableStateFlow<Long?>(null)
    val pcbId: StateFlow<Long?> = _pcbId.asStateFlow()

    fun addPcb(pcb: Pcb) {
        viewModelScope.launch {
            _pcbId.value = warehouseRepository.addPcb(pcb)
        }
    }

    fun updatePcb(pcb: Pcb) {
        viewModelScope.launch {
            warehouseRepository.updatePcb(pcb)
        }
    }

    fun getPcb(pcbId: Long): LiveData<Pcb?> {
        val liveData = MutableLiveData<Pcb?>()
        viewModelScope.launch {
            val pcb = warehouseRepository.getPcbById(pcbId)
            liveData.postValue(pcb)
        }
        return liveData
    }

    fun getPcbComponentsLiveDataByPcbId(pcbId: Long) =
        warehouseRepository.getPcbComponentsLiveDataByPcbId(pcbId)

    suspend fun getPcbComponentsByPcbId(pcbId: Long) =
        warehouseRepository.getPcbComponentsByPcbId(pcbId)

    fun getComponentById(componentId: Long): LiveData<Component?> {
        return warehouseRepository.getComponentLiveDataById(componentId)
    }

    suspend fun updatePcbAndComponents(
        pcbId: Long,
        name: String,
        serialNumber: String,
        batch: String,
        description: String?,
        price: Double,
        boardsCount: Int,
        orderedQuantity: Int,
        manufacturingDate: Long,
        length: Double,
        width: Double,
        layerCount: Int,
        comment: String?,
        imagePath: String?
    ) {
        try {
            warehouseRepository.runInTransaction {
                val existingPcb = warehouseRepository.getPcbById(pcbId)
                    ?: throw IllegalStateException("Плата не найдена")

                val previousBoardsCount = existingPcb.totalStock

                val boardsCountChange = boardsCount - previousBoardsCount

                val pcbComponents = warehouseRepository.getPcbComponentsByPcbId(pcbId)

                for (pcbComponent in pcbComponents) {
                    val component = warehouseRepository.getComponentById(pcbComponent.componentId)
                        ?: throw IllegalStateException("Компонент не найден")
                    val requiredQuantityChange = boardsCountChange * pcbComponent.componentCount

                    val newStockQuantity = component.stockQuantity - requiredQuantityChange

                    if (newStockQuantity < 0) {
                        throw IllegalStateException(
                            "Недостаточно компонентов ${component.name}. Дефицит: ${abs(newStockQuantity)}")
                    }

                    warehouseRepository
                        .updateComponent(component.copy(stockQuantity = newStockQuantity))
                }

                val updatedPcb = existingPcb.copy(
                    name = name,
                    serialNumber = serialNumber,
                    batch = batch,
                    description = description,
                    price = price,
                    totalStock = boardsCount,
                    orderedQuantity = orderedQuantity,
                    manufacturingDate = manufacturingDate,
                    length = length,
                    width = width,
                    layerCount = layerCount,
                    comment = comment,
                    imagePath = imagePath
                )
                warehouseRepository.updatePcb(updatedPcb)
            }

        } catch (e: Exception) {
            Log.e("PcbEditViewModel", "Error updating PCB and components", e)
            throw e
        }
    }

    private suspend fun handlePcbStockChange(
        pcbId: Long,
        oldStock: Int,
        newStock: Int,
        pcbName: String
    ) = warehouseRepository.handlePcbStockChange(pcbId, oldStock, newStock, pcbName)
}