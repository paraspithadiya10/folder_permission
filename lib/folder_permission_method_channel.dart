import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'folder_permission_platform_interface.dart';

/// An implementation of [FolderPermissionPlatform] that uses method channels.
class MethodChannelFolderPermission extends FolderPermissionPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('folder_permission');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
