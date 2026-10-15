/*
 * WebIDE - A powerful IDE for Android web development.
 * Copyright (C) 2025  如日中天  <3382198490@qq.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.web.webide.ui.projects

object ProjectTemplates {

    val normalIndexHtml = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Web Project</title>
</head>
<body>
    <h1>Hello World</h1>
    <button id="btn">点击测试</button>
    <p id="log"></p>
    <script src="js/script.js"></script>
</body>
</html>
    """.trimIndent()

    val normalCss = """
body { padding: 20px; font-family: sans-serif; }
button { padding: 10px 20px; font-size: 16px; margin-top: 10px; }
    """.trimIndent()

    val normalJs = """
document.getElementById('btn').onclick = function() {
    document.getElementById('log').innerText = '时间: ' + new Date().toLocaleTimeString();
};
    """.trimIndent()

    val normalIndexPhp = """
<?php
echo "Hello from PHP!";
?>
    """.trimIndent()

    val apiJs = """
(function() {
    const isAndroid = !!(window.Android && window.Android.httpRequest);
    
    // 1. Native 通信 (Android)
    const callbacks = {};
    window.onAndroidResponse = function(id, b64) {
        const cb = callbacks[id];
        if (cb) {
            try {
                const json = JSON.parse(decodeURIComponent(escape(window.atob(b64))));
                json.success ? cb.resolve(json.data) : cb.reject(json.data);
            } catch (e) { cb.reject("Error"); }
            delete callbacks[id];
        }
    };
    const call = (method, ...args) => new Promise((resolve, reject) => {
        if (!isAndroid) return reject("Browser Mode");
        const id = 'cb_' + Math.random().toString(36).substr(2, 9);
        callbacks[id] = { resolve, reject };
        window.Android[method](...args, id);
    });

    // 2. 浏览器模拟 (Browser Polyfill)
    const toastInBrowser = (msg) => {
        const d = document.createElement('div');
        d.style.cssText = "position:fixed;bottom:20%;left:50%;transform:translateX(-50%);background:rgba(0,0,0,0.7);color:#fff;padding:8px 16px;border-radius:4px;font-size:14px;z-index:999;";
        d.innerText = msg;
        document.body.appendChild(d);
        setTimeout(() => d.remove(), 2000);
    };

    // 3. 对外接口
    window.App = {
        ui: {
            toast: (msg) => isAndroid ? window.Android.showToast(msg) : toastInBrowser(msg),
            notification: (id, t, cpp) => isAndroid && window.Android.showNotification(id, t, cpp)
        },
        sys: {
            vibrate: (ms) => isAndroid ? window.Android.vibrate(ms) : navigator.vibrate?.(ms),
            copy: (t) => isAndroid ? window.Android.copyToClipboard(t) : navigator.clipboard.writeText(t)
        },
        file: {
            write: (p, cpp) => isAndroid ? call('writeFile', p, cpp) : localStorage.setItem(p,cpp),
            read: (p) => isAndroid ? call('readFile', p) : Promise.resolve(localStorage.getItem(p))
        },
        http: {
            get: async (url) => isAndroid ? JSON.parse(await call('httpRequest', 'GET', url, '{}', '')) : (await fetch(url)).text()
        }
    };
})();
    """.trimIndent()

    val webAppIndexHtml = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>WebApp</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <h3>WebApp 演示</h3>
    <button onclick="testToast()">点击弹出 Toast</button>
    <script src="js/api.js"></script>
    <script src="js/index.js"></script>
</body>
</html>
    """.trimIndent()

    val webAppCss = """
body { padding: 20px; font-family: sans-serif; }
button { 
    padding: 12px 24px; 
    font-size: 16px; 
    background-color: #007bff; 
    color: white; 
    border: none; 
    border-radius: 5px; 
}
button:active { background-color: #0056b3; }
    """.trimIndent()

    val webAppIndexJs = """
function testToast() {
    App.ui.toast("Hello World! 这是一个 Toast");
}
    """.trimIndent()

    val nodePackageJson = """
{
  "name": "myapp",
  "version": "1.0.0",
  "description": "Node.js application in WebIDE",
  "main": "index.js",
  "scripts": {
    "start": "node index.js"
  },
  "dependencies": {}
}
    """.trimIndent()

    val nodeIndexJs = """
const http = require('http');

const server = http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end('<h1>Hello from Node.js server in WebIDE!</h1><p>Running on Ubuntu PRoot.</p>');
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log(`Server running at http://localhost:${'$'}{PORT}/`);
});
    """.trimIndent()

    val pythonMainPy = """
def main():
    print("Hello from Python in WebIDE!")
    print("Running inside Ubuntu 20.04 LTS PRoot environment.")

if __name__ == "__main__":
    main()
    """.trimIndent()

    val gitignore = """
node_modules/
__pycache__/
*.pyc
.env
dist/
build/
.DS_Store
.dart_tool/
.packages
.pub-cache/
.pub/
    """.trimIndent()

    val readmeMd = """
# Project

Created with WebIDE & Mobile-Harness Ubuntu PRoot subsystem.
    """.trimIndent()

    fun flutterPubspecYaml(name: String): String = """
name: ${name.lowercase(java.util.Locale.ROOT).replace(Regex("[^a-z0-9_]"), "_").ifBlank { "my_flutter_app" }}
description: "A new Flutter project created in WebIDE."
publish_to: 'none'
version: 1.0.0+1

environment:
  sdk: '>=3.0.0 <4.0.0'

dependencies:
  flutter:
    sdk: flutter
  cupertino_icons: ^1.0.8

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^3.0.0

flutter:
  uses-material-design: true
    """.trimIndent()

    val flutterMainDart = """
import 'package:flutter/material.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'WebIDE Flutter App',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Flutter in WebIDE'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  int _counter = 0;

  void _incrementCounter() {
    setState(() {
      _counter++;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            const Text(
              'You have pushed the button this many times:',
            ),
            Text(
              "${'$'}_counter",
              style: Theme.of(context).textTheme.headlineMedium,
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _incrementCounter,
        tooltip: 'Increment',
        child: const Icon(Icons.add),
      ),
    );
  }
}
    """.trimIndent()

    val flutterWebIndexHtml = """
<!DOCTYPE html>
<html>
<head>
  <base href="/">
  <meta charset="UTF-8">
  <meta content="IE=Edge" http-equiv="X-UA-Compatible">
  <meta name="description" content="A new Flutter project created with WebIDE.">
  <meta name="apple-mobile-web-app-capable" content="yes">
  <meta name="apple-mobile-web-app-status-bar-style" content="black">
  <meta name="apple-mobile-web-app-title" content="WebIDE Flutter App">
  <title>Flutter App</title>
</head>
<body>
  <script src="flutter_bootstrap.js" async></script>
</body>
</html>
    """.trimIndent()

    val flutterAnalysisOptions = """
include: package:flutter_lints/flutter.yaml

linter:
  rules:
    prefer_const_constructors: true
    prefer_const_literals_to_create_immutables: true
    """.trimIndent()
}