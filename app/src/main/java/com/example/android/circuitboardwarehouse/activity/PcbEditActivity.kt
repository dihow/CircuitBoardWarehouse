
package com.example.android.circuitboardwarehouse.activity

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.android.circuitboardwarehouse.R
import com.example.android.circuitboardwarehouse.database.Pcb
import com.example.android.circuitboardwarehouse.viewmodel.PcbEditViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private const val REQUEST_CODE_PERMISSIONS = 123
private const val REQUEST_IMAGE_CAPTURE = 1
private const val REQUEST_IMAGE_PICK = 2
private const val REQUEST_CODE_CREATE_FILE = 3

class PcbEditActivity : AppCompatActivity() {
    private lateinit var viewModel: PcbEditViewModel
    private var pcbId: Long? = null
    private var isEditMode = false
    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pcb_edit)

        viewModel = ViewModelProvider(this)[PcbEditViewModel::class.java]
        pcbId = intent.getLongExtra(EXTRA_PCB_ID, -1).takeIf { it != -1L }
        isEditMode = pcbId != null

        requestPermissionsIfNecessary()

        setupSaveButton()
        setupComponentsButton()
        setupDatePicker()
        setupImageHandling()
        setupAddStockButton()

        if (isEditMode) {
            loadPcbData()
        }
    }

    private fun requestPermissionsIfNecessary() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(),
                REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("Permissions", "All permissions granted")
            } else {
                Toast.makeText(this, "Не выданы необходимые разрешения", Toast.LENGTH_SHORT).show()
                findViewById<Button>(R.id.take_photo_button).isEnabled = false
            }
        }
    }

    private fun setupSaveButton() {
        findViewById<Button>(R.id.save_button).setOnClickListener {
            savePcb()
        }
    }

    private fun setupAddStockButton() {
        findViewById<FloatingActionButton>(R.id.add_stock_button).setOnClickListener {
            showAddStockDialog()
        }
    }

    private fun setupComponentsButton() {
        findViewById<Button>(R.id.components_button).setOnClickListener {
            pcbId?.let { id ->
                val intent = PcbComponentsActivity.newIntent(this, id)
                startActivity(intent)
            } ?: run {
                Toast.makeText(this, "Сначала создайте плату", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePcb() {
        val name = findViewById<TextInputEditText>(R.id.name_edit_text).text.toString()
        val serialNumber = findViewById<TextInputEditText>(R.id.serial_number_edit_text).text.toString()
        val batch = findViewById<TextInputEditText>(R.id.batch_edit_text).text.toString()
        val description = findViewById<TextInputEditText>(R.id.description_edit_text).text.toString()
        val price = findViewById<TextInputEditText>(R.id.price_edit_text).text.toString().toDoubleOrNull()
        val totalStock = findViewById<TextInputEditText>(R.id.total_stock_edit_text).text.toString().toIntOrNull()
        val orderedQuantity = findViewById<TextInputEditText>(R.id.ordered_quantity_edit_text).text.toString().toIntOrNull()

        val dateText = findViewById<TextInputEditText>(R.id.manufacturing_date_edit_text).text.toString()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val manufacturingDate = try {
            dateFormat.parse(dateText)?.time ?: Date().time
        } catch (e: Exception) {
            Date().time
        }

        val length = findViewById<TextInputEditText>(R.id.length_edit_text).text.toString().toDoubleOrNull()
        val width = findViewById<TextInputEditText>(R.id.width_edit_text).text.toString().toDoubleOrNull()
        val layerCount = findViewById<TextInputEditText>(R.id.layer_count_edit_text).text.toString().toIntOrNull()
        val comment = findViewById<TextInputEditText>(R.id.comment_edit_text).text.toString()

        if (name.isEmpty() || price == null || totalStock == null || orderedQuantity == null ||
            length == null || width == null || layerCount == null || serialNumber.isEmpty() ||
            batch.isEmpty()) {
            Toast.makeText(this, "Заполните все необходимые поля", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            pcbId?.let { id ->
                val pcbComponents = viewModel.getPcbComponentsByPcbId(id)
                val shortageComponents = mutableListOf<String>()

                for (pcbComponent in pcbComponents) {
                    val component = viewModel.getComponentById(pcbComponent.componentId).value
                    val requiredQuantity = totalStock * pcbComponent.componentCount
                    if (component != null && component.stockQuantity < requiredQuantity) {
                        shortageComponents.add("${component.name} (требуется: $requiredQuantity, доступно: ${component.stockQuantity})")
                    }
                }

                if (shortageComponents.isNotEmpty()) {
                    val message = "Недостаточно компонентов:\n" + shortageComponents.joinToString("\n")
                    Toast.makeText(this@PcbEditActivity, message, Toast.LENGTH_LONG).show()
                    return@launch
                }

                try {
                    viewModel.updatePcbAndComponents(
                        pcbId = id,
                        name = name,
                        serialNumber = serialNumber,
                        batch = batch,
                        description = description.ifEmpty { null },
                        price = price,
                        boardsCount = totalStock,
                        orderedQuantity = orderedQuantity,
                        manufacturingDate = manufacturingDate,
                        length = length,
                        width = width,
                        layerCount = layerCount,
                        comment = comment,
                        imagePath = currentPhotoPath
                    )

                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@PcbEditActivity, e.message,
                        Toast.LENGTH_LONG).show()
                    Log.e("PcbEditActivity", "Error updating PCB and components", e)
                }

            } ?: run {
                val pcb = Pcb(
                    id = 0,
                    name = name,
                    serialNumber = serialNumber,
                    batch = batch,
                    description = description.ifEmpty { null },
                    price = price,
                    totalStock = totalStock,
                    orderedQuantity = orderedQuantity,
                    manufacturingDate = manufacturingDate,
                    length = length,
                    width = width,
                    layerCount = layerCount,
                    comment = comment,
                    imagePath = currentPhotoPath
                )

                viewModel.addPcb(pcb)
                pcbId = viewModel.pcbId.value
                finish()
            }
        }
    }

    private fun loadPcbData() {
        val pcb = viewModel.getPcb(pcbId!!)
        pcb.observe(this@PcbEditActivity) {
            findViewById<TextInputEditText>(R.id.name_edit_text).setText(it?.name)
            findViewById<TextInputEditText>(R.id.serial_number_edit_text).setText(it?.serialNumber)
            findViewById<TextInputEditText>(R.id.batch_edit_text).setText(it?.batch)
            findViewById<TextInputEditText>(R.id.description_edit_text).setText(it?.description)
            findViewById<TextInputEditText>(R.id.price_edit_text).setText(it?.price.toString())
            findViewById<TextInputEditText>(R.id.total_stock_edit_text).setText(it?.totalStock.toString())
            findViewById<TextInputEditText>(R.id.ordered_quantity_edit_text).setText(it?.orderedQuantity.toString())
            findViewById<TextInputEditText>(R.id.manufacturing_date_edit_text).setText(
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    .format(Date(it?.manufacturingDate ?: Date().time))
            )
            findViewById<TextInputEditText>(R.id.length_edit_text).setText(it?.length.toString())
            findViewById<TextInputEditText>(R.id.width_edit_text).setText(it?.width.toString())
            findViewById<TextInputEditText>(R.id.layer_count_edit_text).setText(it?.layerCount.toString())

            it?.imagePath?.let { path ->
                try {
                    val imageUri = Uri.parse(path)
                    loadImage(imageUri, findViewById<ImageView>(R.id.pcb_image_view))
                } catch (e: Exception) {
                    Log.e("PcbEditActivity", "Error loading image from path: $path", e)
                    findViewById<ImageView>(R.id.pcb_image_view).setImageResource(R.drawable.circuit_placeholder)
                }
            } ?: run {
                findViewById<ImageView>(R.id.pcb_image_view).setImageResource(R.drawable.circuit_placeholder)
            }

        }
    }

    private fun setupDatePicker() {
        val dateEditText = findViewById<TextInputEditText>(R.id.manufacturing_date_edit_text)

        dateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(year, month, day)
                    }
                    dateEditText.setText(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        .format(selectedDate.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupImageHandling() {
        findViewById<Button>(R.id.take_photo_button).setOnClickListener {
            dispatchTakePictureIntent()
        }

        findViewById<Button>(R.id.choose_photo_button).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }

                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )

        currentPhotoPath = imageFile.absolutePath

        return imageFile
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val imageView = findViewById<ImageView>(R.id.pcb_image_view)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    if (currentPhotoPath != null) {
                        val file = File(currentPhotoPath!!)
                        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                        loadImage(uri, imageView)
                        currentPhotoPath = uri.toString()
                    }
                }
                REQUEST_IMAGE_PICK -> {
                    data?.data?.let { uri ->
                        currentPhotoPath = uri.toString()
                        loadImage(uri, imageView)
                    }
                }
                REQUEST_CODE_CREATE_FILE -> {
                    data?.data?.let { uri ->
                        saveImageToFile(uri, imageView)
                    }
                }
            }
        }
    }

    private fun loadImage(uri: Uri, imageView: ImageView) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 4
                }
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                } else {
                    Log.e("loadImage", "Bitmap decoding failed")
                    imageView.setImageResource(R.drawable.circuit_placeholder)
                }
            } else {
                Log.e("loadImage", "InputStream is null")
                imageView.setImageResource(R.drawable.circuit_placeholder)
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Не удалось загрузить изображение: ${e.message}",
                Toast.LENGTH_SHORT).show()
            Log.e("loadImage", "Error loading image", e)
            imageView.setImageResource(R.drawable.circuit_placeholder)
        }
    }

    private fun saveImageToFile(uri: Uri, imageView: ImageView) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                currentPhotoPath = uri.toString()
                loadImage(uri, imageView)
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Не удалось загрузить изображение: ${e.message}",
                Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun showAddStockDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_stock, null)
        val quantityInput = dialogView.findViewById<EditText>(R.id.quantity_edit_text)

        AlertDialog.Builder(this)
            .setTitle("Добавить платы")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val quantity = quantityInput.text.toString().toIntOrNull() ?: 0
                if (quantity > 0) {
                    addToStock(quantity)
                } else {
                    Toast.makeText(this, "Введите корректное количество", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun addToStock(quantity: Int) {
        val currentStockText = findViewById<TextInputEditText>(R.id.total_stock_edit_text).text.toString()
        val currentStock = currentStockText.toIntOrNull() ?: 0
        val newStock = currentStock + quantity
        findViewById<TextInputEditText>(R.id.total_stock_edit_text).setText(newStock.toString())
    }

    companion object {
        const val EXTRA_PCB_ID = "pcb_id"

        fun newIntent(packageContext: Context, pcbId: Long? = null) =
            Intent(packageContext, PcbEditActivity::class.java).apply {
                pcbId?.let { putExtra(EXTRA_PCB_ID, it) }
            }
    }
}