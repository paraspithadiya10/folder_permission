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

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "folder_permission")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "request_permission" -> {
        val path = call.argument<String>("path")
        pendingResult = result
        openDirectory(path)
      }
      "check_Permission" -> {
        val path = call.argument<String>("path")
        val hasPermission = checkPathPermission(path)
        result.success(hasPermission)
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  private fun checkPathPermission(path: String?): Boolean {
    val uriPermissions = context.contentResolver.persistedUriPermissions
    Log.d("FolderPermission", "Checking permission for path: $path")
    Log.d("FolderPermission", "Number of persisted permissions: ${uriPermissions.size}")

    val hasPermission = uriPermissions.any { permission ->
      val uriPath = permission.uri.toString()
      Log.d("FolderPermission", "Checking URI: $uriPath")

      // Clean the path by removing leading slash
      val cleanPath = path?.removePrefix("/") ?: ""
      val encodedPath = Uri.encode(cleanPath)

      // Check multiple ways the path might be encoded in the URI
      val pathVariants = listOf(
        cleanPath,
        encodedPath,
        cleanPath.replace("/", "%2F"),
        cleanPath.replace("/", "%2f")
      )

      val matches = pathVariants.any { variant ->
        uriPath.contains(variant, ignoreCase = true)
      }

      Log.d("FolderPermission", "URI matches path: $matches, isReadPermission: ${permission.isReadPermission}")
      matches && permission.isReadPermission
    }
    
    Log.d("FolderPermission", "Final permission result: $hasPermission")
    return hasPermission
  }

  private fun openDirectory(path: String?) {
    if (activity == null) {
      pendingResult?.error("NO_ACTIVITY", "Activity not available", null)
      pendingResult = null
      return
    }

    try {
      val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        // Handle different path formats
        if (path != null) {
          val initialUri = DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            "primary:$path"
          )
          putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
        }
        
//        // Add flags to make the intent more likely to work
//        addCategory(Intent.CATEGORY_DEFAULT)
      }
      
      Log.d("FolderPermission", "Starting document tree picker...")
      activity?.startActivityForResult(intent, 2000)
    } catch (e: Exception) {
      Log.e("FolderPermission", "Failed to open directory picker", e)
      pendingResult?.error("INTENT_ERROR", "Failed to open directory picker: ${e.message}", null)
      pendingResult = null
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    try {
      if (requestCode == 2000) {
        if (resultCode == Activity.RESULT_OK) {
          data?.data?.also { uri ->
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)

            Log.d("FolderPermission", "Permission granted for URI: $uri")
            pendingResult?.success(true)
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