package com.example.driveuploader

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private var currentAccount: GoogleSignInAccount? = null

    private lateinit var statusText: TextView
    private lateinit var signInButton: Button
    private lateinit var pickButton: Button

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            currentAccount = task.getResult(ApiException::class.java)
            appendStatus("وارد شدی به عنوان: ${currentAccount?.email}")
        } catch (e: ApiException) {
            appendStatus("ورود ناموفق (کد ${e.statusCode}): ${e.message}")
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNullOrEmpty()) {
            appendStatus("هیچ فایلی انتخاب نشد.")
            return@registerForActivityResult
        }
        val account = currentAccount
        if (account == null) {
            appendStatus("اول باید با حساب گوگل وارد بشی.")
            return@registerForActivityResult
        }
        uploadFiles(uris, account)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        signInButton = findViewById(R.id.signInButton)
        pickButton = findViewById(R.id.pickButton)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        currentAccount = GoogleSignIn.getLastSignedInAccount(this)
        currentAccount?.let {
            appendStatus("قبلاً وارد شدی: ${it.email}")
        }

        signInButton.setOnClickListener {
            signInLauncher.launch(googleSignInClient.signInIntent)
        }

        pickButton.setOnClickListener {
            filePickerLauncher.launch(arrayOf("image/*", "video/*"))
        }
    }

    private fun uploadFiles(uris: List<Uri>, account: GoogleSignInAccount) {
        val helper = DriveServiceHelper(this, account)

        CoroutineScope(Dispatchers.Main).launch {
            for (uri in uris) {
                val name = getFileName(uri)
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                appendStatus("در حال آپلود: $name ...")

                try {
                    val fileId = withContext(Dispatchers.IO) {
                        helper.uploadFile(this@MainActivity, uri, name, mimeType)
                    }
                    appendStatus("✅ آپلود موفق: $name  (Drive ID: $fileId)")
                } catch (e: Exception) {
                    appendStatus("❌ آپلود ناموفق: $name  —  ${e.message}")
                }
            }
            appendStatus("پایان عملیات. فایل اصلی روی گوشی دست‌نخورده باقی مانده.")
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "unknown_file"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun appendStatus(message: String) {
        statusText.append("\n$message")
    }
}
