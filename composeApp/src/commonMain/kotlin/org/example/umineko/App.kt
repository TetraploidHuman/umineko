//处理UI的平台差异
package org.example.umineko

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.material3.FloatingActionButtonDefaults.elevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.roundToInt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.atan2
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
                    ContentAreaMobile(pageIndex = selectedIndex)
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
                    ContentAreaMobile(pageIndex = selectedIndex)
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            items(48) { itemIndex ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFE8E8E8)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Page $pageIndex\nItem ${itemIndex + 1}",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ContentAreaMobile(modifier: Modifier = Modifier, pageIndex: Int) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val cardGray = Color(0xFFE8E8E8)
    val haptic = LocalHapticFeedback.current
    var isTimelineActive by remember { mutableStateOf(false) }
    // --- 状态追踪 ---
    // 控制外部 Card 的可见性
    var hoveredWaypointIndex by remember { mutableStateOf<Int?>(null) }
    // 控制内部文字显示的内容，初始为 null 确保每次重新进入时不产生滚动动画
    var displayIndex by remember { mutableStateOf<Int?>(null) }

    // 当用户松开手指，Card 消失后的 300ms（动画结束）清空 displayIndex
    LaunchedEffect(hoveredWaypointIndex) {
        if (hoveredWaypointIndex == null) {
            kotlinx.coroutines.delay(300)
            displayIndex = null
        }
    }

    val activities = remember {
        mutableStateListOf(
            ActivityLog("任务已完成", ActivityType.TASK_COMPLETED,Clock.System.now().toEpochMilliseconds()),
            ActivityLog("抵达 WP-1", ActivityType.WAYPOINT_REACHED, Clock.System.now().toEpochMilliseconds() + 1),
            ActivityLog("获得勋章", ActivityType.MEDAL_EARNED, Clock.System.now().toEpochMilliseconds() + 2),
            ActivityLog("系统更新", ActivityType.SYSTEM_UPDATE,Clock.System.now().toEpochMilliseconds() + 3),
            ActivityLog("任务结束", ActivityType.TASK_END, Clock.System.now().toEpochMilliseconds() + 4),
            ActivityLog("我将下班", ActivityType.TASK_END, Clock.System.now().toEpochMilliseconds() + 5),
            ActivityLog("我将洗澡", ActivityType.LIFE,Clock.System.now().toEpochMilliseconds() + 6),
            ActivityLog("我将睡觉", ActivityType.LIFE, Clock.System.now().toEpochMilliseconds() + 7),
        )
    }

    val timelineState = rememberLazyListState()
    val density = LocalDensity.current
    val isNearEnd by remember {
        derivedStateOf {
            val layoutInfo = timelineState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val totalItems = layoutInfo.totalItemsCount

            if (totalItems == 0) return@derivedStateOf false

            // 允许有 1 个误差（更自然）
            lastVisibleItem != null && lastVisibleItem >= totalItems - 2
        }
    }
    LaunchedEffect(activities.size) {
        if (activities.size > 1 && isNearEnd) {

            val distance = with(density) {
                (95.dp + 16.dp).toPx()
            }
            timelineState.animateScrollBy(
                value = distance,
                animationSpec = spring(
                    stiffness = Spring.StiffnessLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            )
        }
    }
    // 在 ContentAreaMobile 内部

// 计算磁吸对齐的偏移量
// 我们希望 Item 的中心对齐到：容器宽度 - 92dp (箭头位置)
    val snapBehavior = rememberSnapFlingBehavior(
        snapLayoutInfoProvider = remember(timelineState) {
            object : SnapLayoutInfoProvider {
                // 核心修正：使用 calculateSnapOffset 替代旧方法
                override fun calculateSnapOffset(velocity: Float): Float {
                    val layoutInfo = timelineState.layoutInfo
                    val visibleItems = layoutInfo.visibleItemsInfo
                    if (visibleItems.isEmpty()) return 0f

                    val containerWidth = layoutInfo.viewportSize.width
                    // 判定点在屏幕上的绝对位置（箭头位置）
                    val triggerPoint = containerWidth - with(density) { (92.dp - 12.dp).toPx() }

                    // 找到中心点离判定点最近的 Item
                    val closestItem = visibleItems.minByOrNull { item ->
                        val itemCenter = item.offset + (item.size / 2)
                        kotlin.math.abs(itemCenter - triggerPoint)
                    } ?: return 0f

                    // 计算该 Item 中心距离判定点的物理位移
                    val currentItemCenter = closestItem.offset + (closestItem.size / 2)

                    // 返回值：正数表示向右修正，负数表示向左修正
                    return currentItemCenter - triggerPoint
                }
            }
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        AnimatedContent(
            targetState = pageIndex,
            transitionSpec = {
                // 定义动画：新页面从底部滑入 + 淡入，旧页面向上滑出 + 淡出
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

                                if (change.pressed) {
                                    isTimelineActive = false
                                }

                            }
                        }
                    },
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (targetPageIndex == 0) {
                    item(span = { GridItemSpan(6) }) {
                        Text(
                            "基本进度",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }

                    // 1. 统计数据卡片
                    item(span = { GridItemSpan(3) }) {
                        StatCard(
                            "获赞",
                            "1.2k",
                            Icons.Default.Favorite,
                            Color(0xFFFFEBEE)
                        )
                    }
                    item(span = { GridItemSpan(3) }) {
                        StatCard(
                            "关注",
                            "328",
                            Icons.Default.Person,
                            Color(0xFFE3F2FD)
                        )
                    }

                    // 2. 进度条卡片
                    item(span = { GridItemSpan(6) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(7.dp))
                                .background(cardGray).padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("任务进度", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
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
                        Text(
                            "详细信息",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 12.dp, start = 4.dp, bottom = 4.dp)
                        )
                    }

                    // 3. 核心交互区域
                    item(span = { GridItemSpan(6) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // --- 左侧：功能卡片 + 弹出位置 Card ---
                            Box(modifier = Modifier.weight(4f)) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier.weight(1f).aspectRatio(1f)
                                                .clip(RoundedCornerShape(7.dp)).background(Color(0xFFFFF9C4))
                                                .clickable { },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    null
                                                ); Text("收藏", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier.weight(1f).aspectRatio(1f)
                                                .clip(RoundedCornerShape(7.dp)).background(cardGray).clickable { },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.Settings,
                                                    null
                                                ); Text("设置", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier.weight(1f).aspectRatio(1f)
                                                .clip(RoundedCornerShape(7.dp)).background(Color(0xFFE1F5FE))
                                                .clickable { },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.Build,
                                                    null
                                                ); Text("勋章", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier.weight(1f).aspectRatio(1f)
                                                .clip(RoundedCornerShape(7.dp)).background(cardGray).clickable {
                                                activities.add(
                                                    ActivityLog(
                                                        "抵达 WP-1",
                                                        ActivityType.LIFE, Clock.System.now().toEpochMilliseconds()
                                                    )
                                                )
                                            }, contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.Build,
                                                    null
                                                ); Text("睡觉", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }

                                // --- 位置信息 Card ---
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
                                                // 如果是从 null 变到数字（即刚开始触摸），不滚动，只淡入
//                                            if (initialState == null) {
//                                                fadeIn() togetherWith fadeOut()
//                                            } else {
                                                // 只有在数字之间切换时才执行上下滚动
                                                if ((targetState ?: 0) > (initialState ?: 0)) {
                                                    slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                                                } else {
                                                    slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                                                }
                                                //}.using(SizeTransform(clip = false))
                                            }
                                        ) { target ->
                                            if (target != null) {
                                                Column {
                                                    Text(
                                                        "位置详情",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        "WP-$target",
                                                        style = MaterialTheme.typography.headlineSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(Modifier.weight(1f))
                                                    Text(
                                                        "坐标: ${30.123}, ${120.456}",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // --- 右侧：航线节点链 ---
                            data class Waypoint(val id: Int, val distanceToNext: Float)

                            val allWaypoints = remember {
                                List(20) { i ->
                                    Waypoint(
                                        id = i + 1,
                                        distanceToNext = (50..300).random().toFloat()
                                    )
                                }
                            }
                            var currentIndex by remember { mutableIntStateOf(1) }
                            val nodePositions = remember { mutableMapOf<Int, Float>() }

                            Box(
                                modifier = Modifier
                                    .weight(2.4f).fillMaxHeight().clip(RoundedCornerShape(7.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(7.dp))
                                    .padding(8.dp)

                            ) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier.fillMaxHeight()
                                            .padding(top = 6.dp, bottom = 6.dp, start = 12.dp).pointerInput(Unit) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val pointer = event.changes.first()
                                                    val pos = pointer.position
                                                    val isInNodeZone = pos.x < 45.dp.toPx()

                                                    if (pointer.pressed && isInNodeZone) {
                                                        pointer.consume()
                                                        val closest =
                                                            nodePositions.minByOrNull { kotlin.math.abs(it.value - pos.y) }
                                                        if (closest != null && kotlin.math.abs(closest.value - pos.y) < 60f) {
                                                            // 先设置索引，再设置可见性，确保 Card 弹出时内容已就绪
                                                            if (displayIndex != closest.key) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            }
                                                            displayIndex = closest.key
                                                            hoveredWaypointIndex = closest.key

                                                        }
                                                    } else {
                                                        if (hoveredWaypointIndex != null) {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        }
                                                        hoveredWaypointIndex = null
                                                        pointer.consume()
                                                        // 注意：此处不立即清空 displayIndex，由 LaunchedEffect 处理
                                                    }
                                                }
                                            }
                                        },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // 计算各段距离权重 (增加平滑动画)
                                        val wNext2 by animateFloatAsState(
                                            targetValue = ((allWaypoints.getOrNull(currentIndex + 1)?.distanceToNext
                                                ?: 100f) / 50f).coerceIn(0.5f, 5f),
                                            animationSpec = tween(450) // 这里的时长决定了节点挪动的速度
                                        )
                                        val wNext1 by animateFloatAsState(
                                            targetValue = ((allWaypoints.getOrNull(currentIndex)?.distanceToNext
                                                ?: 100f) / 50f).coerceIn(0.5f, 5f),
                                            animationSpec = tween(450)
                                        )
                                        val wPast by animateFloatAsState(
                                            targetValue = ((allWaypoints.getOrNull(currentIndex - 1)?.distanceToNext
                                                ?: 50f) / 50f).coerceIn(0.5f, 2f),
                                            animationSpec = tween(450)
                                        )

                                        Box(
                                            modifier = Modifier.size(6.dp)
                                                .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                                .onGloballyPositioned {
                                                    nodePositions[currentIndex + 2] = it.positionInParent().y
                                                })
                                        Box(
                                            modifier = Modifier.width(1.dp).weight(wNext2)
                                                .background(Color.Gray.copy(alpha = 0.3f))
                                        )
                                        Box(
                                            modifier = Modifier.size(8.dp).border(1.dp, Color.Gray, CircleShape)
                                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                                .onGloballyPositioned {
                                                    nodePositions[currentIndex + 1] = it.positionInParent().y
                                                })
                                        Box(
                                            modifier = Modifier.width(1.dp).weight(wNext1)
                                                .background(Color.Gray.copy(alpha = 0.3f))
                                        )
                                        Box(
                                            modifier = Modifier.size(16.dp)
                                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                .padding(3.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                .onGloballyPositioned {
                                                    nodePositions[currentIndex] = it.positionInParent().y
                                                })
                                        Box(
                                            modifier = Modifier.width(2.dp).weight(wPast)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                        )
                                        Box(
                                            modifier = Modifier.size(6.dp).background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                CircleShape
                                            ).onGloballyPositioned {
                                                nodePositions[currentIndex - 1] = it.positionInParent().y
                                            })
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 4.dp),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                "NEXT WP",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "WP-$currentIndex",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // 距离显示
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.size(70.dp) // 稍微加大一点容器，防止数字大时显得拥挤
                                        ) {
                                            val currentDistance =
                                                allWaypoints.getOrNull(currentIndex)?.distanceToNext ?: 0f

                                            CircularProgressIndicator(
                                                progress = { 0.7f },
                                                modifier = Modifier.fillMaxSize(),
                                                strokeWidth = 3.dp,
                                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            )

                                            // 数字与单位
                                            Row(
                                                verticalAlignment = Alignment.Bottom, // 关键：让单位对齐数字底部
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = currentDistance.toInt().toString(),
                                                    fontSize = 20.sp, // 使用更大的字体
                                                    fontWeight = FontWeight.ExtraBold, // 加粗
                                                    lineHeight = 1.em // 紧凑行高
                                                )
                                                Text(
                                                    text = "m",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp, // 保持单位较小
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(start = 1.dp, bottom = 2.dp) // 微调单位位置
                                                )
                                            }
                                        }

                                        // 抵达按钮
                                        IconButton(
                                            onClick = {
                                                if (currentIndex < allWaypoints.size - 2) currentIndex++ else currentIndex =
                                                    1
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            },
                                            modifier = Modifier
                                                .align(Alignment.End) // 如果在 Column 里，靠右对齐
                                                .size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = "Next",
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. 最近动态 - 横向时间轴
                    item(span = { GridItemSpan(6) }) {
                        Text(
                            "最近动态",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 12.dp, start = 4.dp, bottom = 4.dp)
                        )
                    }

                    item(span = { GridItemSpan(6) }) {
                        // --- 状态：追踪当前指向的活动 ---
                        val activeActivity by remember {
                            derivedStateOf {
                                val layoutInfo = timelineState.layoutInfo
                                val visibleItems = layoutInfo.visibleItemsInfo
                                if (visibleItems.isEmpty()) return@derivedStateOf null

                                // 1. 获取容器的总宽度
                                val containerWidth = layoutInfo.viewportSize.width

                                // 2. 计算判定点的位置：容器宽度 - 箭头距离右边的 padding (92dp) - 箭头自身宽度的一半 (12dp)
                                // 这样判定点就正好落在箭头的尖端正下方
                                val triggerPoint = containerWidth - with(density) { (92.dp - 12.dp).toPx() }

                                // 3. 寻找中心点最接近该判定点的项
                                visibleItems.minByOrNull { item ->
                                    val itemCenter = item.offset + (item.size / 2)
                                    kotlin.math.abs(itemCenter - triggerPoint)
                                }?.let { activities.getOrNull(it.index) }
                            }
                        }
                        LaunchedEffect(activeActivity) {
                            if (activeActivity != null && isTimelineActive) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            val change = event.changes.first()

                                            if (change.pressed) {
                                                isTimelineActive = true
                                            }

                                        }
                                    }
                                }
                        ) {
                            // 时间轴容器
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cardGray.copy(alpha = 0.3f))
                                    .padding(vertical = 12.dp)
                            ) {
                                // --- 新增：向下指的箭头指示器 ---
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd) // 改为右上角对齐
                                        .padding(end = 87.dp)    // 距离右边缘一段距离，对齐最后一个 Item 的大致位置
                                        .size(24.dp)
                                        .offset(y = (-19).dp)
                                )

                                LazyRow(
                                    state = timelineState,
                                    flingBehavior = snapBehavior,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp) // 增加内边距方便滑动
                                ) {
                                    itemsIndexed(
                                        items = activities,
                                        key = { index, item -> item.timestamp }
                                    ) { index, activity ->
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
                                // --- 新增：详情展示 Card ---
                                AnimatedContent(
                                    targetState = activeActivity,
                                    transitionSpec = {
                                        // 获取初始状态和目标状态在原列表中的索引
                                        val initialIndex = activities.indexOf(initialState)
                                        val targetIndex = activities.indexOf(targetState)

                                        // 如果目标索引大于初始索引，说明时间轴在向左滚动（查看未来的项），新卡片应从右侧滑入
                                        if (targetIndex > initialIndex) {
                                            (slideInHorizontally { it } + fadeIn()).togetherWith(
                                                slideOutHorizontally { -it } + fadeOut()
                                            )
                                        } else {
                                            // 否则，新卡片从左侧滑入
                                            (slideInHorizontally { -it } + fadeIn()).togetherWith(
                                                slideOutHorizontally { it } + fadeOut()
                                            )
                                        }.using(
                                            // 加上 SizeTransform(clip = false) 可以防止动画过程中阴影被裁剪
                                            SizeTransform(clip = false)
                                        )
                                    },
                                    label = "ActivityDetail"
                                ) { target ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().aspectRatio(3f),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFE8E8E8)
                                        ),
                                        shape = RoundedCornerShape(7.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Info,
                                                    null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = target?.title ?: "滑动查看动态",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (target != null) "记录时间: ${target.timestamp.toFormattedTime()}" else "请左右滑动上方时间轴",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(6, span = { GridItemSpan(3) }) { itemIndex ->
                        Box(
                            modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(cardGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Item ${itemIndex + 1}", color = Color.Gray)
                        }
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
fun StatCard(label: String, value: String, icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFFE8E8E8))
            .padding(12.dp)
    ) {
        Icon(
            icon, null,
            modifier = Modifier.align(Alignment.TopEnd).size(30.dp),
            tint = Color.Gray.copy(alpha = 0.5f)
        )
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.titleSmall, color = Color.Gray)
        }
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


