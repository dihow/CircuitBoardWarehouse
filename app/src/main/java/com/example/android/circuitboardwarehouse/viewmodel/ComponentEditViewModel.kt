
package com.example.android.circuitboardwarehouse.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.circuitboardwarehouse.database.CapacitorDetails
import com.example.android.circuitboardwarehouse.database.ComponentDetails
import com.example.android.circuitboardwarehouse.database.DiodeDetails
import com.example.android.circuitboardwarehouse.database.ResistorDetails
import com.example.android.circuitboardwarehouse.WarehouseRepository
import com.example.android.circuitboardwarehouse.database.Component
import com.example.android.circuitboardwarehouse.database.ComponentSpecification
import com.example.android.circuitboardwarehouse.database.Movement
import kotlinx.coroutines.launch
import kotlin.math.abs

class ComponentEditViewModel : ViewModel() {
    private val warehouseRepository = WarehouseRepository.Companion.get()

    val selectedComponentType = MutableLiveData<String>()
    val componentDetails = MutableLiveData<ComponentDetails>()
    val componentIdResult = MutableLiveData<Long?>()

    val componentTypes = listOf("Резистор", "Конденсатор", "Диод")

    init {
        selectedComponentType.value = componentTypes.firstOrNull()
        updateComponentDetails()
    }

    fun onComponentTypeSelected(type: String) {
        selectedComponentType.value = type
        updateComponentDetails()
    }

    private fun updateComponentDetails() {
        componentDetails.value = when (selectedComponentType.value) {
            "Резистор" -> ResistorDetails()
            "Конденсатор" -> CapacitorDetails()
            "Диод" -> DiodeDetails()
            else -> ResistorDetails()
        }
    }

    fun getComponent(componentId: Long) = warehouseRepository.getComponentLiveDataById(componentId)

    fun getComponentSpecifications(componentId: Long) = warehouseRepository.getComponentSpecificationsByComponentId(componentId)

    fun saveComponent(serialNumber: String, manufacturer: String, price: String, totalStock: String) {
        viewModelScope.launch {
            val componentId = warehouseRepository.addComponent(
                Component(type = componentDetails.value?.type.toString(),
                    name = serialNumber, manufacturer = manufacturer,
                    price = price.toDouble(), stockQuantity = totalStock.toInt())
            )
            componentIdResult.postValue(componentId)

            warehouseRepository.addMovement(
                Movement(
                    movementType = "Приход",
                    productType = "Компонент",
                    description = "Поступление на склад ${totalStock.toInt()} компонентов \"$serialNumber\"",
                    value = totalStock.toInt(),
                    date = System.currentTimeMillis()
                )
            )

            val details = componentDetails.value

            if (details != null) {
                when (details) {
                    is ResistorDetails -> {
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(componentId = componentId,
                                specification = "Сопротивление",
                                specificationValue = details.resistance.toString())
                        )
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(componentId = componentId,
                                specification = "Допуск",
                                specificationValue = details.tolerance.toString())
                        )
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(componentId = componentId,
                                specification = "Мощность",
                                specificationValue = details.powerRating.toString())
                        )
                    }
                    is CapacitorDetails -> {
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(componentId = componentId,
                                specification = "Ёмкость",
                                specificationValue = details.capacitance.toString())
                        )
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(componentId = componentId,
                                specification = "Напряжение",
                                specificationValue = details.voltageRating.toString())
                        )
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(componentId = componentId,
                                specification = "Максимальная температура",
                                specificationValue = details.maxTemperature.toString())
                        )
                    }
                    is DiodeDetails -> {
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(componentId = componentId,
                                specification = "Падение напряжения",
                                specificationValue = details.forwardVoltage.toString())
                        )
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(componentId = componentId,
                                specification = "Обратное напряжение",
                                specificationValue = details.reverseVoltage.toString())
                        )
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(componentId = componentId,
                                specification = "Прямой ток",
                                specificationValue = details.forwardCurrent.toString())
                        )
                    }
                }
            }
        }
    }

    fun updateComponent(componentId: Long, name: String, manufacturer: String, price: String, totalStock: String) {
        viewModelScope.launch {
            val oldComponent = warehouseRepository.getComponentById(componentId)
            val newQuantity = totalStock.toInt()

            warehouseRepository.updateComponent(
                Component(
                    id = componentId,
                    type = componentDetails.value?.type.toString(),
                    name = name, manufacturer = manufacturer,
                    price = price.toDouble(), stockQuantity = totalStock.toInt()
                )
            )

            if (oldComponent != null && oldComponent.stockQuantity != newQuantity) {
                val difference = newQuantity - oldComponent.stockQuantity
                val movementType = if (difference > 0) "Поступление" else "Списание"

                warehouseRepository.addMovement(
                    Movement(
                        movementType = movementType,
                        productType = "Компонент",
                        description = if (difference > 0)
                            "Поступление на склад $difference компонентов \"$name\""
                        else "Списание со склада ${abs(difference)} компонентов \"$name\"",
                        value = abs(difference),
                        date = System.currentTimeMillis()
                    )
                )
            }

            val details = componentDetails.value

            if (details != null) {
                warehouseRepository.deleteComponentSpecificationsByComponentId(componentId)

                when (details) {
                    is ResistorDetails -> {
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(
                                componentId = componentId,
                                specification = "Сопротивление",
                                specificationValue = details.resistance.toString()
                            )
                        )
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(
                                componentId = componentId,
                                specification = "Допуск",
                                specificationValue = details.tolerance.toString()
                            )
                        )
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(
                                componentId = componentId,
                                specification = "Мощность",
                                specificationValue = details.powerRating.toString()
                            )
                        )
                    }

                    is CapacitorDetails -> {
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(
                                componentId = componentId,
                                specification = "Ёмкость",
                                specificationValue = details.capacitance.toString()
                            )
                        )
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(
                                componentId = componentId,
                                specification = "Напряжение",
                                specificationValue = details.voltageRating.toString()
                            )
                        )
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(
                                componentId = componentId,
                                specification = "Максимальная температура",
                                specificationValue = details.maxTemperature.toString()
                            )
                        )
                    }

                    is DiodeDetails -> {
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(
                                componentId = componentId,
                                specification = "Падение напряжения",
                                specificationValue = details.forwardVoltage.toString()
                            )
                        )
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(
                                componentId = componentId,
                                specification = "Обратное напряжение",
                                specificationValue = details.reverseVoltage.toString()
                            )
                        )
                        warehouseRepository.addComponentSpecification(
                            ComponentSpecification(
                                componentId = componentId,
                                specification = "Прямой ток",
                                specificationValue = details.forwardCurrent.toString()
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun handleComponentStockChange(
        componentId: Long,
        oldStock: Int,
        newStock: Int,
        componentName: String
    ) = warehouseRepository.handleComponentStockChange(componentId, oldStock, newStock, componentName)
}