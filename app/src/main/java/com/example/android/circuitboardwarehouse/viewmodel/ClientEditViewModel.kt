package com.example.android.circuitboardwarehouse.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.circuitboardwarehouse.WarehouseRepository
import com.example.android.circuitboardwarehouse.database.Client
import com.example.android.circuitboardwarehouse.database.LegalEntity
import com.example.android.circuitboardwarehouse.database.PhysicalPerson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClientEditViewModel : ViewModel() {
    private val warehouseRepository = WarehouseRepository.Companion.get()

    fun getClient(clientId: Long): LiveData<Client?> {
        return warehouseRepository.getClientById(clientId)
    }

    fun getPhysicalPerson(clientId: Long): LiveData<PhysicalPerson?> {
        return warehouseRepository.getPhysicalPerson(clientId)
    }

    fun getLegalEntity(clientId: Long): LiveData<LegalEntity?> {
        return warehouseRepository.getLegalEntity(clientId)
    }

    fun addPhysicalClient(fullName: String, address: String, age: Int, phone: String, email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val clientId = warehouseRepository.addClient(
                Client(type = "Физическое лицо", phone = phone, email = email)
            )
            warehouseRepository.addPhysicalPerson(
                PhysicalPerson(clientId, fullName, address, age)
            )
        }
    }

    fun addLegalClient(name: String, inn: String, contactPerson: String, legalAddress: String,
                       actualAddress: String, phone: String, email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val clientId = warehouseRepository.addClient(
                Client(type = "Юридическое лицо", phone = phone, email = email)
            )
            warehouseRepository.addLegalEntity(
                LegalEntity(clientId, name, inn, contactPerson, legalAddress, actualAddress)
            )
        }
    }

    fun updatePhysicalClient(clientId: Long, client: Client, person: PhysicalPerson) {
        CoroutineScope(Dispatchers.IO).launch {
            warehouseRepository.updateClient(client)
            warehouseRepository.updatePhysicalPerson(person)
        }
    }

    fun updateLegalClient(clientId: Long, client: Client, entity: LegalEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            warehouseRepository.updateClient(client)
            warehouseRepository.updateLegalEntity(entity)
        }
    }

    fun updateClientType(
        clientId: Long,
        newType: String,
        phone: String,
        email: String,
        oldIsPhysical: Boolean,
        fullName: String? = null,
        address: String? = null,
        age: Int? = null,
        name: String? = null,
        inn: String? = null,
        contactPerson: String? = null,
        legalAddress: String? = null,
        actualAddress: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            warehouseRepository.runInTransaction {
                if (oldIsPhysical) {
                    warehouseRepository.deletePhysicalPersonById(clientId)
                } else {
                    warehouseRepository.deleteLegalEntityById(clientId)
                }

                if (newType == "Физическое лицо") {
                    warehouseRepository.addPhysicalPerson(
                        PhysicalPerson(clientId, fullName!!, address!!, age!!)
                    )
                } else {
                    warehouseRepository.addLegalEntity(
                        LegalEntity(
                            clientId,
                            name!!,
                            inn!!,
                            contactPerson!!,
                            legalAddress!!,
                            actualAddress!!
                        )
                    )
                }

                warehouseRepository.updateClient(Client(clientId, newType, phone, email))
            }
        }
    }
}