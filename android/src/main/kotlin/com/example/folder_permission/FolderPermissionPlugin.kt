package com.example.folder_permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry

/** FolderPermissionPlugin */
class FolderPermissionPlugin: FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private lateinit var context: Context
  private var activity: Activity? = null
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
      }
      "check_Permission" -> {
        val path = call.argument<String>("path")
        val uriPermissions = context.contentResolver.persistedUriPermissions
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
    if (activity == null) {
      pendingResult?.error("NO_ACTIVITY", "Activity not available", null)
      pendingResult = null
      return
    }

    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
      // Determine the correct path based on Android version
      val pickerInitialPath = Environment.getExternalStorageDirectory().absolutePath +
              "/$path"
      val folderUri = Uri.parse("file://$pickerInitialPath")
      putExtra(DocumentsContract.EXTRA_INITIAL_URI, folderUri)
    }
    activity?.startActivityForResult(intent, 2000)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    try {
      if (requestCode == 2000) {
        if (resultCode == Activity.RESULT_OK) {
          data?.data?.also { uri ->
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)

            // Check if permission was actually granted
            val uriPermissions = context.contentResolver.persistedUriPermissions
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
        return true
      }
    } catch (ex: Exception) {
      Log.d("OnActivity failure", "ON_ACTIVITY_RESULT FAILED WITH EXCEPTION\n$ex")
      pendingResult?.error("PERMISSION_ERROR", ex.message, null)
      pendingResult = null
    }
    return false
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    binding.addActivityResultListener(this)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activity = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
    binding.addActivityResultListener(this)
  }

  override fun onDetachedFromActivity() {
    activity = null
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}