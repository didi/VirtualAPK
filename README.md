# <img src="imgs/va-logo.png" width="200px" align="center" alt="VirtualAPK"/>
[![license](http://img.shields.io/badge/license-Apache2.0-brightgreen.svg?style=flat)](https://github.com/didi/VirtualAPK/blob/master/LICENSE)
[![Release Version](https://img.shields.io/badge/release-0.9.1-red.svg)](https://github.com/didi/VirtualAPK/releases)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/didi/VirtualAPK/pulls)

VirtualAPK is a powerful yet lightweight plugin framework for Android. It can dynamically load and run an APK file (we call it `LoadedPlugin`) seamlessly as an installed application. Developers can use any Class, Resources, Activity, Service, Receiver and Provider in `LoadedPlugin` as if they are registered in app's manifest file.

![VirtualAPK](imgs/va.png)

# Supported Features

| Feature | Detail |
|:-------------|:-------------:|
| Supported components | Activity, Service, Receiver and Provider |
| Manually register components in AndroidManifest.xml | No need |
| Access host app classes and resources | Supported |
| PendingIntent | Supported |
| Supported Android features | Almost all features |
| Compatibility | Almost all devices |
| Building system | Gradle plugin |
| Supported Android versions | API Level 15+ |

# Getting started

## Host Project

Add a dependency in `build.gradle` in root of host project as following.

``` java
dependencies {
    classpath 'com.didi.virtualapk:gradle:0.9.8.4'
}
```

Apply plugin in application module of `build.gradle`.

```
apply plugin: 'com.didi.virtualapk.host'
```

Compile VirtualAPK in application module of `build.gradle`.

``` java
compile 'com.didi.virtualapk:core:0.9.6'
```

Initialize `PluginManager` in `YourApplication::attachBaseContext()`.

``` java
@Override
protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    PluginManager.getInstance(base).init();
}
```

Modify proguard rules to keep VirtualAPK related files.

```
-keep class com.didi.virtualapk.internal.VAInstrumentation { *; }
-keep class com.didi.virtualapk.internal.PluginContentResolver { *; }

-dontwarn com.didi.virtualapk.**
-dontwarn android.**
-keep class android.** { *; }
```

Finally, load an APK and have fun!

``` java
String pluginPath = Environment.getExternalStorageDirectory().getAbsolutePath().concat("/Test.apk");
File plugin = new File(pluginPath);
PluginManager.getInstance(base).loadPlugin(plugin);

// Given "com.didi.virtualapk.demo" is the package name of plugin APK, 
// and there is an activity called `MainActivity`.
Intent intent = new Intent();
intent.setClassName("com.didi.virtualapk.demo", "com.didi.virtualapk.demo.MainActivity");
startActivity(intent);
```

## Plugin Project

Add a dependency in `build.gradle` in root of plugin project as following.

``` java
dependencies {
    classpath 'com.didi.virtualapk:gradle:0.9.8.4'
}
```

Apply plugin in application module of `build.gradle`.

```
apply plugin: 'com.didi.virtualapk.plugin'
```

Config VirtualAPK. Remember to put following lines at the end of `build.gradle`.

```
virtualApk {
    packageId = 0x6f             // The package id of Resources.
    targetHost='source/host/app' // The path of application module in host project.
    applyHostMapping = true      // [Optional] Default value is true. 
}
```

# Developer guide

* API document [wiki](https://github.com/didi/VirtualAPK/wiki)
* Sample project [PluginDemo](https://github.com/didi/VirtualAPK/tree/master/PluginDemo)
* Read [core library source code](https://github.com/didi/VirtualAPK/tree/master/CoreLibrary)
* Read [Release notes](RELEASE-NOTES.md)

# Known issues

* Notifications with custom layout are not supported in plugin.
* Transition animations with animation resources are not supported in plugin.

# Contributing

Welcome to contribute by creating issues or sending pull requests. See [Contributing Guide](CONTRIBUTING.md) for guidelines.

# Who is using VirtualAPK?

<img src="imgs/didi.png" width="78px" align="center" alt="滴滴出行"/> <img src="imgs/uber-china.png" width="78px" align="center" alt="Uber中国"/>

# License

VirtualAPK is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file.
