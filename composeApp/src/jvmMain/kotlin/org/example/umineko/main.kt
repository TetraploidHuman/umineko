package org.example.umineko

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.InputStreamReader

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "umineko",
    ) {
        App()
    }
}

@Composable
actual fun determinePlatformType(): PlatformType {
    //桌面端用于测试TABLET版本的UI（主要是我没有平板~）
    return PlatformType.TABLET

    var platformType by remember { mutableStateOf(PlatformType.DESKTOP) }

    LaunchedEffect(Unit) {
        while (true) {
            // 获取当前的系统模式
            val isCurrentlyTablet = isWindowsInTabletUiMode()
            val currentPlatformType = if (isCurrentlyTablet) PlatformType.TABLET else PlatformType.DESKTOP

            if (platformType != currentPlatformType) {
                println("设备模式发生改变，新的设备模式是: $currentPlatformType")
                platformType = currentPlatformType
            }

            // 等待 1 秒钟再进行下一次查询注册表
            delay(1000L)
        }
    }

    return platformType
}

/**
 * 通过查询 Windows 注册表来检测 UI 是否处于“平板模式”。
 * 这是从桌面应用检测此状态的最可靠方法。
 * @return 如果 UI 处于平板模式，则为 true。
 */
private fun isWindowsInTabletUiMode(): Boolean {
    val os = System.getProperty("os.name").lowercase()
    if (!os.contains("win")) {
        return false
    }

    try {
        val command = "reg query \"HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\ImmersiveShell\" /v TabletMode"
        val process = Runtime.getRuntime().exec(command)
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (line!!.contains("TabletMode") && line.contains("REG_DWORD")) {
                val parts = line.split(Regex("\\s+"))
                if (parts.isNotEmpty()) {
                    val hexValue = parts.last()
                    val intValue = hexValue.substring(2).toIntOrNull(16)
                    if (intValue != null) {
                        process.waitFor()
                        reader.close()
                        return intValue == 1
                    }
                }
            }
        }

        process.waitFor()
        reader.close()
        return false

    } catch (e: Throwable) {
        // 在监控循环中，我们不希望每次错误都打印堆栈跟踪，只在首次或必要时打印
        // println("ERROR: Registry query failed: ${e.message}")
        return false
    }
}