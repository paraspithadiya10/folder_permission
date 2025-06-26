package com.example.folder_permission

import android.content.Context
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** FolderPermissionPlugin */
class FolderPermissionPlugin: FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private lateinit var context: Context
  private var pendingResult: MethodChannel.Result? = null
  private var requestedPath: String? = null

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "folder_permission")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "request_permission" -> {
        val path = call.argument<String>("path")
        requestedPath = path
        pendingResult = result
        openDirectory(path)
        result.success("Permission requested for path: $path")
      }
      "check_Permission" -> {
        val path = call.argument<String>("path")
        val uriPermissions = contentResolver.persistedUriPermissions
        val hasPermission = uriPermissions.any {
          it.uri.toString().contains("$path") && it.isReadPermission
        }
        result.success(hasPermission)
      }
      else -> {
        result.notImplemented()
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

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}