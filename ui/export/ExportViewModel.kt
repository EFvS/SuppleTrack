package com.efvs.suppletrack.ui.export

// ...other imports...
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

class ExportViewModel @Inject constructor(
    application: Application,
    // ...existing constructor...
) : AndroidViewModel(application) {
    // ...existing code...

    fun uploadFileToGoogleDrive(
        account: GoogleSignInAccount,
        fileUri: Uri,
        fileName: String,
        onDone: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = GoogleDriveManager.uploadFile(context, account, fileUri, fileName)
            onDone(result)
        }
    }
}