package com.example.android.circuitboardwarehouse

import java.util.TimeZone

class OrderStatusChecker {
    private val repository = WarehouseRepository.get()

    suspend fun checkOrdersStatus() {
        val timeZone = TimeZone.getTimeZone("Europe/Moscow")

        val currentTimeMillis = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance(timeZone)
        calendar.timeInMillis = currentTimeMillis
        val currentTimeInTimeZone = calendar.timeInMillis

        val doneOrders = repository.getOrders().filter { it.status == "Готов" }

        doneOrders.forEach { order ->
            order.shippingDate?.let { shippingDate ->
                if (shippingDate < currentTimeInTimeZone) {
                    repository.runInTransaction {
                        repository.updateOrder(order.copy(status = "Отправлен"))
                    }
                }
            }
        }
    }
}
