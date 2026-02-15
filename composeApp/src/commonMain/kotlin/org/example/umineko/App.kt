//处理UI的平台差异
package org.example.umineko

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// --- 动画常量 ---
private const val ANIMATION_DURATION_MS = 300
private val animationSpecDp = tween<Dp>(durationMillis = ANIMATION_DURATION_MS)
private val animationSpecFloat = tween<Float>(durationMillis = ANIMATION_DURATION_MS)
private val animationSpecFloatQuick = tween<Float>(durationMillis = ANIMATION_DURATION_MS)

// --- UI 常量 ---
private val regularItemHeight = 44.dp
private val itemSpacing = 2.dp
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
    LaunchedEffect(Unit) {
        username = apiService.sendMessage()
    }
    val fullUserProfile = NavItem(username, Icons.Filled.AccountCircle, "user@example.com")

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
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
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
            ) {
                // 顶部命令栏
                TopTitleBarTablet(
                    title = items[selectedIndex].title
                )

                Box(modifier = Modifier.weight(1f)) {
                    ContentAreaWithPredictiveGestures(
                        targetPage = selectedIndex,
                        onPageChangeByGesture = onItemSelected,
                        totalItems = items.size
                    )
                }
                // *** BottomCommandBarTablet 从这里被移除了 ***
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
fun ContentAreaWithPredictiveGestures(
    targetPage: Int, // 外部传入的目标页面索引
    onPageChangeByGesture: (Int) -> Unit, // 当手势触发页面改变时，回调此函数更新外部状态
    totalItems: Int
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) } // 当前页面的水平偏移量

// 内部状态，代表当前实际稳定显示的页面。只在动画结束后更新。
    var currentPage by remember { mutableStateOf(targetPage) }

// 标志位，用于区分手势拖动和程序化动画，避免冲突
    var isDragging by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds() // 关键：确保拖出屏幕的内容被裁剪
            .pointerInput(currentPage, totalItems) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        coroutineScope.launch { offsetX.stop() }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        val widthPx = size.width.toFloat()
                        val threshold = widthPx / 3
                        val currentOffset = offsetX.value

                        coroutineScope.launch {
                            val newTarget = when {
                                currentOffset < -threshold && currentPage < totalItems - 1 ->
                                    currentPage + 1
                                currentOffset > threshold && currentPage > 0 ->
                                    currentPage - 1
                                else -> currentPage
                            }

                            if (newTarget != currentPage) {
                                onPageChangeByGesture(newTarget)
                            } else {
                                offsetX.animateTo(0f, animationSpec = animationSpecFloatQuick)
                            }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        coroutineScope.launch {
                            offsetX.animateTo(0f, animationSpec = animationSpecFloatQuick)
                        }
                    }
                )
            }
    ) {
        val containerWidthPx = constraints.maxWidth.toFloat()

        LaunchedEffect(targetPage) {
            // 如果正在拖动，直接退出，不干扰手势
            if (isDragging) return@LaunchedEffect

            // 如果没有切换页面，只需要确保 offsetX 回到 0
            if (targetPage == currentPage) {
                if (offsetX.value != 0f) {
                    offsetX.animateTo(0f, animationSpec = animationSpecFloatQuick)
                }
                return@LaunchedEffect
            }

            // 页面切换动画
            val isForward = targetPage > currentPage
            val animationTargetOffset =
                if (isForward) -containerWidthPx else containerWidthPx

            offsetX.animateTo(animationTargetOffset, animationSpec = animationSpecFloatQuick)

            currentPage = targetPage
            offsetX.snapTo(0f)
        }


        val prevPage = currentPage - 1
        if (prevPage >= 0) {
            key(prevPage) { // 使用 key 确保 Compose 正确识别和管理不同页面的 Composable 实例
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset((-containerWidthPx + offsetX.value).roundToInt(), 0) }
                ) {
                    ContentArea(pageIndex = prevPage)
                }
            }
        }

// 2. 当前页面 (始终显示 currentPage 的内容)
        key(currentPage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            ) {
                ContentArea(pageIndex = currentPage)
            }
        }

// 3. 下一个页面 (如果存在)
        val nextPage = currentPage + 1
        if (nextPage < totalItems) {
            key(nextPage) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset((containerWidthPx + offsetX.value).roundToInt(), 0) }
                ) {
                    ContentArea(pageIndex = nextPage)
                }
            }
        }
    }
}

@Composable
fun TopTitleBarTablet(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp),
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
        val contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding(),
            start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
            end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
            bottom = animatedBottomPadding
        )
        ContentArea(Modifier.padding(contentPadding), pageIndex = selectedIndex)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileTopBar(title: String, userProfile: NavItem, isNavOpen: Boolean) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        TopAppBar(
            title = { Text(title, fontWeight = FontWeight.SemiBold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
        AnimatedVisibility(visible = isNavOpen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = userProfile.icon,
                    contentDescription = "User Profile",
                    modifier = Modifier.size(50.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(userProfile.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    userProfile.subtitle?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
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
                .height(regularItemHeight)
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
                        .height(regularItemHeight)
                        .onGloballyPositioned {
                            val newY = with(density) { it.positionInParent().y.toDp() }
                            if (itemPositions[index] != newY) {
                                itemPositions[index] = newY
                            }
                        }
                )
                if (index < items.size - 1) {
                    Spacer(Modifier.height(itemSpacing))
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
            .padding(horizontal = 16.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(48) { itemIndex ->
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
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