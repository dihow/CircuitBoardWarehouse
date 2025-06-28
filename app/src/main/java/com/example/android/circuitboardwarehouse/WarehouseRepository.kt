package com.example.android.circuitboardwarehouse

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.room.Room
import androidx.room.withTransaction
import com.example.android.circuitboardwarehouse.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.abs

private const val DATABASE_NAME = "warehouse-database"

//private const val PREFS_NAME = "app_prefs"
//private const val FIRST_RUN_KEY = "first_run"

class WarehouseRepository private constructor(context: Context) {
//    init {
//        context.deleteDatabase("warehouse-database")
//    }

    private val database: WarehouseDatabase = Room.databaseBuilder(
        context.applicationContext,
        WarehouseDatabase::class.java,
        DATABASE_NAME
    ).build()

    // DAOs
    private val employeeDao = database.employeeDao()
    private val pcbDao = database.pcbDao()
    private val componentDao = database.componentDao()
    private val componentSpecificationDao = database.componentSpecificationDao()
    private val pcbComponentDao = database.pcbComponentDao()
    private val clientDao = database.clientDao()
    private val physicalPersonDao = database.physicalPersonDao()
    private val legalEntityDao = database.legalEntityDao()
    private val orderDao = database.orderDao()
    private val orderItemDao = database.orderItemDao()
    private val movementDao = database.movementDao()

    suspend fun <R> runInTransaction(block: suspend () -> R): R {
        return database.withTransaction {
            block()
        }
    }

    // Employee DAO functions
    suspend fun getEmployees() = employeeDao.getAll()

    suspend fun getEmployeeById(id: Long) = employeeDao.getById(id)

    suspend fun addEmployee(employee: Employee) = employeeDao.insert(employee)

    suspend fun updateEmployee(employee: Employee) = employeeDao.update(employee)

    suspend fun deleteEmployee(employee: Employee) = employeeDao.delete(employee)

    suspend fun getEmployeeByLogin(login: String): Employee? {
        return employeeDao.getByLogin(login)
    }

    suspend fun getEmployeeSalt(login: String) = employeeDao.getEmployeeSalt(login)

    suspend fun updatePasswordById(id: Long, login: String, password: String, salt: String) =
        employeeDao.updateLoginAndPasswordById(id, login, password, salt)

    private suspend fun createEmployee(employee: Employee, password: String): Long {
        val salt = generateSalt()
        val hashedPassword = hashPasswordWithSalt(password, salt)
        val employeeWithHashedPassword = employee.copy(passwordHash = hashedPassword)
        return employeeDao.insert(employeeWithHashedPassword)
    }

    fun hashPasswordWithSalt(password: String, salt: String): String {
        val saltedPassword = password + salt
        return MessageDigest
            .getInstance("SHA-256")
            .digest(saltedPassword.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.DEFAULT)
    }

    // PCB DAO functions
    fun getPcbs() = pcbDao.getAll()

    suspend fun getPcbsSuspend() = pcbDao.getAllSuspend()

    suspend fun getPcbById(id: Long) = pcbDao.getById(id)

    suspend fun getAllPcbsName() = pcbDao.getAllPcbsName()

    suspend fun addPcb(pcb: Pcb) = pcbDao.insert(pcb)

    suspend fun updateOrderedQuantityByPcbId(pcbId: Long, orderedQuantity: Int) =
        pcbDao.updateOrderedQuantityByPcbId(pcbId, orderedQuantity)

    suspend fun updatePcb(pcb: Pcb) {
        val oldPcb = getPcbById(pcb.id) ?: return
        val oldStock = oldPcb.totalStock

        runInTransaction {
            pcbDao.update(pcb)

            if (oldStock != pcb.totalStock) {
                handlePcbStockChange(
                    pcbId = pcb.id,
                    oldStock = oldStock,
                    newStock = pcb.totalStock,
                    pcbName = pcb.name
                )
            }
        }
    }

    suspend fun deletePcb(pcb: Pcb) = pcbDao.delete(pcb)

    // Component DAO functions
    fun getComponents() = componentDao.getAll()

    fun getComponentLiveDataById(id: Long) = componentDao.getLiveDataById(id)

    suspend fun getComponentById(id: Long) = componentDao.getById(id)

    suspend fun addComponent(component: Component) = componentDao.insert(component)

    suspend fun updateComponent(component: Component) = componentDao.update(component)

    suspend fun updateComponentStock(componentId: Long, newStock: Int) {
        val component = getComponentById(componentId) ?: return
        val oldStock = component.stockQuantity

        runInTransaction {
            componentDao.updateStock(componentId, newStock)

            if (oldStock != newStock) {
                handleComponentStockChange(
                    componentId = componentId,
                    oldStock = oldStock,
                    newStock = newStock,
                    componentName = component.name
                )
            }
        }
    }

    suspend fun deleteComponent(component: Component) = componentDao.delete(component)

    // ComponentSpecification DAO functions
    fun getComponentSpecificationsByComponentId(componentId: Long) =
        componentSpecificationDao.getByComponentId(componentId)

    suspend fun addComponentSpecification(specification: ComponentSpecification) =
        componentSpecificationDao.insert(specification)

    suspend fun updateComponentSpecification(specification: ComponentSpecification) =
        componentSpecificationDao.update(specification)

    suspend fun deleteComponentSpecification(specification: ComponentSpecification) =
        componentSpecificationDao.delete(specification)

    suspend fun deleteComponentSpecificationsByComponentId(componentId: Long) =
        componentSpecificationDao.deleteByComponentId(componentId)

    // PcbComponent DAO functions
    fun getPcbComponentInfoByPcbId(pcbId: Long) = pcbComponentDao.getPcbComponentInfoByPcbId(pcbId)

    fun getPcbComponentsLiveDataByPcbId(pcbId: Long) = pcbComponentDao.getLiveDataByPcbId(pcbId)

    suspend fun getPcbComponentsByPcbId(pcbId: Long) = pcbComponentDao.getByPcbId(pcbId)

    suspend fun getPcbComponentByPcbIdAndComponentId(pcbId: Long, componentId: Long) =
        pcbComponentDao.getByPcbIdAndComponentId(pcbId, componentId)

    suspend fun addPcbComponent(pcbComponent: PcbComponent) =
        pcbComponentDao.insert(pcbComponent)

    suspend fun updatePcbComponent(pcbComponent: PcbComponent) =
        pcbComponentDao.update(pcbComponent)

    suspend fun deletePcbComponent(pcbComponent: PcbComponent) =
        pcbComponentDao.delete(pcbComponent)

    // Client DAO functions
    fun getAllClientInfo() = clientDao.getAllClientInfo()

    suspend fun getClients() = clientDao.getAll()

    fun getClientById(id: Long) = clientDao.getById(id)

    suspend fun getClientIdByName(name: String): Long? {
        val physicalPersonId = clientDao.getPersonId(name)
        if (physicalPersonId != null) {
            return physicalPersonId
        }

        val legalEntityId = clientDao.getEntityId(name)
        if (legalEntityId != null) {
            return legalEntityId
        }

        return null
    }

    suspend fun getClientNameById(id: Long): String? {
        val physicalPersonName = clientDao.getPersonNameById(id)
        if (physicalPersonName != null) {
            return physicalPersonName
        }

        val legalEntityName = clientDao.getEntityNameById(id)
        if (legalEntityName != null) {
            return legalEntityName
        }

        return null
    }

    suspend fun getAllClientNames(): List<String> {
        return clientDao.getAllClientNames()
    }

    suspend fun addClient(client: Client) = clientDao.insert(client)

    suspend fun updateClient(client: Client) = clientDao.update(client)

    suspend fun deleteClient(client: Client) = clientDao.delete(client)

    suspend fun getClientCount(): Int = clientDao.getCount()

    // PhysicalPerson DAO functions
    fun getPhysicalPerson(clientId: Long) = physicalPersonDao.getByClientId(clientId)

    suspend fun addPhysicalPerson(person: PhysicalPerson) = physicalPersonDao.insert(person)

    suspend fun updatePhysicalPerson(person: PhysicalPerson) = physicalPersonDao.update(person)

    suspend fun deletePhysicalPerson(person: PhysicalPerson) = physicalPersonDao.delete(person)

    suspend fun deletePhysicalPersonById(id: Long) = physicalPersonDao.deleteById(id)

    // LegalEntity DAO functions
    fun getLegalEntity(clientId: Long) = legalEntityDao.getByClientId(clientId)

    suspend fun addLegalEntity(legalEntity: LegalEntity) = legalEntityDao.insert(legalEntity)

    suspend fun updateLegalEntity(legalEntity: LegalEntity) = legalEntityDao.update(legalEntity)

    suspend fun deleteLegalEntity(legalEntity: LegalEntity) = legalEntityDao.delete(legalEntity)

    suspend fun deleteLegalEntityById(id: Long) = legalEntityDao.deleteById(id)

    // Order DAO functions
    suspend fun getOrderById(id: Long) = orderDao.getById(id)

    fun getAllOrderInfo() = orderDao.getAllOrderInfo()

    suspend fun getOrders() = orderDao.getAll()

    fun getOrderLiveDataById(id: Long) = orderDao.getLiveDataById(id)

    suspend fun addOrder(order: Order) = orderDao.insert(order)

    suspend fun updateOrderTotalAmountById(id: Long, totalAmount: Double) =
        orderDao.updateOrderTotalAmountById(id, totalAmount)

    suspend fun updateOrder(order: Order) = orderDao.update(order)

    suspend fun deleteOrder(order: Order) = orderDao.delete(order)

    // OrderItem DAO functions
    fun getOrderItemInfoByOrderId(orderId: Long) = orderItemDao.getOrderItemInfoByOrderId(orderId)

    suspend fun getOrderItemById(id: Long) = orderItemDao.getById(id)

    suspend fun getOrderItemsByOrderId(orderId: Long) = orderItemDao.getByOrderId(orderId)

    suspend fun getOrderItemByOrderAndPcb(orderId: Long, pcbId: Long) =
        orderItemDao.getByOrderAndPcb(orderId, pcbId)

    suspend fun addOrderItem(orderItem: OrderItem) = orderItemDao.insert(orderItem)

    suspend fun updateOrderItem(orderItem: OrderItem) = orderItemDao.update(orderItem)

    suspend fun deleteOrderItem(orderItem: OrderItem) = orderItemDao.delete(orderItem)

    // Movement DAO functions
    suspend fun addMovement(movement: Movement) = movementDao.insert(movement)

    fun getAllMovements() = movementDao.getAll()

    suspend fun handleComponentStockChange(
        componentId: Long,
        oldStock: Int,
        newStock: Int,
        componentName: String
    ) {
        val difference = newStock - oldStock
        if (difference == 0) return

        val movementType = if (difference > 0) "Приход" else "Расход"
        val description = if (difference > 0)
            "Поступление на склад ${abs(difference)} компонентов \"$componentName\""
        else
            "Списание со склада ${abs(difference)} компонентов \"$componentName\""

        val movement = Movement(
            movementType = movementType,
            productType = "Компонент",
            description = description,
            value = abs(difference),
            date = System.currentTimeMillis()
        )

        addMovement(movement)
    }

    suspend fun handlePcbStockChange(
        pcbId: Long,
        oldStock: Int,
        newStock: Int,
        pcbName: String
    ) {
        val difference = newStock - oldStock
        if (difference == 0) return

        val movementType = if (difference > 0) "Приход" else "Расход"
        val description = if (difference > 0)
            "Поступление на склад ${abs(difference)} плат \"$pcbName\""
        else
            "Списание со склада ${abs(difference)} плат \"$pcbName\""

        val movement = Movement(
            movementType = movementType,
            productType = "Плата",
            description = description,
            value = abs(difference),
            date = System.currentTimeMillis()
        )

        addMovement(movement)
    }

//    init {
//        CoroutineScope(Dispatchers.IO).launch {
//            populateDatabaseWithTestData()
//        }
//    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            if (employeeDao.getCount() == 0) {
                initEmployees()
            }
        }
    }

    suspend fun populateDatabaseWithTestData() {
        withContext(Dispatchers.IO) {
            // 1. Employees
            val employees = List(3) { i ->
                Employee(
                    fullName = "Employee ${i + 1}",
                    address = "Address ${i + 1}",
                    phone = "123-456-789${i + 1}",
                    email = "employee${i + 1}@example.com",
                    position = "Position ${i + 1}",
                    salary = 50000.0 + (i + 1) * 1000,
                    login = "login${i + 1}",
                    passwordHash = "hash${i + 1}",
                    salt = "salt${i + 1}"
                )
            }.map { employee ->
                val employeeId = employeeDao.insert(employee)
                employee.copy(id = employeeId)
            }
            Log.d("TestData", "Inserted ${employees.size} employees")

            // 2. PCBs
            val pcbs = List(3) { i ->
                Pcb(
                    name = "Плата ${i + 1}",
                    serialNumber = "Серийный номер ${i + 1}",
                    batch = "Партия ${i + 1}",
                    description = "Описание ${i + 1}",
                    price = 10.0 + (i + 1) * 2,
                    totalStock = 100 + (i + 1) * 10,
                    orderedQuantity = 10 + (i + 1),
                    manufacturingDate = System.currentTimeMillis() - ((i + 1) * 86400000),
                    length = 10.0 + (i + 1),
                    width = 5.0 + (i + 1),
                    layerCount = (i + 1) + 2,
                    comment = null
                )
            }.map { pcb ->
                val pcbId = pcbDao.insert(pcb)
                pcb.copy(id = pcbId)
            }
            Log.d("TestData", "Inserted ${pcbs.size} PCBs")

            // 3. Components
            val components = mutableListOf<Component>()
            val resistorCount = 1
            val capacitorCount = 1
            val diodeCount = 1

            repeat(resistorCount) { i ->
                val component = Component(
                    name = "RES-SN-${i + 1}",
                    manufacturer = "Производитель резисторов ${i + 1}",
                    price = 1.0 + (i + 1),
                    type = "Резистор",
                    stockQuantity = 100 + (i + 1) * 10
                )
                val componentId = componentDao.insert(component)
                components.add(component.copy(id = componentId))
            }
            repeat(capacitorCount) { i ->
                val component = Component(
                    name = "CAP-SN-${i + 1}",
                    manufacturer = "Производитель конденсаторов ${i + 1}",
                    price = 2.0 + (i + 1),
                    type = "Конденсатор",
                    stockQuantity = 150 + (i + 1) * 15
                )
                val componentId = componentDao.insert(component)
                components.add(component.copy(id = componentId))
            }
            repeat(diodeCount) { i ->
                val component = Component(
                    name = "DIO-SN-${i + 1}",
                    manufacturer = "Производитель диодов ${i + 1}",
                    price = 3.0 + (i + 1),
                    type = "Диод",
                    stockQuantity = 200 + (i + 1) * 20
                )
                val componentId = componentDao.insert(component)
                components.add(component.copy(id = componentId))
            }

            Log.d("TestData", "Inserted ${components.size} components")

            // 4. Component Specifications
            val specs = components.flatMap { component ->
                when (component.type) {
                    "Резистор" -> {
                        List(2) { i ->
                            ComponentSpecification(
                                componentId = component.id,
                                specification = "Сопротивление",
                                specificationValue = "${(i + 1) * 100} Ом"
                            ).also { componentSpecificationDao.insert(it) }
                        } +
                                List(1) {
                                    ComponentSpecification(
                                        componentId = component.id,
                                        specification = "Допуск",
                                        specificationValue = "5%"
                                    ).also { componentSpecificationDao.insert(it) }
                                }
                    }
                    "Конденсатор" -> {
                        List(2) { i ->
                            ComponentSpecification(
                                componentId = component.id,
                                specification = "Ёмкость",
                                specificationValue = "${(i + 1) * 10} Ф"
                            ).also { componentSpecificationDao.insert(it) }
                        } +
                                List(1) {
                                    ComponentSpecification(
                                        componentId = component.id,
                                        specification = "Напряжение",
                                        specificationValue = "50 В"
                                    ).also { componentSpecificationDao.insert(it) }
                                }
                    }
                    "Диод" -> {
                        List(2) { i ->
                            ComponentSpecification(
                                componentId = component.id,
                                specification = "Падение напряжения",
                                specificationValue = "${(i + 1) * 0.7} В"
                            ).also { componentSpecificationDao.insert(it) }
                        } +
                                List(1) {
                                    ComponentSpecification(
                                        componentId = component.id,
                                        specification = "Прямой ток",
                                        specificationValue = "1 А"
                                    ).also { componentSpecificationDao.insert(it) }
                                }
                    }
                    else -> emptyList() // Handle unknown component types, if any
                }
            }
            Log.d("TestData", "Inserted ${specs.size} component specs")

            // 5. PcbComponents
            val pcbComponents = pcbs.flatMap { pcb ->
                components.map { component ->
                    PcbComponent(
                        pcbId = pcb.id,
                        componentId = component.id,
                        componentCount = 2 + (pcb.id + component.id).toInt(),
                        coordinates = "C1, C2, C3"
                    ).also { pcbComponentDao.insert(it) }
                }
            }
            Log.d("TestData", "Inserted ${pcbComponents.size} PCB components")

            // 6. Clients
            val clientTypes = listOf("Физическое лицо", "Юридическое лицо")
            val clients = List(3) { i ->
                Client(
                    type = clientTypes[i % clientTypes.size],
                    phone = "555-123-${i + 1}",
                    email = "client${i + 1}@example.com"
                )
            }.map { client ->
                val clientId = clientDao.insert(client)
                client.copy(id = clientId)
            }
            Log.d("TestData", "Inserted ${clients.size} clients")

            // 7. Physical Persons
            clients.filter { it.type == "Физическое лицо" }.forEach { client ->
                PhysicalPerson(
                    clientId = client.id,
                    fullName = "Клиент ${client.id}",
                    address = "Адрес клиента ${client.id}",
                    age = 20 + client.id.toInt()
                ).also { physicalPersonDao.insert(it) }
            }

            // 8. Legal Entities
            clients.filter { it.type == "Юридическое лицо" }.forEach { client ->
                LegalEntity(
                    clientId = client.id,
                    name = "Компания ${client.id}",
                    inn = "123456789${client.id}",
                    contactPerson = "Контактное лицо ${client.id}",
                    legalAddress = "Юридический адрес ${client.id}",
                    actualAddress = "Фактический адрес ${client.id}"
                ).also { legalEntityDao.insert(it) }
            }
            Log.d("TestData", "Inserted physical/legal entities")

            // 9. Orders
            val orders = clients.map { client ->
                Order(
                    clientId = client.id,
                    registrationDate = System.currentTimeMillis(),
                    status = "Оплачен",
                    totalAmount = 100.0 + client.id * 10,
                    shippingDate = System.currentTimeMillis() + 86400000,
                    shippingCompany = "Shipping Co. ${client.id}"
                )
            }.map { order ->
                val orderId = orderDao.insert(order)
                order.copy(id = orderId)
            }
            Log.d("TestData", "Inserted ${orders.size} orders")

            // 10. Order Items
            val orderItems = orders.flatMap { order ->
                pcbs.map { pcb ->
                    OrderItem(
                        orderId = order.id,
                        pcbId = pcb.id,
                        quantity = 1 + (order.id + pcb.id).toInt(),
                        pricePerPcb = pcb.price
                    ).also { orderItemDao.insert(it) }
                }
            }
            Log.d("TestData", "Inserted ${orderItems.size} order items")

            updateOrderedQuantityByPcbId(1, 12)
            updateOrderedQuantityByPcbId(2, 15)
            updateOrderedQuantityByPcbId(3, 18)

            updateOrderTotalAmountById(1, 172.0)
            updateOrderTotalAmountById(2, 214.0)
            updateOrderTotalAmountById(3, 256.0)

            var salt1 = generateSalt()
            val hash1 = hashPasswordWithSalt("123456", salt1)
            updatePasswordById(1, "user", hash1, salt1)

            var salt2 = generateSalt()
            val hash2 = hashPasswordWithSalt("adm", salt2)
            updatePasswordById(2, "admin", hash2, salt2)

            var salt3 = generateSalt()
            val hash3 = hashPasswordWithSalt("qwerty", salt3)
            updatePasswordById(3, "qwerty", hash3, salt3)
        }
    }

    suspend fun initEmployees() {
        withContext(Dispatchers.IO) {
            val emp1 = employeeDao.insert(
                Employee(
                    fullName = "Иванов Иван Иванович",
                    address = "Новокубанский, 11",
                    phone = "79197326272",
                    email = "ivanov@yandex.ru",
                    position = "Кладовщик",
                    salary = 40000.0,
                    login = "",
                    passwordHash = "",
                    salt = ""
                )
            )

            val emp2 = employeeDao.insert(
                Employee(
                    fullName = "Борисов Анатолий Сергеевич",
                    address = "40-летия Победы, 71",
                    phone = "79193335216",
                    email = "borisov@yandex.ru",
                    position = "Системный администратор",
                    salary = 100000.0,
                    login = "",
                    passwordHash = "",
                    salt = ""
                )
            )

            val emp3 = employeeDao.insert(
                Employee(
                    fullName = "Валерьев Николай Игоревич",
                    address = "Детская, 25",
                    phone = "79197483472",
                    email = "valeryev@yandex.ru",
                    position = "Монтажник",
                    salary = 90000.0,
                    login = "",
                    passwordHash = "",
                    salt = ""
                )
            )

            var salt1 = generateSalt()
            val hash1 = hashPasswordWithSalt("123456", salt1)
            updatePasswordById(emp1, "user", hash1, salt1)

            var salt2 = generateSalt()
            val hash2 = hashPasswordWithSalt("adm", salt2)
            updatePasswordById(emp2, "admin", hash2, salt2)

            var salt3 = generateSalt()
            val hash3 = hashPasswordWithSalt("qwerty", salt3)
            updatePasswordById(emp3, "qwerty", hash3, salt3)
        }
    }

    companion object {
        private var INSTANCE: WarehouseRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = WarehouseRepository(context)
            }
        }

        fun get(): WarehouseRepository {
            return INSTANCE
                ?: throw IllegalStateException("WarehouseRepository must be initialized")
        }
    }
}