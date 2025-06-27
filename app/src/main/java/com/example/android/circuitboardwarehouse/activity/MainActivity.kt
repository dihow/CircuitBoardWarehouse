package com.example.android.circuitboardwarehouse.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.android.circuitboardwarehouse.R
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private lateinit var circuitsListButton: Button
    private lateinit var componentsListButton: Button
    private lateinit var clientsListButton: Button
    private lateinit var ordersListButton: Button
    private lateinit var movementsButton: Button
    private lateinit var exitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        circuitsListButton = findViewById(R.id.circuits_list_button)
        componentsListButton = findViewById(R.id.components_list_button)
        clientsListButton = findViewById(R.id.clients_list_button)
        ordersListButton = findViewById(R.id.orders_list_button)
        movementsButton = findViewById(R.id.movements_button)
        exitButton = findViewById(R.id.exit_button)

        circuitsListButton.setOnClickListener {
            val intent = PcbsListActivity.newIntent(this@MainActivity)
            startActivity(intent)
        }

        componentsListButton.setOnClickListener {
            val intent = ComponentsListActivity.newIntent(this@MainActivity)
            startActivity(intent)
        }

        clientsListButton.setOnClickListener {
            val intent = Intent(this@MainActivity, ClientsListActivity::class.java)
            startActivity(intent)
        }

        ordersListButton.setOnClickListener {
            val intent = OrdersListActivity.newIntent(this@MainActivity)
            startActivity(intent)
        }

        movementsButton.setOnClickListener {
            val intent = MovementsActivity.newIntent(this@MainActivity)
            startActivity(intent)
        }

        exitButton.setOnClickListener {
            exitProcess(0)
        }
    }
}