package com.example.android.circuitboardwarehouse.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.circuitboardwarehouse.database.PcbComponentInfo
import com.example.android.circuitboardwarehouse.WarehouseRepository
import com.example.android.circuitboardwarehouse.database.Component
import com.example.android.circuitboardwarehouse.database.PcbComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PcbComponentsViewModel : ViewModel() {
    private val warehouseRepository = WarehouseRepository.get()

    private val _componentsLiveData = MutableLiveData<List<PcbComponentInfo>>()
    val componentsLiveData: LiveData<List<PcbComponentInfo>> = _componentsLiveData

    fun loadComponents(pcbId: Long) {
        Log.d("PcbComponentsViewModel", "loadComponents invoked for pcbId: $pcbId")
        viewModelScope.launch {
            warehouseRepository.getPcbComponentInfoByPcbId(pcbId).observeForever { components ->
                Log.d("PcbComponentsViewModel", "Components loaded: $components")
                _componentsLiveData.postValue(components)
            }
        }
    }

    private val _allComponents = MutableLiveData<List<Component>>()
    val allComponents: LiveData<List<Component>> = _allComponents

    init {
        loadAllComponents()
    }

    private fun loadAllComponents() {
        viewModelScope.launch {
            warehouseRepository.getComponents().observeForever { components ->
                Log.d("PcbComponentsViewModel", "Components loaded: $components")
                _allComponents.postValue(components)
            }
        }
    }

    suspend fun updateComponentOnPcb(
        context: Context,
        pcbId: Long,
        componentId: Long,
        newCount: Int,
        newCoordinates: String
    ): Boolean {
        return viewModelScope.async {
            try {
                val pcb = warehouseRepository.getPcbById(pcbId) ?: return@async false
                val component = warehouseRepository.getComponentById(componentId)
                    ?: return@async false
                val currentPcbComponent =
                    warehouseRepository.getPcbComponentByPcbIdAndComponentId(pcbId, componentId)

                val totalPcbCount = pcb.totalStock

                val oldComponentCount = currentPcbComponent?.componentCount ?: 0
                val totalAvailable = component.stockQuantity + (oldComponentCount * totalPcbCount)

                val totalRequired = newCount * totalPcbCount
                if (totalRequired > totalAvailable) {
                    val deficit = totalRequired - totalAvailable

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Недостаточно компонентов. Дефицит: $deficit",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    return@async false
                }

                warehouseRepository.runInTransaction {
                    val pcbComponent = PcbComponent(
                        pcbId = pcbId,
                        componentId = componentId,
                        componentCount = newCount,
                        coordinates = newCoordinates
                    )

                    if (currentPcbComponent == null) {
                        warehouseRepository.addPcbComponent(pcbComponent)
                    } else {
                        warehouseRepository.updatePcbComponent(pcbComponent)
                    }

                    val newStock = totalAvailable - totalRequired
                    warehouseRepository.updateComponentStock(componentId, newStock)
                }

                true
            } catch (e: Exception) {
                Log.e("PcbComponentsVM", "Error updating component", e)
                false
            }
        }.await()
    }

    suspend fun getPcbById(pcbId: Long) =
        withContext(viewModelScope.coroutineContext) {
            warehouseRepository.getPcbById(pcbId)
        }

    private suspend fun handleComponentStockChange(
        componentId: Long,
        oldStock: Int,
        newStock: Int,
        componentName: String
    ) = warehouseRepository.handleComponentStockChange(componentId, oldStock, newStock, componentName)
}