package com.example.folder_permission_example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity(){
    private val channel = "folder_permission"
    private var pendingResult: MethodChannel.Result? = null
    private var requestedPath: String? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor, channel)
            .setMethodCallHandler { call, result ->
                when (call.method){
                    "request_permission" -> {
                        val path: String? = call.argument("path")
                        requestedPath = path
                        pendingResult = result
                        openDirectory(path)
                    }
                    "check_Permission" -> {
                        try {
                            val path: String? = call.argument("path")
                            val uriPermissions = contentResolver.persistedUriPermissions
                            val hasPermission = uriPermissions.any {
                                it.uri.toString().contains("$path") && it.isReadPermission
                            }
                            result.success(hasPermission)
                        } catch (e: Exception) {
                            result.error("PERMISSION_CHECK_ERROR", e.message, null)
                        }
                    }
                    else -> {
                        result.notImplemented()
                    }
                }
            }
    }

    private fun openDirectory(path: String?) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            // Determine the correct path based on Android version
            val pickerInitialPath = Environment.getExternalStorageDirectory().absolutePath +
                    "$path"
            val folderUri = Uri.parse("file://$pickerInitialPath")
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, folderUri)
        }
        startActivityForResult(intent, 2000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (requestCode == 2000) {
                if (resultCode == RESULT_OK) {
                    data?.data?.also { uri ->
                        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        contentResolver.takePersistableUriPermission(uri, takeFlags)

                        // Check if permission was actually granted
                        val uriPermissions = contentResolver.persistedUriPermissions
                        val hasPermission = uriPermissions.any {
                            it.uri.toString().contains("$requestedPath") && it.isReadPermission
                        }
                        pendingResult?.success(hasPermission)
                    } ?: run {
                        pendingResult?.success(false)
                    }
                } else {
                    pendingResult?.success(false)
                }
                pendingResult = null
            }
        } catch (ex: Exception) {
            Log.d("OnActivity failure", "ON_ACTIVITY_RESULT FAILED WITH EXCEPTION\n$ex")
            pendingResult?.error("PERMISSION_ERROR", ex.message, null)
            pendingResult = null
        }
    }

}
