package org.example.umineko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}

@Composable
actual fun determinePlatformType(): PlatformType {
    // 使用用户指定的 500dp 作为区分手机和平板的阈值
    val configuration = LocalConfiguration.current
    return if (configuration.screenWidthDp.dp > 500.dp) PlatformType.TABLET else PlatformType.MOBILE
}