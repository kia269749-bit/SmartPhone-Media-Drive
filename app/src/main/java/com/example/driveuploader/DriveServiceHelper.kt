package com.example.driveuploader

import android.content.Context
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile

class DriveServiceHelper(context: Context, account: GoogleSignInAccount) {

    private val driveService: Drive

    init {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        driveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Drive Uploader")
            .build()
    }

    fun uploadFile(context: Context, uri: Uri, displayName: String, mimeType: String): String {
        val resolver = context.contentResolver
        val inputStream = resolver.openInputStream(uri)
            ?: throw IllegalStateException("نمی‌توان فایل را باز کرد: $displayName")

        inputStream.use { stream ->
            val fileMetadata = DriveFile().setName(displayName)
            val mediaContent = InputStreamContent(mimeType, stream)

            val uploadedFile = driveService.files()
                .create(fileMetadata, mediaContent)
                .setFields("id, name, webViewLink")
                .execute()

            return uploadedFile.id
        }
    }
}
