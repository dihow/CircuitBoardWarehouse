package com.example.android.circuitboardwarehouse.viewmodel

import androidx.lifecycle.ViewModel
import com.example.android.circuitboardwarehouse.WarehouseRepository
import com.example.android.circuitboardwarehouse.database.Movement
import com.example.android.circuitboardwarehouse.database.Order
import com.example.android.circuitboardwarehouse.database.OrderItem
import com.example.android.circuitboardwarehouse.database.Pcb

class CartViewModel : ViewModel() {
    private val warehouseRepository = WarehouseRepository.get()

    fun orderItemLiveData(orderId: Long) = warehouseRepository.getOrderItemInfoByOrderId(orderId)

    suspend fun getOrderItemById(id: Long) = warehouseRepository.getOrderItemById(id)

    suspend fun getPcbById(id: Long) = warehouseRepository.getPcbById(id)

    suspend fun getPcbsSuspend() = warehouseRepository.getPcbsSuspend()

    suspend fun getOrderItemByOrderAndPcb(orderId: Long, pcbId: Long) =
        warehouseRepository.getOrderItemByOrderAndPcb(orderId, pcbId)

    suspend fun calculateOrderTotal(orderId: Long): Double {
        return warehouseRepository.getOrderItemsByOrderId(orderId).sumOf {
            it.quantity * it.pricePerPcb
        }
    }

    suspend fun updateOrder(order: Order) = warehouseRepository.updateOrder(order)

    suspend fun updateOrderItem(orderItem: OrderItem) {
        val existingItem = getOrderItemById(orderItem.id) ?: return

        runInTransaction {
            warehouseRepository.updateOrderItem(orderItem)

            if (existingItem.quantity != orderItem.quantity ||
                existingItem.pcbId != orderItem.pcbId) {

                val pcb = getPcbById(orderItem.pcbId) ?: return@runInTransaction

                val movement = Movement(
                    movementType = "Расход",
                    productType = "Плата",
                    description = "Расход ${orderItem.quantity} плат \"${pcb.name}\" по заказу",
                    value = orderItem.quantity,
                    date = System.currentTimeMillis()
                )

                warehouseRepository.addMovement(movement)
            }
        }
    }

    suspend fun addOrderItem(orderItem: OrderItem) = warehouseRepository.addOrderItem(orderItem)

    suspend fun deleteOrderItem(orderItem: OrderItem) =
        warehouseRepository.deleteOrderItem(orderItem)

    suspend fun getOrderById(id: Long) = warehouseRepository.getOrderById(id)

    suspend fun updatePcb(pcb: Pcb) = warehouseRepository.updatePcb(pcb)

    suspend fun <R> runInTransaction(block: suspend () -> R) =
        warehouseRepository.runInTransaction(block)

    private suspend fun handlePcbStockChange(
        pcbId: Long,
        oldStock: Int,
        newStock: Int,
        pcbName: String
    ) = warehouseRepository.handlePcbStockChange(pcbId, oldStock, newStock, pcbName)
}