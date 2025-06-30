package com.example.android.circuitboardwarehouse

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import com.example.android.circuitboardwarehouse.database.Employee
import com.example.android.circuitboardwarehouse.database.EmployeeDao
import com.example.android.circuitboardwarehouse.database.WarehouseDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class EmployeeDaoTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var employeeDao: EmployeeDao
    private lateinit var db: WarehouseDatabase

    @Before
    fun createDb() {
        val context = mock(Context::class.java)

        db = Room.inMemoryDatabaseBuilder(
            context, WarehouseDatabase::class.java
        ).allowMainThreadQueries().build()
        employeeDao = db.employeeDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetEmployee() = runBlocking {
        val employee = Employee(fullName = "Иванов Иван Иванович", address = "Московская, 13",
            phone = "555-1212", email = "ivanov@yandex.ru", position = "Монтажник",
            salary = 60000.0, login = "ivanov", passwordHash = "123456", salt = "123")
        val employeeId = employeeDao.insert(employee)
        val retrievedEmployee = employeeDao.getById(employeeId)

        assertNotNull(retrievedEmployee)
        assertEquals("Иванов Иван Иванович", retrievedEmployee?.fullName)
    }

    @Test
    fun getAllEmployees() = runBlocking {
        val employee1 = Employee(fullName = "Иванов Иван Иванович", address = "Московская, 13",
            phone = "555-1212", email = "ivanov@yandex.ru", position = "Монтажник",
            salary = 65000.0, login = "ivanov", passwordHash = "123456", salt = "123")
        val employee2 = Employee(fullName = "Петров Сергей Миронович", address = "Серая, 16",
            phone = "555-3212", email = "petrov@yandex.ru", position = "Уборщик",
            salary = 50000.0, login = "petrov", passwordHash = "qwerty", salt = "123")

        employeeDao.insert(employee1)
        employeeDao.insert(employee2)

        val allEmployees = employeeDao.getAll()
        assertEquals(2, allEmployees.size)
    }
}
