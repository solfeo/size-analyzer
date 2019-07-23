# Size Analyzer

The Size Analyzer is a tool for developers to understand the size of their
Android application.

## How to build the size analyzer
The tool can be built using [gradle](https://gradle.org/). An executable jar can
be built using the command below:

``` shell
./gradlew :analyzer:executableJar
```

## How to use the size analyzer

The executable jar can be run against either an Android Studio project or an
[Android App Bundle](https://g.co/androidappbundle).

```shell
java -jar analyzer/build/libs/analyzer.jar check-bundle <path-to-aab>
java -jar analyzer/build/libs/analyzer.jar check-project <path-to-project-directory>
```

## Android Studio Plugin

The Size Analyzer is also available in an Android Studio plugin format. This
can be built with the buildPlugin task in gradle:

``` shell
./gradlew :studio_plugin:buildPlugin
```

This will produce a JAR file that can be installed from the gear menu's
'Install Plugin from Disk...' option in the Plugins pane of Android Studio's
Settings dialog.

Alternatively, it can be downloaded and installed from the Jetbrains plugin
marketplace in Android Studio.

Once installed, you can invoke the Size Analyzer on the currently loaded project
from the 'Analyze App Size...' menu item in the 'Analyze' menu in Android Studio.

## Binary distributions
Pre-built distributions of this tool will be made available with each release
on our [releases page](https://github.com/android/size-analyzer/releases).
