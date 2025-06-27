package com.example.android.circuitboardwarehouse.database

data class ClientInfo(
    val id: Long,
    val type: String,
    val phone: String,
    val email: String,
    val displayName: String?
)

data class PcbComponentInfo(
    val id: Long,
    val name: String,
    val type: String,
    val count: Int,
    val coordinates: String
)

data class OrderInfo(
    val id: Long,
    val client: String,
    val status: String,
    val totalAmount: Double,
    val registrationDate: Long,
    val shippingDate: Long?
)

data class OrderItemInfo(
    val id: Long,
    val pcb: String,
    val quantity: Int,
    val price: Double
)

interface ComponentDetails {
    val type: String // Тип компонента (резистор, конденсатор и т.д.)
}

data class ResistorDetails(
    override val type: String = "Резистор",
    var resistance: String = "", // Сопротивление
    var tolerance: String = "",  // Допуск
    var powerRating: String = ""  // Мощность
) : ComponentDetails

data class CapacitorDetails(
    override val type: String = "Конденсатор",
    var capacitance: String = "", // Емкость
    var voltageRating: String = "", // Напряжение
    var maxTemperature: String = ""     // Максимальная температура
) : ComponentDetails

data class DiodeDetails(
    override val type: String = "Диод",
    var forwardVoltage: String = "", // Падение напряжения
    var reverseVoltage: String = "", // Обратное напряжение
    var forwardCurrent: String = ""   // Прямой ток
) : ComponentDetails
