### Umineko介绍
Umineko是用于实现Umineko无人机中指令下达、数据显示等功能的软件。使用[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform)构建的，拥有目前支持并适配Windows、Android、Web平台。

下面是一些项目运行需要用到的内容：

* [/composeApp](./composeApp/src) 用于存放将在你的 Compose 多平台应用程序之间共享的代码。
  [commonMain](./composeApp/src/commonMain/kotlin) 用于存放所有目标平台通用的代码。其他文件夹用于存放仅针对文件夹名称所指示的平台进行编译的 Kotlin 代码。

* [/server](./server/src/main/kotlin) 用于 Ktor 服务器应用程序。

* [/shared](./shared/src) 用于将在项目中所有目标平台之间共享的代码。最重要的子文件夹是 [commonMain](./shared/src/commonMain/kotlin)。如果愿意，你也可以在这里添加特定于平台的代码。

### 构建和运行 Android 应用程序

要构建和运行 Android 应用的开发版本，请使用 IDE 工具栏中运行小部件里的运行配置，或直接在终端中构建：

- 在 macOS/Linux 上
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- 在 Windows 上
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### 构建和运行桌面（JVM）应用程序

要构建和运行桌面应用的开发版本，请使用 IDE 工具栏中运行小部件里的运行配置，或直接在终端中运行：

- 在 macOS/Linux 上
  ```shell
  ./gradlew :composeApp:run
  ```
- 在 Windows 上
  ```shell
  .\gradlew.bat :composeApp:run
  ```

### 构建和运行服务器

要构建和运行服务器的开发版本，请使用 IDE 工具栏中运行小部件里的运行配置，或直接在终端中运行：

- 在 macOS/Linux 上
  ```shell
  ./gradlew :server:run
  ```
- 在 Windows 上
  ```shell
  .\gradlew.bat :server:run
  ```

### 构建和运行 Web 应用程序

要构建和运行 Web 应用的开发版本，请使用 IDE 工具栏中运行小部件里的运行配置，或直接在终端中运行：

- 针对 Wasm 目标（更快，现代浏览器）：
    - 在 macOS/Linux 上
      ```shell
      ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
      ```
    - 在 Windows 上
      ```shell
      .\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
      ```
- 针对 JS 目标（较慢，支持旧版浏览器）：
    - 在 macOS/Linux 上
      ```shell
      ./gradlew :composeApp:jsBrowserDevelopmentRun
      ```
    - 在 Windows 上
      ```shell
      .\gradlew.bat :composeApp:jsBrowserDevelopmentRun
      ```

---

了解更多关于 [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

如果你遇到任何问题，请在 [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP)报告。