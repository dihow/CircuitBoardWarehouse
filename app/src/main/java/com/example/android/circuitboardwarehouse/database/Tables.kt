package com.example.android.circuitboardwarehouse.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val address: String,
    val phone: String,
    val email: String,
    val position: String,
    val salary: Double,
    val login: String,
    val passwordHash: String,
    val salt: String
)

@Entity(tableName = "pcb")
data class Pcb(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val serialNumber: String,
    val batch: String,
    val description: String?,
    val price: Double,
    val totalStock: Int,
    val orderedQuantity: Int,
    val manufacturingDate: Long,
    val length: Double,
    val width: Double,
    val layerCount: Int,
    val comment: String?,
    val imagePath: String? = null
)

@Entity(tableName = "components")
data class Component(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val manufacturer: String,
    val price: Double,
    val type: String,
    val stockQuantity: Int
)

@Entity(tableName = "component_specifications",
    foreignKeys = [
        ForeignKey(
            entity = Component::class,
            parentColumns = ["id"],
            childColumns = ["componentId"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        )
    ],
    indices = [Index("componentId")]
)
data class ComponentSpecification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val componentId: Long,
    val specification: String,
    val specificationValue: String
)

@Entity(tableName = "pcb_components",
    primaryKeys = ["pcbId", "componentId"],
    foreignKeys = [
        ForeignKey(
            entity = Pcb::class,
            parentColumns = ["id"],
            childColumns = ["pcbId"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        ),
        ForeignKey(
            entity = Component::class,
            parentColumns = ["id"],
            childColumns = ["componentId"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        )
    ],
    indices = [Index("pcbId"), Index("componentId")]
)
data class PcbComponent(
    val pcbId: Long,
    val componentId: Long,
    val componentCount: Int,
    val coordinates: String?
)

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val phone: String,
    val email: String
)

@Entity(tableName = "physical_persons",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        )
    ],
    indices = [Index("clientId")]
)
data class PhysicalPerson(
    @PrimaryKey val clientId: Long,
    val fullName: String,
    val address: String,
    val age: Int
)

@Entity(tableName = "legal_entities",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        )
    ],
    indices = [Index("clientId")]
)
data class LegalEntity(
    @PrimaryKey val clientId: Long,
    val name: String,
    val inn: String,
    val contactPerson: String,
    val legalAddress: String,
    val actualAddress: String
)

@Entity(tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        )
    ],
    indices = [Index("clientId")]
)
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val registrationDate: Long,
    val status: String,
    val totalAmount: Double,
    val shippingDate: Long?,
    val shippingCompany: String?
)

@Entity(tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = Order::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        ),
        ForeignKey(
            entity = Pcb::class,
            parentColumns = ["id"],
            childColumns = ["pcbId"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        )
    ],
    indices = [Index("orderId"), Index("pcbId")]
)
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val pcbId: Long,
    val quantity: Int,
    val pricePerPcb: Double
)

@Entity(tableName = "movements")
data class Movement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val movementType: String,
    val productType: String,
    val description: String?,
    val value: Int,
    val date: Long
)