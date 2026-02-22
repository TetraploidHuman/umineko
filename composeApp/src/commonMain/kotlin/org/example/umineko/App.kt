//处理UI的平台差异
package org.example.umineko

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.material3.FloatingActionButtonDefaults.elevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import umineko.composeapp.generated.resources.Res
import umineko.composeapp.generated.resources.map
import kotlin.math.roundToInt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.TimeSource

// --- 动画常量 ---
private const val ANIMATION_DURATION_MS = 300
private val animationSpecDp = tween<Dp>(
    durationMillis = ANIMATION_DURATION_MS,
    easing = FastOutSlowInEasing  // 缓动曲线，开始慢，中间快，结束慢
)
private val animationSpecFloat = tween<Float>(durationMillis = ANIMATION_DURATION_MS)
private val animationSpecFloatQuick = tween<Float>(durationMillis = ANIMATION_DURATION_MS)

// 移动端UI常量
private val mobileRegularItemHeight = 48.dp
private val mobileitemSpacing = 4.dp
private val commandBarHeight = 50.dp

// 平板UI常量
private val tabletItemHeight = 56.dp
private val tabletItemSpacing = 8.dp
private val tabletCommandBarHeight = 64.dp

// --- 数据类 ---
data class NavItem(
    val title: String,
    val icon: ImageVector,
    val subtitle: String? = null
)

enum class PlatformType {
    MOBILE,  // 手机
    TABLET,  // 平板（或无键盘的电脑）
    DESKTOP  // 带键盘的桌面电脑
}

@Composable
expect fun determinePlatformType(): PlatformType

@Composable
fun App() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF0078D4),
            surface = Color(0xFFF3F3F3),
            surfaceVariant = Color(0xFFE5E5E5),
            onSurface = Color.Black,
            onSurfaceVariant = Color.Black.copy(alpha = 0.8f)
        )
    ) {
        MainLayout()
        //LoginScreenDemo()
    }
}

@Composable
fun MainLayout() {
    val platformType = determinePlatformType()
    var selectedIndex by remember { mutableStateOf(0) }
    val apiService = remember { ApiService() }

    var username by remember { mutableStateOf("加载中…") }
    var retryInfo by remember { mutableStateOf("0/3") } // 存储 "1/3"

    LaunchedEffect(Unit) {
        username = apiService.sendMessage(
            maxRetries = 3,
            onRetry = { status ->
                retryInfo = status // 更新重试次数
            }
        )
        // 请求完成后，清空重试信息
        retryInfo = ""
    }

    val fullUserProfile = remember(username, retryInfo) {
        NavItem(
            title = username,
            icon = Icons.Filled.AccountCircle,
            subtitle = if (retryInfo.isNotEmpty()) "正在重试 ($retryInfo)" else "Server无响应"
        )
    }

    val navItems = listOf(
        NavItem("个人", fullUserProfile.icon),
        NavItem("Home+$platformType", Icons.Outlined.Home),
        NavItem("Mail", Icons.Outlined.Email),
        NavItem("Settings", Icons.Filled.Settings),
        NavItem("About", Icons.Filled.Info),
    )

    when(platformType){
        PlatformType.DESKTOP -> {
            var isNavExpanded by remember { mutableStateOf(false) }
            Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                NavigationPaneDesktop(
                    items = navItems,
                    fullUserProfile = fullUserProfile,
                    selectedIndex = selectedIndex,
                    isExpanded = isNavExpanded,
                    onMenuToggle = { isNavExpanded = !isNavExpanded },
                    onItemSelected = { selectedIndex = it }
                )
                Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 6.dp)) {
                    TopCommandBarDesktop(title = navItems[selectedIndex].title)
                    ContentArea(pageIndex = selectedIndex)
                }
            }

        }
        PlatformType.MOBILE-> {
            NavigationPaneMobile(
                items = navItems,
                fullUserProfile = fullUserProfile,
                selectedIndex = selectedIndex,
                onItemSelected = { selectedIndex = it }
            )
        }
        PlatformType.TABLET-> {
            NavigationPaneTablet(
                items = navItems,
                fullUserProfile = fullUserProfile,
                selectedIndex = selectedIndex,
                onItemSelected = { selectedIndex = it }
            )
        }
    }
}


@Composable
fun TopCommandBarDesktop(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "Search") }
        IconButton(onClick = {}) { Icon(Icons.Default.Share, contentDescription = "Share") }
        IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
    }
}

@Composable
fun NavigationPaneTablet(
    items: List<NavItem>,
    fullUserProfile: NavItem,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    var isNavExpanded by remember { mutableStateOf(true) }

    val width by animateDpAsState(
        targetValue = if (isNavExpanded) 280.dp else 68.dp,
        animationSpec = animationSpecDp
    )

    val expandedProfileItemHeight = 80.dp
    val regularItemHeightTablet = tabletItemHeight
    val itemSpacingTablet = tabletItemSpacing

    val animatedProfileHeight by animateDpAsState(
        targetValue = if (isNavExpanded) expandedProfileItemHeight else regularItemHeightTablet,
        animationSpec = animationSpecDp
    )

    val density = LocalDensity.current
    var aboutItemActualY by remember { mutableStateOf(0.dp) }

    val indicatorOffsetY = remember(selectedIndex, isNavExpanded, aboutItemActualY) {
        derivedStateOf {
            val menuButtonTotalHeight = 80.dp
            val dividerTotalHeight = 24.dp

            when (selectedIndex) {
                0 -> menuButtonTotalHeight
                in 1 until items.size - 1 -> {
                    var offset = menuButtonTotalHeight
                    offset += if (isNavExpanded) expandedProfileItemHeight else regularItemHeightTablet
                    offset += dividerTotalHeight
                    val middleItemIndex = selectedIndex - 1
                    offset += (regularItemHeightTablet + itemSpacingTablet) * middleItemIndex
                    offset
                }
                items.size - 1 -> aboutItemActualY
                else -> menuButtonTotalHeight
            }
        }
    }.value

    val animatedIndicatorOffsetY by animateDpAsState(
        targetValue = indicatorOffsetY,
        animationSpec = animationSpecDp
    )

    val indicatorHeight by animateDpAsState(
        targetValue = if (selectedIndex == 0) {
            if (isNavExpanded) expandedProfileItemHeight else regularItemHeightTablet
        } else {
            regularItemHeightTablet
        },
        animationSpec = animationSpecDp
    )

    val arrowRotation by animateFloatAsState(
        targetValue = if (isNavExpanded) 90f else 0f,
        animationSpec = animationSpecFloat
    )

    // --- 核心修改点 ---
    // 1. 使用 Column 作为根布局，实现垂直排列
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // 2. 将原来的 Row 作为 Column 的第一个子项，并用 weight(1f) 占据大部分空间
        Row(modifier = Modifier.weight(1f)) {
            // 导航侧边栏 (这部分代码不变)
            Surface(
                modifier = Modifier
                    .width(width)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 选中指示器
                    Box(
                        modifier = Modifier
                            .offset(y = animatedIndicatorOffsetY)
                            .height(indicatorHeight)
                            .padding(vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .fillMaxHeight()
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                                )
                        )
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // 菜单切换按钮
                        IconButton(
                            onClick = { isNavExpanded = !isNavExpanded },
                            modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Toggle Menu",
                                modifier = Modifier
                                    .size(28.dp)
                                    .rotate(arrowRotation)
                            )
                        }

                        // 用户资料卡片
                        AnimatedNavigationItem(
                            item = fullUserProfile,
                            isSelected = selectedIndex == 0,
                            isExpanded = isNavExpanded,
                            isProfileCard = true,
                            onClick = { onItemSelected(0) },
                            modifier = Modifier.height(animatedProfileHeight),
                            isTablet = true
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            thickness = 1.5.dp,
                            color = DividerDefaults.color
                        )

                        // 中间导航项
                        val middleNavItems = items.subList(1, items.size - 1)
                        middleNavItems.forEachIndexed { index, item ->
                            val absoluteIndex = index + 1
                            AnimatedNavigationItem(
                                item = item,
                                isSelected = selectedIndex == absoluteIndex,
                                isExpanded = isNavExpanded,
                                onClick = { onItemSelected(absoluteIndex) },
                                modifier = Modifier.height(regularItemHeightTablet),
                                isTablet = true
                            )
                            if (index < middleNavItems.size - 1) {
                                Spacer(Modifier.height(itemSpacingTablet))
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        val aboutItem = items.last()
                        val aboutItemIndex = items.lastIndex
                        AnimatedNavigationItem(
                            item = aboutItem,
                            isSelected = selectedIndex == aboutItemIndex,
                            isExpanded = isNavExpanded,
                            onClick = { onItemSelected(aboutItemIndex) },
                            modifier = Modifier
                                .height(regularItemHeightTablet)
                                .onGloballyPositioned { layoutCoordinates ->
                                    val newY = with(density) { layoutCoordinates.positionInParent().y.toDp() }
                                    if (aboutItemActualY != newY) {
                                        aboutItemActualY = newY
                                    }
                                },
                            isTablet = true
                        )
                    }
                }
            }

            // 主内容区域 (这部分代码结构不变，只是移除了底部的 CommandBar)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp)
            ) {
                // 顶部命令栏
                TopTitleBarTablet(
                    title = items[selectedIndex].title
                )

                Box(modifier = Modifier.weight(1f)) {
                    ContentArea(pageIndex = selectedIndex)
                }
            }
        }

        // 3. 将 BottomCommandBarTablet 作为 Column 的第二个子项，它会被推到屏幕底部
        BottomCommandBarTablet(
            onNavigateBack = {
                if (selectedIndex > 0) onItemSelected(selectedIndex - 1)
            },
            onNavigateForward = {
                if (selectedIndex < items.size - 1) onItemSelected(selectedIndex + 1)
            },
            canNavigateBack = selectedIndex > 0,
            canNavigateForward = selectedIndex < items.size - 1
        )
    }
}

@Composable
fun TopTitleBarTablet(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun BottomCommandBarTablet(
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit,
    canNavigateBack: Boolean,
    canNavigateForward: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(onClick = onNavigateBack, enabled = canNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous",
                tint = if (canNavigateBack)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }

        Spacer(Modifier.width(12.dp))

        IconButton(onClick = onNavigateForward, enabled = canNavigateForward) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next",
                tint = if (canNavigateForward)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }

        Spacer(Modifier.weight(1f))

        IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "Search") }
        Spacer(Modifier.width(12.dp))
        IconButton(onClick = {}) { Icon(Icons.Default.Share, contentDescription = "Share") }
        Spacer(Modifier.width(12.dp))
        IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
    }
}


@Composable
fun NavigationPaneDesktop(
    items: List<NavItem>,
    fullUserProfile: NavItem,
    selectedIndex: Int,
    isExpanded: Boolean,
    onMenuToggle: () -> Unit,
    onItemSelected: (Int) -> Unit
) {
    val width by animateDpAsState(targetValue = if (isExpanded) 250.dp else 60.dp, animationSpec = animationSpecDp)
    val expandedProfileItemHeight = 72.dp
    val regularItemHeightDesktop = 48.dp
    val itemSpacingDesktop = 4.dp
    val animatedProfileHeight by animateDpAsState(
        targetValue = if (isExpanded) expandedProfileItemHeight else regularItemHeightDesktop,
        animationSpec = animationSpecDp
    )
    val density = LocalDensity.current
    var aboutItemActualY by remember { mutableStateOf(0.dp) }

    val indicatorOffsetY = remember(selectedIndex, isExpanded, aboutItemActualY) {
        derivedStateOf {
            val menuButtonTotalHeight = 64.dp
            val dividerTotalHeight = 16.dp

            when (selectedIndex) {
                0 -> menuButtonTotalHeight
                in 1 until items.size - 1 -> {
                    var offset = menuButtonTotalHeight
                    offset += if (isExpanded) expandedProfileItemHeight else regularItemHeightDesktop
                    offset += dividerTotalHeight
                    val middleItemIndex = selectedIndex - 1
                    offset += (regularItemHeightDesktop + itemSpacingDesktop) * middleItemIndex
                    offset
                }
                items.size - 1 -> aboutItemActualY
                else -> menuButtonTotalHeight
            }
        }
    }.value

    val animatedIndicatorOffsetY by animateDpAsState(targetValue = indicatorOffsetY, animationSpec = animationSpecDp)
    val indicatorHeight by animateDpAsState(
        targetValue = if (selectedIndex == 0) {
            if (isExpanded) expandedProfileItemHeight else regularItemHeightDesktop
        } else {
            regularItemHeightDesktop
        },
        animationSpec = animationSpecDp
    )

    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = animationSpecFloat
    )

    Surface(
        modifier = Modifier.width(width).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .offset(y = animatedIndicatorOffsetY)
                    .height(indicatorHeight)
                    .padding(vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)
                        )
                )
            }
            Column(modifier = Modifier.fillMaxSize()) {
                IconButton(
                    onClick = onMenuToggle,
                    modifier = Modifier.padding(start = 5.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Toggle Menu",
                        modifier = Modifier.rotate(arrowRotation)
                    )
                }
                AnimatedNavigationItem(
                    item = fullUserProfile,
                    isSelected = selectedIndex == 0,
                    isExpanded = isExpanded,
                    isProfileCard = true,
                    onClick = { onItemSelected(0) },
                    modifier = Modifier.height(animatedProfileHeight)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )
                val middleNavItems = items.subList(1, items.size - 1)
                middleNavItems.forEachIndexed { index, item ->
                    val absoluteIndex = index + 1
                    AnimatedNavigationItem(
                        item = item,
                        isSelected = selectedIndex == absoluteIndex,
                        isExpanded = isExpanded,
                        onClick = { onItemSelected(absoluteIndex) },
                        modifier = Modifier.height(regularItemHeightDesktop)
                    )
                    if (index < middleNavItems.size - 1) {
                        Spacer(Modifier.height(itemSpacingDesktop))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                val aboutItem = items.last()
                val aboutItemIndex = items.lastIndex
                AnimatedNavigationItem(
                    item = aboutItem,
                    isSelected = selectedIndex == aboutItemIndex,
                    isExpanded = isExpanded,
                    onClick = { onItemSelected(aboutItemIndex) },
                    modifier = Modifier
                        .height(regularItemHeightDesktop)
                        .onGloballyPositioned { layoutCoordinates ->
                            val newY = with(density) { layoutCoordinates.positionInParent().y.toDp() }
                            if (aboutItemActualY != newY) {
                                aboutItemActualY = newY
                            }
                        }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}


@Composable
fun NavigationPaneMobile(
    items: List<NavItem>,
    fullUserProfile: NavItem,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    var isNavOpen by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    var navContentSize by remember { mutableStateOf(IntSize.Zero) }
    val navContentHeight = with(density) { navContentSize.height.toDp() }
    val navPaneHeight = navContentHeight.coerceAtMost(400.dp)

    val totalBottomPaneHeight = navPaneHeight + commandBarHeight

    val bottomPaneOffsetY by animateDpAsState(
        targetValue = if (isNavOpen) 0.dp else navPaneHeight,
        animationSpec = animationSpecDp
    )

    val requiredBottomPadding = if (isNavOpen) totalBottomPaneHeight else commandBarHeight
    val animatedBottomPadding by animateDpAsState(requiredBottomPadding, animationSpec = animationSpecDp)

    // 定义移动端特有的快捷操作
    val quickActions = remember {
        listOf(
            Triple(Icons.Default.Search, "Search") { println("Search Clicked") },
            Triple(Icons.Default.Share, "Share") { println("Share Clicked") },
            Triple(Icons.Default.MoreVert, "More") { println("More Clicked") }
        )
    }

    Scaffold(
        topBar = {
            MobileTopBar(
                title = items[selectedIndex].title,
                userProfile = fullUserProfile,
                isNavOpen = isNavOpen
            )
        },
        bottomBar = {
            Box(modifier = Modifier.height(totalBottomPaneHeight)) {
                Column(
                    modifier = Modifier
                        .offset(y = bottomPaneOffsetY)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    MobileCommandBar(
                        isNavOpen = isNavOpen,
                        onNavOpen = { isNavOpen = !isNavOpen }
                    )
                    Box(
                        modifier = Modifier
                            .height(navPaneHeight)
                            .fillMaxWidth()
                            .clipToBounds()
                    ) {
                        MobileNavContent(
                            items = items,
                            selectedIndex = selectedIndex,
                            onItemSelected = {
                                onItemSelected(it)
                                isNavOpen = false
                            },
                            onContentSizeChanged = { navContentSize = it }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        // 使用 Box 叠加主内容和悬浮按钮
        Box(modifier = Modifier.fillMaxSize()) {
            ContentAreaMobile(Modifier.padding(top = paddingValues.calculateTopPadding()), pageIndex = selectedIndex)

            // bottom padding 随底部栏高度动态变化，确保始终在底部栏上方
            CircularQuickActions(
                actions = quickActions,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = animatedBottomPadding + 20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileTopBar(title: String, userProfile: NavItem, isNavOpen: Boolean) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        TopAppBar(
            modifier = Modifier.height(64.dp),
            title = {
                AnimatedContent(
                    targetState = title,
                    transitionSpec = {
                        // 定义进入和退出的动画：淡入 + 淡出
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                scaleIn(initialScale = 1f, animationSpec = tween(220, delayMillis = 90)))
                            .togetherWith(fadeOut(animationSpec = tween(90)))
                    },
                    label = "TitleTransition"
                ) { targetTitle ->
                    Text(
                        text = targetTitle,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                    },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
        AnimatedVisibility(visible = (isNavOpen || title == "个人")) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = userProfile.icon,
                    contentDescription = "User Profile",
                    modifier = Modifier.size(65.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(userProfile.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row {
                        userProfile.subtitle?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(//0xFF94DDF1
                                color = Color(0xFF1FC1EC).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(" 开发者 ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MobileNavContent(
    items: List<NavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onContentSizeChanged: (IntSize) -> Unit
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val itemPositions = remember { mutableStateMapOf<Int, Dp>() }

    val indicatorOffsetY by animateDpAsState(
        targetValue = itemPositions[selectedIndex] ?: 0.dp,
        animationSpec = animationSpecDp
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .onSizeChanged(onContentSizeChanged)
    ) {
        Box(
            modifier = Modifier
                .offset(y = indicatorOffsetY)
                .height(mobileRegularItemHeight)
                .padding(vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)
                    )
            )
        }
        Column {
            items.forEachIndexed { index, item ->
                AnimatedNavigationItem(
                    item = item,
                    isSelected = selectedIndex == index,
                    isExpanded = true,
                    showText = true,
                    onClick = { onItemSelected(index) },
                    modifier = Modifier
                        .height(mobileRegularItemHeight)
                        .onGloballyPositioned {
                            val newY = with(density) { it.positionInParent().y.toDp() }
                            if (itemPositions[index] != newY) {
                                itemPositions[index] = newY
                            }
                        }
                )
                if (index < items.size - 1) {
                    Spacer(Modifier.height(mobileitemSpacing))
                }
            }
        }
    }
}

@Composable
private fun MobileCommandBar(isNavOpen: Boolean, onNavOpen: () -> Unit) {

    val arrowRotation by animateFloatAsState(
        targetValue = if (isNavOpen) -90f else 0f,
        animationSpec = animationSpecFloat
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(commandBarHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onNavOpen) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Open Navigation",
                modifier = Modifier.rotate(arrowRotation)
            )
        }
    }
}

@Composable
fun AnimatedNavigationItem(
    item: NavItem,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isProfileCard: Boolean = false,
    showText: Boolean = true,
    isTablet: Boolean = false
) {
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val titleStyle = if (isProfileCard) {
        if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium
    } else {
        if (isTablet) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyLarge
    }
    val subtitleStyle = if (isProfileCard) {
        if (isTablet) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
    } else {
        MaterialTheme.typography.bodySmall
    }
    val expandedProfileIconSize = if (isTablet) 60.dp else 52.dp
    val regularIconSize = if (isTablet) 28.dp else 24.dp
    val animatedIconSize by animateDpAsState(
        targetValue = if (isProfileCard && isExpanded) expandedProfileIconSize else regularIconSize,
        animationSpec = animationSpecDp
    )
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val startPadding = if (isTablet) 20.dp else 16.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (isTablet) 10.dp else 7.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = if (isProfileCard) Color.Unspecified else contentColor,
            modifier = Modifier.size(animatedIconSize)
        )

        AnimatedVisibility(
            visible = isExpanded && showText,
            modifier = Modifier.padding(start = startPadding)
        ) {
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = item.title,
                    color = contentColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    style = titleStyle
                )

                item.subtitle?.let {
                    Text(
                        text = it,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        style = subtitleStyle
                    )
                }
            }
        }
    }
}

@Composable
fun ContentArea(modifier: Modifier = Modifier, pageIndex: Int) {
    //数据初始化
    val activities = SampleData.activitiesList
    val allAircraft = SampleData.aircraftList

    var expandedStat by remember { mutableStateOf<String?>(null) }
    val colorA = Color(0xFFFFEBEE)
    val colorB = Color(0xFFE3F2FD)
    val cardGray = Color(0xFFE8E8E8)
    val animatedDetailColor by animateColorAsState(
        targetValue = when (expandedStat) {
            "A"  -> colorA
            "B"  -> colorB
            else -> cardGray
        },
        label = "detailColor"
    )

    val haptic = LocalHapticFeedback.current

    var hoveredWaypointIndex by remember { mutableStateOf<Int?>(null) }
    var displayIndex by remember { mutableStateOf<Int?>(null) }

    val timelineState = rememberLazyListState()
    val density = LocalDensity.current
    var isTimelineActive by remember { mutableStateOf(false) }
    val snapBehavior = rememberSnapFlingBehavior(
        snapLayoutInfoProvider = remember(timelineState) {
            object : SnapLayoutInfoProvider {
                override fun calculateSnapOffset(velocity: Float): Float {
                    val layoutInfo = timelineState.layoutInfo
                    val visibleItems = layoutInfo.visibleItemsInfo
                    if (visibleItems.isEmpty()) return 0f
                    val containerWidth = layoutInfo.viewportSize.width
                    val triggerPoint = containerWidth - with(density) { (92.dp - 12.dp).toPx() }
                    val closestItem = visibleItems.minByOrNull { item ->
                        val itemCenter = item.offset + (item.size / 2)
                        kotlin.math.abs(itemCenter - triggerPoint)
                    } ?: return 0f
                    val currentItemCenter = closestItem.offset + (closestItem.size / 2)
                    return currentItemCenter - triggerPoint
                }
            }
        }
    )

    var aircraftColumnCount by remember { mutableIntStateOf(1) }
    var aircraftSearchQuery by remember { mutableStateOf("") }
    val filteredAircraft by remember {
        derivedStateOf {
            val q = aircraftSearchQuery.trim()
            if (q.isEmpty()) allAircraft
            else allAircraft.filter {
                it.id.contains(q, ignoreCase = true) ||
                        it.model.contains(q, ignoreCase = true) ||
                        it.mission.contains(q, ignoreCase = true)
            }
        }
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AnimatedContent(
            targetState = pageIndex,
            transitionSpec = {
                (slideInVertically { height -> height } + fadeIn(animationSpec = tween(400)))
                    .togetherWith(slideOutVertically { height -> -height } + fadeOut(animationSpec = tween(400)))
                    .using(SizeTransform(clip = false))

            },
            label = "PageTransition"
        ) { targetPageIndex ->
            if (targetPageIndex == 0) {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(span = { GridItemSpan(6) }) {
                            Text(
                                "基本进度",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }

                        item(span = { GridItemSpan(6) }) {
                            val anyExpanded = expandedStat != null
                            val isASelected = expandedStat == "A"
                            val isBSelected = expandedStat == "B"
                            val animatedColorA by animateColorAsState(targetValue = if (isASelected) colorA else cardGray)
                            val animatedColorB by animateColorAsState(targetValue = if (isBSelected) colorB else cardGray)
                            val weightA by animateFloatAsState(
                                targetValue = when {
                                    !anyExpanded -> 1f; isASelected -> 0.7f; else -> 1.3f
                                },
                                animationSpec = tween(300), label = "weightA"
                            )
                            val weightB by animateFloatAsState(
                                targetValue = when {
                                    !anyExpanded -> 1f; isBSelected -> 0.7f; else -> 1.3f
                                },
                                animationSpec = tween(300), label = "weightB"
                            )
                            val heightFactorA by animateFloatAsState(
                                targetValue = when {
                                    !anyExpanded -> 1.5f; else -> 3.1f
                                },
                                animationSpec = tween(300), label = "heightFactorA"
                            )
                            val heightFactorB by animateFloatAsState(
                                targetValue = when {
                                    !anyExpanded -> 1.5f; else -> 3.1f
                                },
                                animationSpec = tween(300), label = "heightFactorB"
                            )
                            val compensatedRatioA = heightFactorA * weightA
                            val compensatedRatioB = heightFactorB * weightB

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(weightA)) {
                                    val bottomCorner by animateDpAsState(
                                        targetValue = if (isASelected) 0.dp else 7.dp,
                                        label = "cornerA"
                                    )
                                    val bottomPadding by animateDpAsState(
                                        targetValue = if (isASelected) 1.dp else 7.dp,
                                        animationSpec = tween(300),
                                        label = "paddingA"
                                    )
                                    val startPadding by animateDpAsState(
                                        targetValue = if (isASelected) 16.dp else 8.dp,
                                        animationSpec = tween(300),
                                        label = "startPaddingA"
                                    )
                                    StatCard(
                                        label = "正在航行",
                                        value = "05 / 16",
                                        icon = Icons.Default.Favorite,
                                        containerColor = animatedColorA,
                                        onClick = {
                                            expandedStat = if (isASelected) null else "A"; haptic.performHapticFeedback(
                                            HapticFeedbackType.ContextClick
                                        )
                                        },
                                        shape = RoundedCornerShape(
                                            topStart = 7.dp,
                                            topEnd = 7.dp,
                                            bottomStart = bottomCorner,
                                            bottomEnd = bottomCorner
                                        ),
                                        ratio = compensatedRatioA,
                                        bottomPadding = bottomPadding,
                                        startPadding = startPadding
                                    )
                                }
                                Box(modifier = Modifier.weight(weightB)) {
                                    val bottomCorner by animateDpAsState(
                                        targetValue = if (isBSelected) 0.dp else 7.dp,
                                        label = "cornerB"
                                    )
                                    val bottomPadding by animateDpAsState(
                                        targetValue = if (isBSelected) 1.dp else 7.dp,
                                        animationSpec = tween(300),
                                        label = "paddingB"
                                    )
                                    val startPadding by animateDpAsState(
                                        targetValue = if (isASelected) 16.dp else 8.dp,
                                        animationSpec = tween(300),
                                        label = "startPaddingB"
                                    )
                                    StatCard(
                                        label = "最低续航",
                                        value = "12h 32m",
                                        icon = Icons.Default.Person,
                                        containerColor = animatedColorB,
                                        onClick = {
                                            expandedStat = if (isBSelected) null else "B"; haptic.performHapticFeedback(
                                            HapticFeedbackType.ContextClick
                                        )
                                        },
                                        shape = RoundedCornerShape(
                                            topStart = 7.dp,
                                            topEnd = 7.dp,
                                            bottomStart = bottomCorner,
                                            bottomEnd = bottomCorner
                                        ),
                                        ratio = compensatedRatioB,
                                        bottomPadding = bottomPadding,
                                        startPadding = startPadding
                                    )
                                }
                            }
                        }

                        item(span = { GridItemSpan(6) }) {
                            AnimatedVisibility(
                                visible = expandedStat != null,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut(),
                                modifier = Modifier.fillMaxWidth().offset(y = -4.dp),
                            ) {
                                val isLeftExpanded = expandedStat == "A"
                                DetailCard(
                                    modifier = Modifier.fillMaxWidth().aspectRatio(2.6f),
                                    containerColor = animatedDetailColor,
                                    shape = if (isLeftExpanded)
                                        RoundedCornerShape(
                                            topStart = 0.dp,
                                            topEnd = 7.dp,
                                            bottomStart = 7.dp,
                                            bottomEnd = 7.dp
                                        )
                                    else
                                        RoundedCornerShape(
                                            topStart = 7.dp,
                                            topEnd = 0.dp,
                                            bottomStart = 7.dp,
                                            bottomEnd = 7.dp
                                        ),
                                    content = {
                                        Text(
                                            text = if (expandedStat == "A") "航行状态" else "续航状态",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        val mockData =
                                            remember { List(48) { FlightStatus.entries.toTypedArray().random() } }
                                        AnimatedContent(
                                            targetState = expandedStat,
                                            transitionSpec = {
                                                if (targetState == null || initialState == null) {
                                                    fadeIn() togetherWith fadeOut()
                                                } else {
                                                    val isForward = targetState == "B"
                                                    if (isForward) {
                                                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                                                slideOutHorizontally { width -> width } + fadeOut()
                                                    } else {
                                                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                                                slideOutHorizontally { width -> -width } + fadeOut()
                                                    }.using(SizeTransform(clip = false))
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().weight(1f)
                                        ) { target ->
                                            if (target == "A") {
                                                FlightStatusSection(
                                                    data = mockData,
                                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                                )
                                            } else {
                                                val mockAircraft = remember {
                                                    List(32) { index ->
                                                        AircraftEndurance(
                                                            id = "AC-${index + 1}",
                                                            enduranceMinutes = (400..900).random()
                                                        )
                                                    }
                                                }
                                                AircraftEnduranceSection(
                                                    data = mockAircraft,
                                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        item(span = { GridItemSpan(6) }) {
                            Text(
                                "详细信息",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 12.dp, start = 4.dp, bottom = 4.dp)
                            )
                        }

                        item(span = { GridItemSpan(6) }) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(7.dp))
                                    .background(cardGray).padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "任务进度",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.Gray
                                        )
                                        Text(
                                            "75%",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { 0.75f },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                                    )
                                }
                            }
                        }


                        item(span = { GridItemSpan(6) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(4f)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(7.dp)).background(Color(0xFFFFF9C4)).clickable { }, contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Star, null); Text("收藏", style = MaterialTheme.typography.labelSmall) }
                                            }
                                            Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(7.dp)).background(cardGray).clickable { }, contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Settings, null); Text("设置", style = MaterialTheme.typography.labelSmall) }
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(7.dp)).background(Color(0xFFE1F5FE)).clickable { }, contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Build, null); Text("勋章", style = MaterialTheme.typography.labelSmall) }
                                            }
                                            Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(7.dp)).background(cardGray).clickable {
                                                activities.add(ActivityLog("抵达 WP-1", ActivityType.LIFE, Clock.System.now().toEpochMilliseconds()))
                                            }, contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Build, null); Text("睡觉", style = MaterialTheme.typography.labelSmall) }
                                            }
                                        }
                                    }
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = hoveredWaypointIndex != null && displayIndex != null,
                                        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                                        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(7.dp))
                                                .padding(16.dp)
                                        ) {
                                            AnimatedContent(
                                                targetState = displayIndex,
                                                transitionSpec = {
                                                    if ((targetState ?: 0) > (initialState ?: 0))
                                                        slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                                                    else
                                                        slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                                                }
                                            ) { target ->
                                                if (target != null) {
                                                    Column {
                                                        Text("位置详情", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                                        Text("WP-$target", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                                        Spacer(Modifier.weight(1f))
                                                        Text("坐标: ${30.123}, ${120.456}", style = MaterialTheme.typography.bodySmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                data class Waypoint(val id: Int, val distanceToNext: Float)
                                val allWaypoints = remember { List(20) { i -> Waypoint(id = i + 1, distanceToNext = (50..300).random().toFloat()) } }
                                var currentIndex by remember { mutableIntStateOf(1) }
                                val nodePositions = remember { mutableMapOf<Int, Float>() }

                                Box(
                                    modifier = Modifier.weight(2.6f).fillMaxHeight().clip(RoundedCornerShape(7.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(7.dp))
                                        .padding(8.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxSize()) {
                                        Column(
                                            modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 4.dp),
                                            verticalArrangement = Arrangement.SpaceBetween,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                                                Text("NEXT WP", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                                                Text(text = "WP-${currentIndex.toString().padStart(2, '0')}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                            }
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(75.dp)) {
                                                val currentDistance = allWaypoints.getOrNull(currentIndex)?.distanceToNext ?: 0f
                                                CircularProgressIndicator(progress = { 0.7f }, modifier = Modifier.fillMaxSize(), strokeWidth = 5.dp, trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                                                    Text(text = currentDistance.toInt().toString(), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 1.em)
                                                    Text(text = "m", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(start = 1.dp, bottom = 2.dp))
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (currentIndex < allWaypoints.size - 2) currentIndex++ else currentIndex = 1
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                },
                                                modifier = Modifier.align(Alignment.Start).size(32.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next", modifier = Modifier.size(26.dp))
                                            }
                                        }
                                        Column(
                                            modifier = Modifier.fillMaxHeight().padding(top = 6.dp, bottom = 6.dp, start = 8.dp, end = 8.dp)
                                                .pointerInput(Unit) {
                                                    awaitPointerEventScope {
                                                        while (true) {
                                                            val event = awaitPointerEvent()
                                                            val pointer = event.changes.first()
                                                            val pos = pointer.position
                                                            val isInNodeZone = pos.x < 45.dp.toPx()
                                                            if (pointer.pressed && isInNodeZone) {
                                                                pointer.consume()
                                                                val closest = nodePositions.minByOrNull { kotlin.math.abs(it.value - pos.y) }
                                                                if (closest != null && kotlin.math.abs(closest.value - pos.y) < 60f) {
                                                                    if (displayIndex != closest.key) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    displayIndex = closest.key
                                                                    hoveredWaypointIndex = closest.key
                                                                }
                                                            } else {
                                                                if (hoveredWaypointIndex != null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                hoveredWaypointIndex = null
                                                                pointer.consume()
                                                            }
                                                        }
                                                    }
                                                },
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            val wNext2 by animateFloatAsState(targetValue = ((allWaypoints.getOrNull(currentIndex + 1)?.distanceToNext ?: 100f) / 50f).coerceIn(0.5f, 5f), animationSpec = tween(450))
                                            val wNext1 by animateFloatAsState(targetValue = ((allWaypoints.getOrNull(currentIndex)?.distanceToNext ?: 100f) / 50f).coerceIn(0.5f, 5f), animationSpec = tween(450))
                                            val wPast  by animateFloatAsState(targetValue = ((allWaypoints.getOrNull(currentIndex - 1)?.distanceToNext ?: 50f) / 50f).coerceIn(0.5f, 2f), animationSpec = tween(450))
                                            Box(modifier = Modifier.size(8.dp).border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape).onGloballyPositioned { nodePositions[currentIndex + 2] = it.positionInParent().y })
                                            Box(modifier = Modifier.width(3.dp).weight(wNext2).background(Color.Gray.copy(alpha = 0.3f)))
                                            Box(modifier = Modifier.size(10.dp).border(1.dp, Color.Gray, CircleShape).background(MaterialTheme.colorScheme.surface, CircleShape).onGloballyPositioned { nodePositions[currentIndex + 1] = it.positionInParent().y })
                                            Box(modifier = Modifier.width(3.dp).weight(wNext1).background(Color.Gray.copy(alpha = 0.3f)))
                                            Box(modifier = Modifier.size(18.dp).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape).padding(3.dp).background(MaterialTheme.colorScheme.primary, CircleShape).onGloballyPositioned { nodePositions[currentIndex] = it.positionInParent().y })
                                            Box(modifier = Modifier.width(4.dp).weight(wPast).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)))
                                            Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape).onGloballyPositioned { nodePositions[currentIndex - 1] = it.positionInParent().y })
                                        }
                                    }
                                }
                            }
                        }


                        item(span = { GridItemSpan(6) }) {
                            Text("最近动态", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, start = 4.dp, bottom = 4.dp))
                        }

                        item(span = { GridItemSpan(6) }) {
                            val activeActivity by remember {
                                derivedStateOf {
                                    val layoutInfo = timelineState.layoutInfo
                                    val visibleItems = layoutInfo.visibleItemsInfo
                                    if (visibleItems.isEmpty()) return@derivedStateOf null
                                    val containerWidth = layoutInfo.viewportSize.width
                                    val triggerPoint = containerWidth - with(density) { (92.dp - 12.dp).toPx() }
                                    visibleItems.minByOrNull { item ->
                                        val itemCenter = item.offset + (item.size / 2)
                                        kotlin.math.abs(itemCenter - triggerPoint)
                                    }?.let { activities.getOrNull(it.index) }
                                }
                            }
                            LaunchedEffect(activeActivity) {
                                if (activeActivity != null && isTimelineActive) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            if (event.changes.first().pressed) isTimelineActive = true
                                        }
                                    }
                                }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                        .background(cardGray.copy(alpha = 0.3f)).padding(vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.align(Alignment.TopEnd).padding(end = 87.dp).size(24.dp).offset(y = (-19).dp)
                                    )
                                    LazyRow(
                                        state = timelineState,
                                        flingBehavior = snapBehavior,
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        itemsIndexed(items = activities, key = { _, item -> item.timestamp }) { index, activity ->
                                            TimelineItem(
                                                activity = activity,
                                                isFirst = index == 0,
                                                isLast = index == activities.size - 1,
                                                isSelected = activeActivity == activity,
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = isTimelineActive,
                                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                                ) {
                                    AnimatedContent(
                                        targetState = activeActivity,
                                        transitionSpec = {
                                            val initialIndex = activities.indexOf(initialState)
                                            val targetIndex = activities.indexOf(targetState)
                                            if (targetIndex > initialIndex)
                                                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                                            else
                                                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                                                    .using(SizeTransform(clip = false))
                                        },
                                        label = "ActivityDetail"
                                    ) { target ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().aspectRatio(3f),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8E8E8)),
                                            shape = RoundedCornerShape(7.dp)
                                        ) {
                                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                                }
                                                Spacer(Modifier.width(12.dp))
                                                Column {
                                                    Text(text = target?.title ?: "滑动查看动态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                    Text(text = if (target != null) "记录时间: ${target.timestamp.toFormattedTime()}" else "请左右滑动上方时间轴", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }


                        item(span = { GridItemSpan(6) }) {
                            Text(
                                "飞机详情",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 12.dp, start = 4.dp, bottom = 4.dp)
                            )
                        }



                        item(span = { GridItemSpan(6) }) {
                            AircraftSearchAndToggle(
                                query = aircraftSearchQuery,
                                onQueryChange = { aircraftSearchQuery = it },
                                columnCount = aircraftColumnCount,
                                onColumnChange = {
                                    if (aircraftColumnCount != it) {
                                        aircraftColumnCount = it
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            )
                        }
// --- 空状态提示 ---
                        item(span = { GridItemSpan(6) }) {
                            AnimatedVisibility(
                                visible = filteredAircraft.isEmpty(),
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(cardGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "未找到匹配的飞机",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                        // ============================================================
                        // 【核心修改：将列表区域作为整体进行淡入淡出左右滑动动画】
                        // ============================================================
                        item(span = { GridItemSpan(6) }) {
                            AnimatedContent(
                                targetState = aircraftColumnCount,
                                transitionSpec = {
                                    // 根据列数增加或减少决定滑动方向
                                    if (targetState > initialState) {
                                        // 1列 -> 2列：新页面从右边滑入，旧页面向左边滑出
                                        (slideInHorizontally { it / 2 } + fadeIn(tween(300)))
                                            .togetherWith(slideOutHorizontally { -it / 2 } + fadeOut(tween(300)))
                                    } else {
                                        // 2列 -> 1列：新页面从左边滑入，旧页面向右边滑出
                                        (slideInHorizontally { -it / 2 } + fadeIn(tween(300)))
                                            .togetherWith(slideOutHorizontally { it / 2 } + fadeOut(tween(300)))
                                    }.using(SizeTransform(clip = false))
                                },
                                label = "AircraftListSwitch"
                            ) { currentCols ->
                                // 根据当前选中的列数，渲染完全不同的布局页面
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (currentCols == 1) {
                                        // --- 单列页面布局 ---
                                        filteredAircraft.forEach { aircraft ->
                                            AircraftDetailCard(
                                                aircraft = aircraft,
                                                isCompact = false
                                            )
                                        }
                                    } else {
                                        // --- 双列页面布局 ---
                                        val rows = filteredAircraft.chunked(2)
                                        rows.forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                rowItems.forEach { aircraft ->
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        AircraftDetailCard(
                                                            aircraft = aircraft,
                                                            isCompact = true
                                                        )
                                                    }
                                                }
                                                // 如果最后一行只有一个，补齐空间
                                                if (rowItems.size < 2) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }

                                    if (filteredAircraft.isEmpty()) {
                                        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                            Text("未找到匹配结果", color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }

                        // 底部留白
                        item(span = { GridItemSpan(6) }) {
                            Spacer(Modifier.height(36.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(
                        modifier = Modifier
                            .weight(2f)
                            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                            .fillMaxSize()
                    ) {
                        Text(
                            "全局地图",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        var mousePosition by remember { mutableStateOf<Offset?>(null) }

                        val squareSize = 30f // 中心小正方形的边长
                        val lineColor = Color.White // 白色在地图上通常更清晰
                        val lineWidth = 1.dp

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color.LightGray)
                                // 使用 pointerInput 替代直接的 onPointerEvent
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            // 检查事件类型
                                            when (event.type) {
                                                PointerEventType.Move, PointerEventType.Enter -> {
                                                    // 获取第一个手指或鼠标的位置
                                                    mousePosition = event.changes.first().position
                                                }
                                                PointerEventType.Exit -> {
                                                    mousePosition = null
                                                }
                                            }
                                        }
                                    }
                                }
                                .drawWithContent {
                                    drawContent() // 绘制底层图片

                                    mousePosition?.let { pos ->
                                        val halfSize = squareSize / 2
                                        val strokeWidth = lineWidth.toPx()

                                        // 1. 绘制中心空心小正方形
                                        drawRect(
                                            color = lineColor,
                                            topLeft = Offset(pos.x - halfSize, pos.y - halfSize),
                                            size = Size(squareSize, squareSize),
                                            style = Stroke(width = strokeWidth)
                                        )

                                        // 2. 绘制十字指示线 (跳过中间正方形区域)
                                        // 垂直线 - 上段
                                        drawLine(
                                            color = lineColor,
                                            start = Offset(pos.x, 0f),
                                            end = Offset(pos.x, pos.y - halfSize),
                                            strokeWidth = strokeWidth
                                        )
                                        // 垂直线 - 下段
                                        drawLine(
                                            color = lineColor,
                                            start = Offset(pos.x, pos.y + halfSize),
                                            end = Offset(pos.x, size.height),
                                            strokeWidth = strokeWidth
                                        )
                                        // 水平线 - 左段
                                        drawLine(
                                            color = lineColor,
                                            start = Offset(0f, pos.y),
                                            end = Offset(pos.x - halfSize, pos.y),
                                            strokeWidth = strokeWidth
                                        )
                                        // 水平线 - 右段
                                        drawLine(
                                            color = lineColor,
                                            start = Offset(pos.x + halfSize, pos.y),
                                            end = Offset(size.width, pos.y),
                                            strokeWidth = strokeWidth
                                        )
                                    }
                                }
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.map),
                                contentDescription = "Aircraft Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            } else {

            }
        }
    }
}


// DrawScope 的扩展：绘制十字线并在小方块区域留空，再绘制空心方块边框
private fun DrawScope.drawCrossAndHollowSquare(
    px: Float,
    py: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    indicatorColor: Color,
    strokeWidth: Float,
    squarePxSize: Float
) {
    // 计算小方块矩形（以鼠标位置为中心）
    val half = squarePxSize / 2f
    val left = px - half
    val top = py - half
    val right = px + half
    val bottom = py + half

    // 画十字线：两条贯穿整个区域，但在方块区域不绘制
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Square)

    // 水平线：从左到方块左侧，从方块右侧到右
    if (top < canvasHeight && bottom > 0f) {
        // 左段
        drawLine(
            color = indicatorColor,
            start = androidx.compose.ui.geometry.Offset(0f, py),
            end = androidx.compose.ui.geometry.Offset(maxOf(0f, left), py),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Square
        )
        // 右段
        drawLine(
            color = indicatorColor,
            start = androidx.compose.ui.geometry.Offset(minOf(canvasWidth, right), py),
            end = androidx.compose.ui.geometry.Offset(canvasWidth, py),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Square
        )
    }

    // 垂直线：从上到方块上侧，从方块下侧到下
    if (left < canvasWidth && right > 0f) {
        // 上段
        drawLine(
            color = indicatorColor,
            start = androidx.compose.ui.geometry.Offset(px, 0f),
            end = androidx.compose.ui.geometry.Offset(px, maxOf(0f, top)),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Square
        )
        // 下段
        drawLine(
            color = indicatorColor,
            start = androidx.compose.ui.geometry.Offset(px, minOf(canvasHeight, bottom)),
            end = androidx.compose.ui.geometry.Offset(px, canvasHeight),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Square
        )
    }

    // 画空心小方块（只画边框，不填充）
    drawRect(
        color = indicatorColor,
        topLeft = androidx.compose.ui.geometry.Offset(left, top),
        size = Size(squarePxSize, squarePxSize),
        style = stroke
    )
}
// =====================================================
// 新增 Data Class / Enum（文件顶层定义）
// =====================================================

data class AircraftInfo(
    val id: String,
    val model: String,
    val mission: String,
    val lat: Double,
    val lon: Double,
    val altitude: Int,
    val speed: Int,
    val fuel: Float,
    val signalStrength: Float, // 0.0 - 1.0
    val status: AircraftStatus
)

enum class AircraftStatus(val label: String) {
    ACTIVE("执行中"),
    STANDBY("待命"),
    MAINTENANCE("维护中")
}

val AircraftStatus.statusColor: Color
    get() = when (this) {
        AircraftStatus.ACTIVE      -> Color(0xFF4CAF50)
        AircraftStatus.STANDBY     -> Color(0xFF2196F3)
        AircraftStatus.MAINTENANCE -> Color(0xFFFF9800)
    }



// =====================================================
// 改进后的 ContentAreaMobile（完整函数）
// =====================================================

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ContentAreaMobile(modifier: Modifier = Modifier, pageIndex: Int) {
    //数据初始化
    val activities = SampleData.activitiesList
    val allAircraft = SampleData.aircraftList


    val haptic = LocalHapticFeedback.current
    var isTimelineActive by remember { mutableStateOf(false) }

    var hoveredWaypointIndex by remember { mutableStateOf<Int?>(null) }
    var displayIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(hoveredWaypointIndex) {
        if (hoveredWaypointIndex == null) {
            delay(300)
            displayIndex = null
        }
    }

    var expandedStat by remember { mutableStateOf<String?>(null) }
    val gridSpacing = 4.dp

    // ---- 飞机详情新增状态 ----
    var aircraftColumnCount by remember { mutableIntStateOf(1) }
    var aircraftSearchQuery by remember { mutableStateOf("") }

    val filteredAircraft by remember {
        derivedStateOf {
            val q = aircraftSearchQuery.trim()
            if (q.isEmpty()) allAircraft
            else allAircraft.filter {
                it.id.contains(q, ignoreCase = true) ||
                        it.model.contains(q, ignoreCase = true) ||
                        it.mission.contains(q, ignoreCase = true)
            }
        }
    }

    val timelineState = rememberLazyListState()
    val density = LocalDensity.current
    val isNearEnd by remember {
        derivedStateOf {
            val layoutInfo = timelineState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf false
            lastVisibleItem != null && lastVisibleItem >= totalItems - 2
        }
    }
    LaunchedEffect(activities.size) {
        if (activities.size > 1 && isNearEnd) {
            val distance = with(density) { (95.dp + 16.dp).toPx() }
            timelineState.animateScrollBy(
                value = distance,
                animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)
            )
        }
    }

    val snapBehavior = rememberSnapFlingBehavior(
        snapLayoutInfoProvider = remember(timelineState) {
            object : SnapLayoutInfoProvider {
                override fun calculateSnapOffset(velocity: Float): Float {
                    val layoutInfo = timelineState.layoutInfo
                    val visibleItems = layoutInfo.visibleItemsInfo
                    if (visibleItems.isEmpty()) return 0f
                    val containerWidth = layoutInfo.viewportSize.width
                    val triggerPoint = containerWidth - with(density) { (92.dp - 12.dp).toPx() }
                    val closestItem = visibleItems.minByOrNull { item ->
                        val itemCenter = item.offset + (item.size / 2)
                        kotlin.math.abs(itemCenter - triggerPoint)
                    } ?: return 0f
                    val currentItemCenter = closestItem.offset + (closestItem.size / 2)
                    return currentItemCenter - triggerPoint
                }
            }
        }
    )

    val cardGray = Color(0xFFE8E8E8)
    val colorA = Color(0xFFFFEBEE)
    val colorB = Color(0xFFE3F2FD)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val animatedDetailColor by animateColorAsState(
        targetValue = when (expandedStat) {
            "A"  -> colorA
            "B"  -> colorB
            else -> cardGray
        },
        label = "detailColor"
    )

    Column(modifier = modifier.fillMaxSize().background(surfaceColor)) {
        AnimatedContent(
            targetState = pageIndex,
            transitionSpec = {
                (slideInVertically { height -> height } + fadeIn(animationSpec = tween(400)))
                    .togetherWith(slideOutVertically { height -> -height } + fadeOut(animationSpec = tween(400)))
            },
            label = "PageTransition"
        ) { targetPageIndex ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.first()
                                if (change.pressed) isTimelineActive = false
                            }
                        }
                    },
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (targetPageIndex == 0) {
                    // =========================================
                    // 原有区域（保持不变）
                    // =========================================
                    item(span = { GridItemSpan(6) }) {
                        Text(
                            "基本进度",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }

                    item(span = { GridItemSpan(6) }) {
                        val anyExpanded = expandedStat != null
                        val isASelected = expandedStat == "A"
                        val isBSelected = expandedStat == "B"
                        val animatedColorA by animateColorAsState(targetValue = if (isASelected) colorA else cardGray)
                        val animatedColorB by animateColorAsState(targetValue = if (isBSelected) colorB else cardGray)
                        val weightA by animateFloatAsState(
                            targetValue = when { !anyExpanded -> 1f; isASelected -> 0.7f; else -> 1.3f },
                            animationSpec = tween(300), label = "weightA"
                        )
                        val weightB by animateFloatAsState(
                            targetValue = when { !anyExpanded -> 1f; isBSelected -> 0.7f; else -> 1.3f },
                            animationSpec = tween(300), label = "weightB"
                        )
                        val heightFactorA by animateFloatAsState(
                            targetValue = when { !anyExpanded -> 1.5f; else -> 3.1f },
                            animationSpec = tween(300), label = "heightFactorA"
                        )
                        val heightFactorB by animateFloatAsState(
                            targetValue = when { !anyExpanded -> 1.5f; else -> 3.1f },
                            animationSpec = tween(300), label = "heightFactorB"
                        )
                        val compensatedRatioA = heightFactorA * weightA
                        val compensatedRatioB = heightFactorB * weightB

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(weightA)) {
                                val bottomCorner by animateDpAsState(targetValue = if (isASelected) 0.dp else 7.dp, label = "cornerA")
                                val bottomPadding by animateDpAsState(targetValue = if (isASelected) 1.dp else 7.dp, animationSpec = tween(300), label = "paddingA")
                                val startPadding by animateDpAsState(targetValue = if (isASelected) 16.dp else 8.dp, animationSpec = tween(300), label = "startPaddingA")
                                StatCard(
                                    label = "正在航行", value = "05 / 16", icon = Icons.Default.Favorite, containerColor = animatedColorA,
                                    onClick = { expandedStat = if (isASelected) null else "A"; haptic.performHapticFeedback(HapticFeedbackType.ContextClick) },
                                    shape = RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = bottomCorner, bottomEnd = bottomCorner),
                                    ratio = compensatedRatioA, bottomPadding = bottomPadding, startPadding = startPadding
                                )
                            }
                            Box(modifier = Modifier.weight(weightB)) {
                                val bottomCorner by animateDpAsState(targetValue = if (isBSelected) 0.dp else 7.dp, label = "cornerB")
                                val bottomPadding by animateDpAsState(targetValue = if (isBSelected) 1.dp else 7.dp, animationSpec = tween(300), label = "paddingB")
                                val startPadding by animateDpAsState(targetValue = if (isASelected) 16.dp else 8.dp, animationSpec = tween(300), label = "startPaddingB")
                                StatCard(
                                    label = "最低续航", value = "12h 32m", icon = Icons.Default.Person, containerColor = animatedColorB,
                                    onClick = { expandedStat = if (isBSelected) null else "B"; haptic.performHapticFeedback(HapticFeedbackType.ContextClick) },
                                    shape = RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = bottomCorner, bottomEnd = bottomCorner),
                                    ratio = compensatedRatioB, bottomPadding = bottomPadding, startPadding = startPadding
                                )
                            }
                        }
                    }

                    item(span = { GridItemSpan(6) }) {
                        AnimatedVisibility(
                            visible = expandedStat != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                            modifier = Modifier.fillMaxWidth().offset(y = -gridSpacing),
                        ) {
                            val isLeftExpanded = expandedStat == "A"
                            DetailCard(
                                modifier = Modifier.fillMaxWidth().aspectRatio(2.6f),
                                containerColor = animatedDetailColor,
                                shape = if (isLeftExpanded)
                                    RoundedCornerShape(topStart = 0.dp, topEnd = 7.dp, bottomStart = 7.dp, bottomEnd = 7.dp)
                                else
                                    RoundedCornerShape(topStart = 7.dp, topEnd = 0.dp, bottomStart = 7.dp, bottomEnd = 7.dp),
                                content = {
                                    Text(
                                        text = if (expandedStat == "A") "航行状态" else "续航状态",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    val mockData = remember { List(48) { FlightStatus.entries.toTypedArray().random() } }
                                    AnimatedContent(
                                        targetState = expandedStat,
                                        transitionSpec = {
                                            if (targetState == null || initialState == null) {
                                                fadeIn() togetherWith fadeOut()
                                            } else {
                                                val isForward = targetState == "B"
                                                if (isForward) {
                                                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                                            slideOutHorizontally { width -> width } + fadeOut()
                                                } else {
                                                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                                                            slideOutHorizontally { width -> -width } + fadeOut()
                                                }.using(SizeTransform(clip = false))
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().weight(1f)
                                    ) { target ->
                                        if (target == "A") {
                                            FlightStatusSection(data = mockData, modifier = Modifier.fillMaxWidth().weight(1f))
                                        } else {
                                            val mockAircraft = remember {
                                                List(32) { index -> AircraftEndurance(id = "AC-${index + 1}", enduranceMinutes = (400..900).random()) }
                                            }
                                            AircraftEnduranceSection(data = mockAircraft, modifier = Modifier.fillMaxWidth().weight(1f))
                                        }
                                    }
                                }
                            )
                        }
                    }

                    item(span = { GridItemSpan(6) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(7.dp))
                                .background(cardGray).padding(16.dp)
                        ) {
                            Column {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("任务进度", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                                    Text("75%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { 0.75f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                                )
                            }
                        }
                    }

                    item(span = { GridItemSpan(6) }) {
                        Text(
                            "详细信息",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 12.dp, start = 4.dp, bottom = 4.dp)
                        )
                    }

                    item(span = { GridItemSpan(6) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(4f)) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(7.dp)).background(Color(0xFFFFF9C4)).clickable { }, contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Star, null); Text("收藏", style = MaterialTheme.typography.labelSmall) }
                                        }
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(7.dp)).background(cardGray).clickable { }, contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Settings, null); Text("设置", style = MaterialTheme.typography.labelSmall) }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(7.dp)).background(Color(0xFFE1F5FE)).clickable { }, contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Build, null); Text("勋章", style = MaterialTheme.typography.labelSmall) }
                                        }
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(7.dp)).background(cardGray).clickable {
                                            activities.add(ActivityLog("抵达 WP-1", ActivityType.LIFE, Clock.System.now().toEpochMilliseconds()))
                                        }, contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Build, null); Text("睡觉", style = MaterialTheme.typography.labelSmall) }
                                        }
                                    }
                                }
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = hoveredWaypointIndex != null && displayIndex != null,
                                    enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                                    exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(7.dp))
                                            .padding(16.dp)
                                    ) {
                                        AnimatedContent(
                                            targetState = displayIndex,
                                            transitionSpec = {
                                                if ((targetState ?: 0) > (initialState ?: 0))
                                                    slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                                                else
                                                    slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                                            }
                                        ) { target ->
                                            if (target != null) {
                                                Column {
                                                    Text("位置详情", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                                    Text("WP-$target", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                                    Spacer(Modifier.weight(1f))
                                                    Text("坐标: ${30.123}, ${120.456}", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            data class Waypoint(val id: Int, val distanceToNext: Float)
                            val allWaypoints = remember { List(20) { i -> Waypoint(id = i + 1, distanceToNext = (50..300).random().toFloat()) } }
                            var currentIndex by remember { mutableIntStateOf(1) }
                            val nodePositions = remember { mutableMapOf<Int, Float>() }

                            Box(
                                modifier = Modifier.weight(2.6f).fillMaxHeight().clip(RoundedCornerShape(7.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(7.dp))
                                    .padding(8.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 4.dp),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                                            Text("NEXT WP", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                                            Text(text = "WP-${currentIndex.toString().padStart(2, '0')}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                        }
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(75.dp)) {
                                            val currentDistance = allWaypoints.getOrNull(currentIndex)?.distanceToNext ?: 0f
                                            CircularProgressIndicator(progress = { 0.7f }, modifier = Modifier.fillMaxSize(), strokeWidth = 5.dp, trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                                                Text(text = currentDistance.toInt().toString(), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 1.em)
                                                Text(text = "m", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(start = 1.dp, bottom = 2.dp))
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                if (currentIndex < allWaypoints.size - 2) currentIndex++ else currentIndex = 1
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            },
                                            modifier = Modifier.align(Alignment.Start).size(32.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next", modifier = Modifier.size(26.dp))
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.fillMaxHeight().padding(top = 6.dp, bottom = 6.dp, start = 8.dp, end = 8.dp)
                                            .pointerInput(Unit) {
                                                awaitPointerEventScope {
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val pointer = event.changes.first()
                                                        val pos = pointer.position
                                                        val isInNodeZone = pos.x < 45.dp.toPx()
                                                        if (pointer.pressed && isInNodeZone) {
                                                            pointer.consume()
                                                            val closest = nodePositions.minByOrNull { kotlin.math.abs(it.value - pos.y) }
                                                            if (closest != null && kotlin.math.abs(closest.value - pos.y) < 60f) {
                                                                if (displayIndex != closest.key) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                displayIndex = closest.key
                                                                hoveredWaypointIndex = closest.key
                                                            }
                                                        } else {
                                                            if (hoveredWaypointIndex != null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            hoveredWaypointIndex = null
                                                            pointer.consume()
                                                        }
                                                    }
                                                }
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        val wNext2 by animateFloatAsState(targetValue = ((allWaypoints.getOrNull(currentIndex + 1)?.distanceToNext ?: 100f) / 50f).coerceIn(0.5f, 5f), animationSpec = tween(450))
                                        val wNext1 by animateFloatAsState(targetValue = ((allWaypoints.getOrNull(currentIndex)?.distanceToNext ?: 100f) / 50f).coerceIn(0.5f, 5f), animationSpec = tween(450))
                                        val wPast  by animateFloatAsState(targetValue = ((allWaypoints.getOrNull(currentIndex - 1)?.distanceToNext ?: 50f) / 50f).coerceIn(0.5f, 2f), animationSpec = tween(450))
                                        Box(modifier = Modifier.size(8.dp).border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape).onGloballyPositioned { nodePositions[currentIndex + 2] = it.positionInParent().y })
                                        Box(modifier = Modifier.width(3.dp).weight(wNext2).background(Color.Gray.copy(alpha = 0.3f)))
                                        Box(modifier = Modifier.size(10.dp).border(1.dp, Color.Gray, CircleShape).background(MaterialTheme.colorScheme.surface, CircleShape).onGloballyPositioned { nodePositions[currentIndex + 1] = it.positionInParent().y })
                                        Box(modifier = Modifier.width(3.dp).weight(wNext1).background(Color.Gray.copy(alpha = 0.3f)))
                                        Box(modifier = Modifier.size(18.dp).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape).padding(3.dp).background(MaterialTheme.colorScheme.primary, CircleShape).onGloballyPositioned { nodePositions[currentIndex] = it.positionInParent().y })
                                        Box(modifier = Modifier.width(4.dp).weight(wPast).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)))
                                        Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape).onGloballyPositioned { nodePositions[currentIndex - 1] = it.positionInParent().y })
                                    }
                                }
                            }
                        }
                    }

                    item(span = { GridItemSpan(6) }) {
                        Text("最近动态", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, start = 4.dp, bottom = 4.dp))
                    }

                    item(span = { GridItemSpan(6) }) {
                        val activeActivity by remember {
                            derivedStateOf {
                                val layoutInfo = timelineState.layoutInfo
                                val visibleItems = layoutInfo.visibleItemsInfo
                                if (visibleItems.isEmpty()) return@derivedStateOf null
                                val containerWidth = layoutInfo.viewportSize.width
                                val triggerPoint = containerWidth - with(density) { (92.dp - 12.dp).toPx() }
                                visibleItems.minByOrNull { item ->
                                    val itemCenter = item.offset + (item.size / 2)
                                    kotlin.math.abs(itemCenter - triggerPoint)
                                }?.let { activities.getOrNull(it.index) }
                            }
                        }
                        LaunchedEffect(activeActivity) {
                            if (activeActivity != null && isTimelineActive) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        if (event.changes.first().pressed) isTimelineActive = true
                                    }
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .background(cardGray.copy(alpha = 0.3f)).padding(vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 87.dp).size(24.dp).offset(y = (-19).dp)
                                )
                                LazyRow(
                                    state = timelineState,
                                    flingBehavior = snapBehavior,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    itemsIndexed(items = activities, key = { _, item -> item.timestamp }) { index, activity ->
                                        TimelineItem(
                                            activity = activity,
                                            isFirst = index == 0,
                                            isLast = index == activities.size - 1,
                                            isSelected = activeActivity == activity,
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isTimelineActive,
                                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                            ) {
                                AnimatedContent(
                                    targetState = activeActivity,
                                    transitionSpec = {
                                        val initialIndex = activities.indexOf(initialState)
                                        val targetIndex = activities.indexOf(targetState)
                                        if (targetIndex > initialIndex)
                                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                                        else
                                            (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                                                .using(SizeTransform(clip = false))
                                    },
                                    label = "ActivityDetail"
                                ) { target ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().aspectRatio(3f),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8E8E8)),
                                        shape = RoundedCornerShape(7.dp)
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(text = target?.title ?: "滑动查看动态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text(text = if (target != null) "记录时间: ${target.timestamp.toFormattedTime()}" else "请左右滑动上方时间轴", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(span = { GridItemSpan(6) }) {
                        Text(
                            "飞机详情",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 12.dp, start = 4.dp, bottom = 4.dp)
                        )
                    }



                    item(span = { GridItemSpan(6) }) {
                        AircraftSearchAndToggle(
                            query = aircraftSearchQuery,
                            onQueryChange = { aircraftSearchQuery = it },
                            columnCount = aircraftColumnCount,
                            onColumnChange = {
                                if (aircraftColumnCount != it) {
                                    aircraftColumnCount = it
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        )
                    }
// --- 空状态提示 ---
                    item(span = { GridItemSpan(6) }) {
                        AnimatedVisibility(
                            visible = filteredAircraft.isEmpty(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(cardGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "未找到匹配的飞机",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    // ============================================================
                    // 【核心修改：将列表区域作为整体进行淡入淡出左右滑动动画】
                    // ============================================================
                    item(span = { GridItemSpan(6) }) {
                        AnimatedContent(
                            targetState = aircraftColumnCount,
                            transitionSpec = {
                                // 根据列数增加或减少决定滑动方向
                                if (targetState > initialState) {
                                    // 1列 -> 2列：新页面从右边滑入，旧页面向左边滑出
                                    (slideInHorizontally { it / 2 } + fadeIn(tween(300)))
                                        .togetherWith(slideOutHorizontally { -it / 2 } + fadeOut(tween(300)))
                                } else {
                                    // 2列 -> 1列：新页面从左边滑入，旧页面向右边滑出
                                    (slideInHorizontally { -it / 2 } + fadeIn(tween(300)))
                                        .togetherWith(slideOutHorizontally { it / 2 } + fadeOut(tween(300)))
                                }.using(SizeTransform(clip = false))
                            },
                            label = "AircraftListSwitch"
                        ) { currentCols ->
                            // 根据当前选中的列数，渲染完全不同的布局页面
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (currentCols == 1) {
                                    // --- 单列页面布局 ---
                                    filteredAircraft.forEach { aircraft ->
                                        AircraftDetailCard(
                                            aircraft = aircraft,
                                            isCompact = false
                                        )
                                    }
                                } else {
                                    // --- 双列页面布局 ---
                                    val rows = filteredAircraft.chunked(2)
                                    rows.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowItems.forEach { aircraft ->
                                                Box(modifier = Modifier.weight(1f)) {
                                                    AircraftDetailCard(
                                                        aircraft = aircraft,
                                                        isCompact = true
                                                    )
                                                }
                                            }
                                            // 如果最后一行只有一个，补齐空间
                                            if (rowItems.size < 2) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }

                                if (filteredAircraft.isEmpty()) {
                                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                        Text("未找到匹配结果", color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    // 底部留白
                    item(span = { GridItemSpan(6) }) {
                        Spacer(Modifier.height(36.dp))
                    }

                } else {
                    items(48, span = { GridItemSpan(3) }) { itemIndex ->
                        Box(
                            modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(cardGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Item ${itemIndex + 1}", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AircraftDetailCard(
    aircraft: AircraftInfo,
    isCompact: Boolean,
) {
    val cardGray = Color(0xFFE8E8E8)
    val fuelColor = when {
        aircraft.fuel > 0.5f  -> Color(0xFF4CAF50)
        aircraft.fuel > 0.25f -> Color(0xFFFF9800)
        else                  -> Color(0xFFF44336)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(cardGray)
    ) {

        if (isCompact) {
            // --- 2 列紧凑布局 ---
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // 型号 + 状态
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = aircraft.model,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(aircraft.status.statusColor.copy(alpha = 0.13f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = aircraft.status.label,
                            fontSize = 9.sp,
                            color = aircraft.status.statusColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                // 编号 + 任务
                Text(
                    text = "${aircraft.id} · ${aircraft.mission}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp
                )
                // 坐标
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, null, tint = Color.Gray, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "${aircraft.lat}°N  ${aircraft.lon}°E",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
                // 高度 + 速度
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("↑${aircraft.altitude}m", fontSize = 9.sp, color = Color(0xFF388E3C))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFF3E0))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("${aircraft.speed}km/h", fontSize = 9.sp, color = Color(0xFFE65100))
                    }
                }
                // 分隔线
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.Gray.copy(alpha = 0.2f)))
                // 燃油
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("燃油", fontSize = 9.sp, color = Color.Gray)
                    Text(
                        "${(aircraft.fuel * 100).toInt()}%",
                        fontSize = 9.sp,
                        color = fuelColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { aircraft.fuel },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                    color = fuelColor,
                    trackColor = Color.Gray.copy(alpha = 0.15f)
                )
            }
        } else {
            // --- 1 列展开布局 ---
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 头部：图标 + 型号/编号 + 状态
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 状态指示块
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(aircraft.status.statusColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .background(aircraft.status.statusColor, CircleShape)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = aircraft.id,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = aircraft.model,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    // 状态 Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(aircraft.status.statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(Modifier.size(6.dp).background(aircraft.status.statusColor, CircleShape))
                            Text(
                                text = aircraft.status.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = aircraft.status.statusColor
                            )
                        }
                    }
                }

                // 任务/高度/速度 Tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier.clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            aircraft.mission,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text("↑ ${aircraft.altitude}m", style = MaterialTheme.typography.labelSmall, color = Color(0xFF388E3C))
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFFFF3E0))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text("${aircraft.speed}km/h", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                    }
                }

                // 分隔线
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.Gray.copy(alpha = 0.2f)))

                // 坐标
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Place, null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                    Text(
                        text = "${aircraft.lat}°N,  ${aircraft.lon}°E",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // 燃油 + 信号双行数据
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 燃油
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("燃油", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Text(
                                "${(aircraft.fuel * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = fuelColor
                            )
                        }
                        LinearProgressIndicator(
                            progress = { aircraft.fuel },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                            color = fuelColor,
                            trackColor = Color.Gray.copy(alpha = 0.15f)
                        )
                    }
                    // 信号
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("信号", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Text(
                                "${(aircraft.signalStrength * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { aircraft.signalStrength },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Gray.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }

    }
}

@Composable
fun AircraftSearchAndToggle(
    query: String,
    onQueryChange: (String) -> Unit,
    columnCount: Int,
    onColumnChange: (Int) -> Unit
) {
    val cardGray = Color(0xFFE8E8E8)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ✅ 搜索框
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(7.dp))
                .background(cardGray)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                Icon(
                    Icons.Default.Search,
                    null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(8.dp))

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    decorationBox = { innerTextField ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    "搜索型号、编号、任务...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        Icons.Default.Close,
                        null,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onQueryChange("") }
                    )
                }
            }
        }

        // ✅ 列切换
        // ✅ 修改后的列切换：点击整个大区域即可切换
        Box(
            modifier = Modifier
                .height(44.dp)
                .width(88.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(cardGray)
                .padding(4.dp)
                // 【关键改动 1】：将点击事件移到这里，点击整个大框直接切换
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null // 移除默认涟漪，因为我们有自定义的滑块动画
                ) {
                    // 如果是 1 就切到 2，如果是 2 就切到 1
                    onColumnChange(if (columnCount == 1) 2 else 1)
                }
        ) {
            // 【关键改动 2】：添加一个带动画的背景滑块
            // 计算滑块的偏移量：如果是 1 则在左边(0dp)，如果是 2 则在右边(40dp)
            val indicatorOffset by animateDpAsState(
                targetValue = if (columnCount == 1) 0.dp else 40.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )

            // 动画滑块主体
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .fillMaxHeight()
                    .width(40.dp) // 宽度约为总宽的一半减去 padding
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surface)
            )

            // 上层的文字显示
            Row(Modifier.fillMaxSize()) {
                listOf(1, 2).forEach { count ->
                    val selected = columnCount == count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = count.toString(),
                            // 文字颜色也做个简单的动画过渡
                            color = animateColorAsState(
                                if (selected) MaterialTheme.colorScheme.primary
                                else Color.Gray
                            ).value,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

    }
}
data class AircraftEndurance(
    val id: String,
    val enduranceMinutes: Int
)
fun Int.toHourMinute(): String {
    val h = this / 60
    val m = this % 60
    return "${h}h ${m}m"
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AircraftEnduranceSection(
    data: List<AircraftEndurance>,
    modifier: Modifier = Modifier
) {
    var ascending by remember { mutableStateOf(true) }

    val sortedData = remember(data, ascending) {
        if (ascending)
            data.sortedBy { it.enduranceMinutes }
        else
            data.sortedByDescending { it.enduranceMinutes }
    }

    val maxValue = sortedData.maxOfOrNull { it.enduranceMinutes } ?: 1
    val minValue = sortedData.minOfOrNull { it.enduranceMinutes } ?: 0

    Column(modifier = modifier) {

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),   // ✅ 两列
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sortedData.size) { index ->
                val aircraft = sortedData[index]

                val ratio = aircraft.enduranceMinutes / maxValue.toFloat()
                val isLowest = aircraft.enduranceMinutes == minValue
                val isCritical = aircraft.enduranceMinutes < 300

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = aircraft.id,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = aircraft.enduranceMinutes.toHourMinute(),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.End,
                            fontWeight = if (isLowest) FontWeight.Bold else FontWeight.Normal,
                            color = Color.Unspecified
                        )
                    }
                    Spacer(Modifier.height(3.dp))

                    SegmentedEnergyBar(
                        ratio = ratio,
                        segments = 5,
                        isCritical = isCritical,
                        isLowest = isLowest
                    )
                }
            }
        }
    }
}
@Composable
fun SegmentedEnergyBar(
    ratio: Float,
    segments: Int = 5,
    isCritical: Boolean,
    isLowest: Boolean
) {
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(600),
        label = "energyAnim"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(segments) { index ->

            val filled = index < (animatedRatio * segments)

            val baseColor = when {
                isCritical -> Color(0xFFDE9B99)
                ratio < 0.5f -> Color(0xFFFFD6A0)
                else -> Color(0xFFA1ECA4)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (filled) baseColor else Color.LightGray.copy(alpha = 0.15f))
            )
        }
    }
}
enum class FlightStatus {
    GROUND,       // 地勤
    STANDBY,      // 待机
    CRUISE        // 巡航
}
val StatusColorMap = mapOf(
    FlightStatus.GROUND to Color(0xFFFAABAA),
    FlightStatus.STANDBY to Color(0xFFCECECE),
    FlightStatus.CRUISE to Color(0xFFB0E3B5)
)
@Composable
fun StatusMatrix(
    modifier: Modifier = Modifier,
    data: List<FlightStatus>,
    cellSize: Dp = 19.dp,
    spacing: Dp = 4.dp
) {
    BoxWithConstraints(modifier = modifier) {

        val maxWidthPx = constraints.maxWidth.toFloat()
        val cellPx = with(LocalDensity.current) { cellSize.toPx() }
        val spacingPx = with(LocalDensity.current) { spacing.toPx() }

        // ✅ 动态计算每行多少列
        val columnCount = max(
            1,
            ((maxWidthPx + spacingPx) / (cellPx + spacingPx)).toInt()
        )

        val rows = data.chunked(columnCount)

        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    row.forEach { status ->
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .background(
                                    color = StatusColorMap[status] ?: Color.Gray,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun StatusLegend() {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StatusColorMap.forEach { (status, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(19.dp)
                        .background(color, RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when (status) {
                        FlightStatus.GROUND -> "地勤"
                        FlightStatus.STANDBY -> "待机"
                        FlightStatus.CRUISE -> "巡航"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
@Composable
fun FlightStatusSection(
    data: List<FlightStatus>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(22.dp)
    ) {

        // ✅ 左侧：自适应矩阵
        StatusMatrix(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            data = data
        )

        // ✅ 右侧：图例
        StatusLegend()
    }
}
enum class ActivityType {
    TASK_COMPLETED,
    WAYPOINT_REACHED,
    MEDAL_EARNED,
    SYSTEM_UPDATE,
    TASK_END,
    LIFE;

    // 匹配圆点颜色
    val primaryColor: Color
        @Composable get() = when (this) {
            TASK_COMPLETED -> Color(0xFF4CAF50) // 绿色
            WAYPOINT_REACHED -> MaterialTheme.colorScheme.primary // 蓝色
            MEDAL_EARNED -> Color(0xFFFFC107) // 金色
            SYSTEM_UPDATE -> Color(0xFF9C27B0) // 紫色
            TASK_END -> Color(0xFFF44336) // 红色
            LIFE -> Color(0xFF607D8B) // 灰蓝色
        }

    // 匹配图标
    val icon: ImageVector
        get() = when (this) {
            TASK_COMPLETED -> Icons.Default.CheckCircle
            WAYPOINT_REACHED -> Icons.Default.LocationOn
            MEDAL_EARNED -> Icons.Default.Star
            SYSTEM_UPDATE -> Icons.Default.Refresh
            TASK_END -> Icons.Default.Done
            LIFE -> Icons.Default.Build
        }
}
fun Long.toFormattedTime(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
}
data class ActivityLog(
    val title: String,
    val type: ActivityType,
    val timestamp: Long
)
@Composable
fun TimelineItem(
    activity: ActivityLog,
    isFirst: Boolean,
    isLast: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = activity.type.primaryColor
    val lineColor = Color.LightGray.copy(alpha = 0.7f)

    val dotSize by animateDpAsState(
        targetValue = if (isSelected) 14.dp else 10.dp, // 选中时圆点变大
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dotSize"
    )

    val ringAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f, // 选中时圆环透明度
        animationSpec = tween(400),
        label = "ringAlpha"
    )

    val ringScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.5f, // 选中时圆环从小变大
        animationSpec = tween(400, easing = LinearOutSlowInEasing),
        label = "ringScale"
    )

    Column(
        modifier = modifier.width(95.dp), // 每个节点的固定宽度
        horizontalAlignment = Alignment.Start
    ) {
        // --- 顶部：轴线与圆点区域 ---
        Box(
            modifier = Modifier.fillMaxWidth().height(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // 连接线：从圆点中心向后延伸
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // 左侧连接线 (前一个节点伸过来的)
                Box(
                    modifier = Modifier
                        .width(dotSize / 2)
                        .height(2.dp)
                        .background(if (isFirst) Color.Transparent else lineColor)
                )
                // 右侧连接线 (伸向下一个节点的)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(lineColor)
                )
                if (isLast) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .offset(x = (-8).dp), // 稍微向左偏移一点，让箭头尖端与线完美衔接
                        tint = lineColor
                    )
                }
            }
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center // 内部组件相对于 24.dp 的中心对齐
            ) {
                // --- 选中圆环 (包裹在圆点外面) ---
                Box(
                    modifier = Modifier
                        .size(24.dp) // 圆环的最大尺寸
                        .graphicsLayer {
                            alpha = ringAlpha
                            scaleX = ringScale
                            scaleY = ringScale
                        }
                        .border(2.dp, accentColor.copy(alpha = 0.6f), CircleShape)
                )
                // 圆点：固定在最左侧（偏移半个宽度以对齐线）
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(2.dp, accentColor, CircleShape)
                        .padding(2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(accentColor, CircleShape))
                }
            }
        }

        // --- 下部：文字内容区域 ---
        // 关键：paddingStart 等于圆点半径，使文字与圆点中心对齐
        Column(
            modifier = Modifier.padding(start = 0.dp, top = 4.dp, end = 12.dp)
        ) {
            Text(
                text = activity.timestamp.toFormattedTime(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontSize = 10.sp
            )
            Text(
                text = activity.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(7.dp),
    containerColor: Color = Color(0xFFE8E8E8),
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    bottomPadding: Dp = 7.dp,
    startPadding: Dp = 8.dp,
    ratio: Float
) {
    Surface(
        modifier = modifier
            .aspectRatio(ratio)
            .animateContentSize()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current
                    ) { onClick() }
                } else Modifier
            ),
        shape = shape,
        color = containerColor,
        tonalElevation = elevation,
        shadowElevation = elevation
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(startPadding, end = 12.dp, top = 7.dp, bottom = bottomPadding)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(30.dp)
            )

            Column(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.Gray
                )
            }
        }
    }
}
@Composable
fun DetailCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(
        bottomStart = 7.dp,
        bottomEnd = 7.dp
    ),
    containerColor: Color = Color(0xFFE8E8E8),
    elevation: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit = {
        Text(
            text = "详细信息",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        val mockData = remember {
            List(120) {
                FlightStatus.entries.toTypedArray().random()
            }
        }

        FlightStatusSection(
            data = mockData,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = shape,
        color = containerColor,
        tonalElevation = elevation,
        shadowElevation = elevation
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            content = content
        )
    }
}
@Composable
fun CircularQuickActions(
    modifier: Modifier = Modifier,
    actions: List<Triple<ImageVector, String, () -> Unit>>
) {
    var isExpanded by remember { mutableStateOf(false) }
    var draggedActionIndex by remember { mutableStateOf<Int?>(null) }
    val radius = 100.dp
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val actionButtonPositions = remember { mutableMapOf<Int, Offset>() }

    val fanRotation by animateFloatAsState(
        targetValue = if (isExpanded) 120f else 0f,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
    )

    Box(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
        actions.forEachIndexed { index, (icon, label, onClick) ->
            val angleStep = 90f / (actions.size - 1).coerceAtLeast(1)
            val individualOffset = angleStep * index
            val currentAngle = 150f + (fanRotation - (90f - individualOffset)).coerceAtLeast(0f)
            val isStarted = fanRotation >= (90f - individualOffset)

            val alpha by animateFloatAsState(targetValue = if (isExpanded && isStarted) 1f else 0f)
            val scale by animateFloatAsState(targetValue = if (draggedActionIndex == index) 1.25f else 1f)

            val containerColor by animateColorAsState(
                targetValue = if (draggedActionIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
            )
            val iconColor by animateColorAsState(
                targetValue = if (draggedActionIndex == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
            )

            if (alpha > 0.01f) {
                val radian = currentAngle * (PI / 180.0)
                // 计算像素偏移
                val pxOffset = with(LocalDensity.current) { radius.toPx() }
                val x = (pxOffset * cos(radian)).toFloat()
                val y = (pxOffset * sin(radian)).toFloat()

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        // 【关键改动 1】：使用 graphicsLayer 处理位移和裁剪
                        // 这比 offset + clip 更稳定，能防止动画过程中的“变方”
                        .graphicsLayer {
                            translationX = x
                            translationY = y
                            this.alpha = alpha
                            scaleX = scale
                            scaleY = scale
                            clip = true
                            shape = CircleShape
                        }
                        .background(containerColor)
                        .onGloballyPositioned { coordinates ->
                            val centerOffset = with(density) { 24.dp.toPx() }
                            actionButtonPositions[index] = coordinates.positionInParent() + Offset(centerOffset, centerOffset)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp),
                        tint = iconColor
                    )
                }
            }
        }

        // --- 主按钮 ---
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    clip = true
                    shape = CircleShape
                }
                .background(MaterialTheme.colorScheme.primary)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.first()

                            if (change.pressed) {
                                // 只要按下，立即展开
                                if (!isExpanded) {
                                    isExpanded = true
                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                }

                                // 实时检测手指位置（拖拽过程中）
                                val position = change.position
                                var hoveredIndex: Int? = null

                                // 遍历所有子按钮的位置快照
                                for ((idx, center) in actionButtonPositions) {
                                    // 判定半径设为 30dp 左右比较舒适
                                    val distance = (position - center).getDistance()
                                    if (distance <= with(density) { 30.dp.toPx() }) {
                                        hoveredIndex = idx
                                        break
                                    }
                                }

                                if (hoveredIndex != draggedActionIndex) {
                                    draggedActionIndex = hoveredIndex
                                    if (hoveredIndex != null) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }

                                // 消耗掉事件，防止滚动父容器
                                change.consume()
                            }

                            // 只要松手，立即执行逻辑并收回
                            if (change.changedToUp()) {
                                if (draggedActionIndex != null) {
                                    // 如果松手时悬停在某个子按钮上，触发它
                                    actions[draggedActionIndex!!].third.invoke()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }

                                // 无论如何都收回菜单
                                isExpanded = false
                                draggedActionIndex = null
                            }
                        }
                    }
                }
            ,
            contentAlignment = Alignment.Center
        ) {
            val rotation by animateFloatAsState(targetValue = if (isExpanded) 45f else 0f)
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}


