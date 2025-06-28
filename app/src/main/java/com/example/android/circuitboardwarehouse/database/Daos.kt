package com.example.android.circuitboardwarehouse.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface EmployeeDao {
    @Query("SELECT salt FROM employees WHERE login = :login LIMIT 1")
    suspend fun getEmployeeSalt(login: String): String?

    @Query("SELECT * FROM employees WHERE login = :login LIMIT 1")
    suspend fun getByLogin(login: String): Employee?

    @Query("SELECT * FROM employees")
    suspend fun getAll(): List<Employee>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getById(id: Long): Employee?

    @Query("SELECT count(*) from employees")
    suspend fun getCount(): Int

    @Insert
    suspend fun insert(employee: Employee): Long

    @Update
    suspend fun update(employee: Employee)

    @Query("UPDATE employees SET login = :login, passwordHash = :password, salt = :salt WHERE id = :id")
    suspend fun updateLoginAndPasswordById(id: Long, login: String, password: String, salt: String)

    @Delete
    suspend fun delete(employee: Employee)
}

@Dao
interface PcbDao {
    @Query("SELECT * FROM pcb")
    suspend fun getAllSuspend(): List<Pcb>

    @Query("SELECT * FROM pcb")
    fun getAll(): LiveData<List<Pcb>>

    @Query("SELECT * FROM pcb WHERE id = :id")
    suspend fun getById(id: Long): Pcb?

    @Query("SELECT name FROM pcb")
    suspend fun getAllPcbsName(): List<String>

    @Insert
    suspend fun insert(pcb: Pcb): Long

    @Query("UPDATE pcb SET orderedQuantity = :orderedQuantity WHERE id = :pcbId")
    suspend fun updateOrderedQuantityByPcbId(pcbId: Long, orderedQuantity: Int)

    @Update
    suspend fun update(pcb: Pcb)

    @Delete
    suspend fun delete(pcb: Pcb)
}

@Dao
interface ComponentDao {
    @Query("SELECT * FROM components")
    fun getAll(): LiveData<List<Component>>

    @Query("SELECT * FROM components WHERE id = :id")
    fun getLiveDataById(id: Long): LiveData<Component?>

    @Query("SELECT * FROM components WHERE id = :id")
    suspend fun getById(id: Long): Component?

    @Insert
    suspend fun insert(component: Component): Long

    @Update
    suspend fun update(component: Component)

    @Delete
    suspend fun delete(component: Component)

    @Query("UPDATE components SET stockQuantity = :newQuantity WHERE id = :componentId")
    suspend fun updateStock(componentId: Long, newQuantity: Int)
}

@Dao
interface ComponentSpecificationDao {
    @Query("SELECT * FROM component_specifications WHERE componentId = :componentId")
    fun getByComponentId(componentId: Long): LiveData<List<ComponentSpecification>>

    @Insert
    suspend fun insert(specification: ComponentSpecification): Long

    @Update
    suspend fun update(specification: ComponentSpecification)

    @Delete
    suspend fun delete(specification: ComponentSpecification)

    @Query("DELETE FROM component_specifications where componentId = :componentId")
    suspend fun deleteByComponentId(componentId: Long)
}

@Dao
interface PcbComponentDao {
    @Query(
        """
        SELECT
            c.id AS id,
            c.name AS name,
            c.type AS type,
            pc.componentCount AS count,
            pc.coordinates AS coordinates
        FROM components AS c
        LEFT JOIN pcb_components AS pc ON c.id = pc.componentId
        WHERE pc.pcbId = :pcbId
    """
    )
    fun getPcbComponentInfoByPcbId(pcbId: Long): LiveData<List<PcbComponentInfo>>

    @Query("SELECT * FROM pcb_components WHERE pcbId = :pcbId")
    fun getLiveDataByPcbId(pcbId: Long): LiveData<List<PcbComponent>>

    @Query("SELECT * FROM pcb_components WHERE pcbId = :pcbId")
    suspend fun getByPcbId(pcbId: Long): List<PcbComponent>

    @Query("SELECT * FROM pcb_components WHERE pcbId = :pcbId AND componentId = :componentId")
    suspend fun getByPcbIdAndComponentId(pcbId: Long, componentId: Long): PcbComponent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pcbComponent: PcbComponent): Long

    @Update
    suspend fun update(pcbComponent: PcbComponent)

    @Delete
    suspend fun delete(pcbComponent: PcbComponent)
}

@Dao
interface ClientDao {
    @Query("""
        SELECT 
            c.id AS id,
            c.type AS type,
            c.phone AS phone,
            c.email AS email,
            CASE
                WHEN c.type = 'Физическое лицо' THEN pp.fullName
                WHEN c.type = 'Юридическое лицо' THEN le.name
                ELSE 'Неизвестное лицо'  -- Обработка неожиданного типа
            END AS displayName
        FROM clients AS c
        LEFT JOIN physical_persons AS pp ON c.id = pp.clientId
        LEFT JOIN legal_entities AS le ON c.id = le.clientId
    """)
    fun getAllClientInfo(): LiveData<List<ClientInfo>>

    @Query("SELECT * FROM clients")
    fun getAll(): LiveData<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id")
    fun getById(id: Long): LiveData<Client?>

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun getCount(): Int

    @Query("SELECT clientId FROM physical_persons WHERE fullName = :name LIMIT 1")
    suspend fun getPersonId(name: String): Long?

    @Query("SELECT clientId FROM legal_entities WHERE name = :name LIMIT 1")
    suspend fun getEntityId(name: String): Long?

    @Query("SELECT fullName FROM physical_persons WHERE clientId = :id LIMIT 1")
    suspend fun getPersonNameById(id: Long): String?

    @Query("SELECT name FROM legal_entities WHERE clientId = :id LIMIT 1")
    suspend fun getEntityNameById(id: Long): String?

    @Query("""
    SELECT 
        CASE
            WHEN c.type = 'Физическое лицо' THEN pp.fullName
            WHEN c.type = 'Юридическое лицо' THEN le.name
            ELSE 'Неизвестное лицо'
        END AS displayName
    FROM clients AS c
    LEFT JOIN physical_persons AS pp ON c.id = pp.clientId
    LEFT JOIN legal_entities AS le ON c.id = le.clientId
    """)
    suspend fun getAllClientNames(): List<String>

    @Insert
    suspend fun insert(client: Client): Long

    @Update
    suspend fun update(client: Client)

    @Delete
    suspend fun delete(client: Client)

    @Transaction
    suspend fun runInTransaction(block: suspend () -> Unit) {
        block()
    }
}

@Dao
interface PhysicalPersonDao {
    @Query("SELECT * FROM physical_persons WHERE clientId = :clientId")
    fun getByClientId(clientId: Long): LiveData<PhysicalPerson?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(physicalPerson: PhysicalPerson): Long

    @Update
    suspend fun update(physicalPerson: PhysicalPerson)

    @Delete
    suspend fun delete(physicalPerson: PhysicalPerson)

    @Query("DELETE FROM physical_persons WHERE clientId = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface LegalEntityDao {
    @Query("SELECT * FROM legal_entities WHERE clientId = :clientId")
    fun getByClientId(clientId: Long): LiveData<LegalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(legalEntity: LegalEntity): Long

    @Update
    suspend fun update(legalEntity: LegalEntity)

    @Delete
    suspend fun delete(legalEntity: LegalEntity)

    @Query("DELETE FROM legal_entities WHERE clientId = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface OrderDao {
    @Query("""
        SELECT
            o.id as id,
            CASE
                WHEN c.type = 'Физическое лицо' THEN pp.fullName
                WHEN c.type = 'Юридическое лицо' THEN le.name
                ELSE 'Неизвестное лицо'
            END AS client,
            o.status AS status,
            o.totalAmount AS totalAmount,
            o.registrationDate AS registrationDate,
            o.shippingDate AS shippingDate
        FROM orders AS o
        LEFT JOIN clients AS c ON o.clientId = c.id
        LEFT JOIN physical_persons AS pp ON c.id = pp.clientId
        LEFT JOIN legal_entities AS le ON c.id = le.clientId
    """)
    fun getAllOrderInfo(): LiveData<List<OrderInfo>>

    @Query("SELECT * FROM orders")
    suspend fun getAll(): List<Order>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getById(id: Long): Order?

    @Query("SELECT * FROM orders WHERE id = :id")
    fun getLiveDataById(id: Long): LiveData<Order?>

    @Insert
    suspend fun insert(order: Order): Long

    @Query("UPDATE orders SET totalAmount = :totalAmount WHERE id = :id")
    suspend fun updateOrderTotalAmountById(id: Long, totalAmount: Double)

    @Update
    suspend fun update(order: Order)

    @Delete
    suspend fun delete(order: Order)
}

@Dao
interface OrderItemDao {
    @Query("""
        SELECT
            oi.id AS id,
            p.name AS pcb,
            oi.quantity AS quantity,
            oi.pricePerPcb AS price
        FROM order_items AS oi
        LEFT JOIN pcb AS p ON p.id = oi.pcbId
        WHERE oi.orderId = :orderId
    """)
    fun getOrderItemInfoByOrderId(orderId: Long): LiveData<List<OrderItemInfo>>

    @Query("SELECT * FROM order_items WHERE id = :id")
    suspend fun getById(id: Long): OrderItem?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getByOrderId(orderId: Long): List<OrderItem>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId AND pcbId = :pcbId LIMIT 1")
    suspend fun getByOrderAndPcb(orderId: Long, pcbId: Long): OrderItem?

    @Insert
    suspend fun insert(orderItem: OrderItem): Long

    @Update
    suspend fun update(orderItem: OrderItem)

    @Delete
    suspend fun delete(orderItem: OrderItem)
}

@Dao
interface MovementDao {
    @Insert
    suspend fun insert(movement: Movement)

    @Query("SELECT * FROM movements ORDER BY date DESC")
    fun getAll(): LiveData<List<Movement>>
}