package com.example.driveuploader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var pickButton: Button
    private lateinit var sendButton: Button
    private lateinit var deleteButton: Button

    private var selectedUris: List<Uri> = emptyList()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNullOrEmpty()) {
            appendStatus("هیچ فایلی انتخاب نشد.")
            return@registerForActivityResult
        }
        selectedUris = uris
        sendButton.isEnabled = true
        deleteButton.isEnabled = false
        appendStatus("${uris.size} فایل انتخاب شد. حالا دکمه «ارسال به Drive» را بزن.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        pickButton = findViewById(R.id.pickButton)
        sendButton = findViewById(R.id.sendButton)
        deleteButton = findViewById(R.id.deleteButton)

        sendButton.isEnabled = false
        deleteButton.isEnabled = false

        pickButton.setOnClickListener {
            filePickerLauncher.launch(arrayOf("image/*", "video/*"))
        }

        sendButton.setOnClickListener {
            sendToDrive()
        }

        deleteButton.setOnClickListener {
            deleteSelectedFiles()
        }
    }

    private fun sendToDrive() {
        if (selectedUris.isEmpty()) return

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(selectedUris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "بارگذاری با..."))
        appendStatus("پنجره انتخاب باز شد. Drive رو انتخاب کن، پوشه و حساب رو خودت مشخص کن و بارگذاری کن.")

        deleteButton.isEnabled = true
    }

    private fun deleteSelectedFiles() {
        if (selectedUris.isEmpty()) return

        var successCount = 0
        var failCount = 0

        for (uri in selectedUris) {
            try {
                val deleted = DocumentsContract.deleteDocument(contentResolver, uri)
                if (deleted) successCount++ else failCount++
            } catch (e: Exception) {
                failCount++
            }
        }

        appendStatus("پاک شد: $successCount فایل موفق، $failCount فایل ناموفق.")
        if (failCount > 0) {
            appendStatus("برای فایل‌های ناموفق، خودت از اپ گالری حذفشون کن.")
        }

        selectedUris = emptyList()
        sendButton.isEnabled = false
        deleteButton.isEnabled = false
    }

    private fun appendStatus(message: String) {
        statusText.append("\n$message")
    }
}
