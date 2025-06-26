// import 'package:flutter_test/flutter_test.dart';
// import 'package:folder_permission/folder_permission.dart';
// import 'package:folder_permission/folder_permission_platform_interface.dart';
// import 'package:folder_permission/folder_permission_method_channel.dart';
// import 'package:plugin_platform_interface/plugin_platform_interface.dart';

// class MockFolderPermissionPlatform
//     with MockPlatformInterfaceMixin
//     implements FolderPermissionPlatform {

//   @override
//   Future<String?> getPlatformVersion() => Future.value('42');
// }

// void main() {
//   final FolderPermissionPlatform initialPlatform = FolderPermissionPlatform.instance;

//   test('$MethodChannelFolderPermission is the default instance', () {
//     expect(initialPlatform, isInstanceOf<MethodChannelFolderPermission>());
//   });

//   test('getPlatformVersion', () async {
//     FolderPermission folderPermissionPlugin = FolderPermission();
//     MockFolderPermissionPlatform fakePlatform = MockFolderPermissionPlatform();
//     FolderPermissionPlatform.instance = fakePlatform;

//     expect(await folderPermissionPlugin.getPlatformVersion(), '42');
//   });
// }
