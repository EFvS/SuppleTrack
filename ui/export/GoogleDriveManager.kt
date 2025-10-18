package com.efvs.suppletrack.ui.export

import android.content.Context
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GoogleDriveManager {
    fun getDriveService(context: Context, account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(com.google.api.services.drive.DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account
        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("SuppleTrack").build()
    }

    suspend fun uploadFile(
        context: Context,
        account: GoogleSignInAccount,
        fileUri: Uri,
        fileName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService(context, account)
            val fileMetadata = DriveFile().apply { name = fileName }
            val inputStream = context.contentResolver.openInputStream(fileUri) ?: return@withContext false
            val contentStream = com.google.api.client.http.InputStreamContent(
                "application/octet-stream", inputStream
            )
            drive.files().create(fileMetadata, contentStream).setFields("id").execute()
            inputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}