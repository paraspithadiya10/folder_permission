import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'folder_permission_method_channel.dart';

abstract class FolderPermissionPlatform extends PlatformInterface {
  /// Constructs a FolderPermissionPlatform.
  FolderPermissionPlatform() : super(token: _token);

  static final Object _token = Object();

  static FolderPermissionPlatform _instance = MethodChannelFolderPermission();

  /// The default instance of [FolderPermissionPlatform] to use.
  ///
  /// Defaults to [MethodChannelFolderPermission].
  static FolderPermissionPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FolderPermissionPlatform] when
  /// they register themselves.
  static set instance(FolderPermissionPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
