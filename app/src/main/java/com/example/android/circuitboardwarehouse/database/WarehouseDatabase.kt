package com.example.android.circuitboardwarehouse.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [
    Employee::class,
    Pcb::class,
    Component::class,
    ComponentSpecification::class,
    PcbComponent::class,
    Client::class,
    PhysicalPerson::class,
    LegalEntity::class,
    Order::class,
    OrderItem::class,
    Movement::class
], version = 5, exportSchema = true)
abstract class WarehouseDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun pcbDao(): PcbDao
    abstract fun componentDao(): ComponentDao
    abstract fun componentSpecificationDao(): ComponentSpecificationDao
    abstract fun pcbComponentDao(): PcbComponentDao
    abstract fun clientDao(): ClientDao
    abstract fun physicalPersonDao(): PhysicalPersonDao
    abstract fun legalEntityDao(): LegalEntityDao
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun movementDao(): MovementDao
}