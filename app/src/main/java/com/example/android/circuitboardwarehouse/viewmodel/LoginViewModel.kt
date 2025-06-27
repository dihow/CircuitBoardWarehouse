package com.example.android.circuitboardwarehouse.viewmodel

import androidx.lifecycle.ViewModel
import com.example.android.circuitboardwarehouse.WarehouseRepository

class LoginViewModel : ViewModel() {
    private val warehouseRepository = WarehouseRepository.get()

    suspend fun getEmployeeByLogin(login: String) = warehouseRepository.getEmployeeByLogin(login)

    fun hashPasswordWithSalt(password: String, storedSalt: String) =
        warehouseRepository.hashPasswordWithSalt(password, storedSalt)
}