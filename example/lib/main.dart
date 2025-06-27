import 'package:flutter/material.dart';
import 'package:folder_permission/folder_permission.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool? hasPermission;
  final String folderPath =
      "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses";

  @override
  void initState() {
    super.initState();
    _checkPermission();
  }

  Future<void> _checkPermission() async {
    try {
      print('Checking permission for path: $folderPath');
      final permission = await FolderPermission.checkPermission(
        path: folderPath,
      );
      print('Permission check result: $permission');
      setState(() {
        hasPermission = permission;
      });
    } catch (e) {
      print('Error checking permission: $e');
      setState(() {
        hasPermission = false;
      });
    }
  }

  Future<void> _requestPermission() async {
    try {
      print('Requesting permission for path: $folderPath');
      final granted = await FolderPermission.request(path: folderPath);
      print('Permission request result: $granted');

      if (granted) {
        print('Permission granted, rechecking status...');
        // Add a small delay to ensure the permission is fully processed
        await Future.delayed(const Duration(milliseconds: 500));
        await _checkPermission();
      } else {
        print('Permission was not granted');
        setState(() {
          hasPermission = false;
        });
      }
    } catch (e) {
      print('Error requesting permission: $e');
      setState(() {
        hasPermission = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Folder Permission Example')),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              ElevatedButton(
                onPressed: _requestPermission,
                child: const Text('Give Permission'),
              ),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: _checkPermission,
                child: const Text('Check Permission'),
              ),
              const SizedBox(height: 20),
              if (hasPermission != null)
                Container(
                  padding: const EdgeInsets.all(16),
                  margin: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: hasPermission!
                        ? Colors.green.shade100
                        : Colors.red.shade100,
                    border: Border.all(
                      color: hasPermission! ? Colors.green : Colors.red,
                      width: 2,
                    ),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    hasPermission!
                        ? '✓ Permission Granted!\nYou have access to the folder.'
                        : '✗ Permission Denied!\nPlease grant permission to access the folder.',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      color: hasPermission!
                          ? Colors.green.shade800
                          : Colors.red.shade800,
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
