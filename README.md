# <img src="imgs/va-logo.png" width="200px" align="center" alt="VirtualAPK"/>
[![license](http://img.shields.io/badge/license-Apache2.0-brightgreen.svg?style=flat)](https://github.com/didichuxing/VirtualAPK/blob/master/LICENSE)
[![Release Version](https://img.shields.io/badge/release-0.9.0-red.svg)](https://github.com/didichuxing/VirtualAPK/releases)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/didichuxing/VirtualAPK/pulls)

VirtualAPK is a powerful but lightweight plugin framework for Android, it can load an apk file dynamically, then the loaded apk file which is called LoadedPlugin by us can be treated as applications installed.

Through VirtualAPK, developers can visit Class and Resources in LoadedPlugin, more important, can visit Android components(Activity/Service/Receiver/Provider) just like they are registered in Android.

![VirtualAPK](imgs/va.png)
# Feature supported

|Feature|Detail
|:-------------|:-------------:|
| Supported components |Activity / Service / Receiver / Provider
| Components need to register in AndroidManifest.xml |Not needed
| Plugin can depend on host app| Yes
| Support PendingIntent| Yes
| Supported Android features| Almost all features
| Compatible devices| Almost all devices
| How to build plugin apk |Gradle plugin
| Supported Android versions |API 15 +
# Getting started
### Host Project
Add the following dependency in the build.gradle in root path of host project:
``` java
dependencies {
    classpath 'com.didi.virtualapk:gradle:0.9.0'
}
```

Apply plugin in the build.gradle of application module:
```
apply plugin: 'com.didi.virtualapk.host'
```

Add the following dependency in the build.gradle of application module:
``` java
compile 'com.didi.virtualapk:core:0.9.0'
```

Then add initial code in attachBaseContext method of application:
``` java
@Override
protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    PluginManager.getInstance(base).init();
}
```

Lastly, add the following proguard rules to your application module:
```
-keep class com.didi.virtualapk.internal.VAInstrumentation { *; }
-keep class com.didi.virtualapk.internal.PluginContentResolver { *; }

-dontwarn com.didi.virtualapk.**
-dontwarn android.content.pm.**
-keep class android.** { *; }
```

Now, you can load an apk as you wish, for example:

``` java
String pluginPath = Environment.getExternalStorageDirectory().getAbsolutePath().concat("/Test.apk");
File plugin = new File(pluginPath);
PluginManager.getInstance(base).loadPlugin(plugin);

//suppose "com.didi.virtualapk.demo" is the package name of plugin apk.
Intent intent = new Intent();
intent.setClassName("com.didi.virtualapk.demo", "com.didi.virtualapk.demo.MainActivity");
startActivity(intent);
```
### Plugin Project
Add the following dependency in the build.gradle in root path of plugin project:
``` java
dependencies {
    classpath 'com.didi.virtualapk:gradle:0.9.0'
}
```

Then apply plugin in the build.gradle of application module and config VirtualAPK.

Note : put the following code at the end of build.gradle
```
apply plugin: 'com.didi.virtualapk.plugin'
virtualApk {
    packageId = 0x6f // the package id of Resources.
    targetHost='source/host/app' // the path of application module in host project.
    applyHostMapping = true // optional, default value: true. 
}
```
# Develop guide

1. See the [wiki](https://github.com/didichuxing/VirtualAPK/wiki)
2. See the sample project [PluginDemo](https://github.com/didichuxing/VirtualAPK/tree/master/PluginDemo)
3. Read the [source code](https://github.com/didichuxing/VirtualAPK/tree/master/CoreLibrary)

# Known issues
- not support notifications with custom layout in plugin
- not support transition animations with animation resources in plugin

# Contributing
Welcome to contribute to VirtualAPK, you can contribute issues or pull requests, see the [Contributing Guide](CONTRIBUTING.md).

# Who is using VirtualAPK?
<img src="imgs/didi.png" width="78px" align="center" alt="滴滴出现"/> <img src="imgs/uber-china.png" width="78px" align="center" alt="Uber中国"/>

# License
VirtualAPK is under the Apache License 2.0, see the [LICENSE](LICENSE) file.
