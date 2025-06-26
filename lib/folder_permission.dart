import 'package:flutter/services.dart';

class FolderPermission {
  /// Define Method Channel
  static const MethodChannel _channel = MethodChannel('folder_permission');

  // Request Permission For Given Path
  static Future request({required String path}) async {
    await _channel.invokeMethod('request_permission', {"path": path});
  }

  // Check Permission For Given Path
  static Future checkPermission({required String path}) async {
    await _channel.invokeMethod('check_Permission', {"path": path});
  }
}
