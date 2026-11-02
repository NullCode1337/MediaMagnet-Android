package dev.nullcode.mediamagnet.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.nullcode.mediamagnet.ui.components.TransferBanner
import dev.nullcode.mediamagnet.ui.screens.AboutScreen
import dev.nullcode.mediamagnet.ui.screens.AppearanceSettingsScreen
import dev.nullcode.mediamagnet.ui.screens.ConnectionSettingsScreen
import dev.nullcode.mediamagnet.ui.screens.CookiesScreen
import dev.nullcode.mediamagnet.ui.screens.DownloadsScreen
import dev.nullcode.mediamagnet.ui.screens.DownloadsSettingsScreen
import dev.nullcode.mediamagnet.ui.screens.GalleryDlSettingsScreen
import dev.nullcode.mediamagnet.ui.screens.HistoryScreen
import dev.nullcode.mediamagnet.ui.screens.ShareOptionsSheet
import dev.nullcode.mediamagnet.ui.screens.SettingsScreen
import dev.nullcode.mediamagnet.ui.screens.SpotdlSettingsScreen
import dev.nullcode.mediamagnet.ui.screens.ToolsScreen
import dev.nullcode.mediamagnet.ui.screens.YtDlpSettingsScreen

private data class Destination(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    Destination(START_ROUTE, "Downloads", Icons.Rounded.Download),
    Destination("history", "History", Icons.Rounded.History),
    Destination("settings", "Settings", Icons.Rounded.Settings),
)

private const val START_ROUTE = "downloads"
private val IconSlot = 20.dp
private val IconGap = 8.dp
private val ItemPadding = 16.dp
private val ItemGap = 2.dp
private const val NavFadeMs = 140

private fun titleFor(route: String): String = when (route) {
    "downloads" -> "Downloads"
    "history" -> "History"
    "settings" -> "Settings"
    "settings/connection" -> "Server connection"
    "settings/appearance" -> "Appearance"
    "settings/downloads" -> "Downloads"
    "settings/cookies" -> "Cookies"
    "settings/ytdlp" -> "yt-dlp"
    "settings/gallerydl" -> "gallery-dl"
    "settings/spotdl" -> "spotdl"
    "settings/tools" -> "Tools"
    "settings/about" -> "About"
    else -> "MediaMagnet"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaMagnetApp(
    viewModel: AppViewModel,
    sharedLink: SharedLink? = null,
    onSharedLinkConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: START_ROUTE
    val topLevel = destinations.any { it.route == currentRoute }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message.text)
        }
    }

    Scaffold(
        topBar = {
            if (currentRoute == START_ROUTE) {
                TopAppBar(
                    title = {},
                    actions = {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            } else if (topLevel) {
                MediumFlexibleTopAppBar(
                    title = { Text(titleFor(currentRoute)) },
                    actions = {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            } else {
                TopAppBar(
                    title = { Text(titleFor(currentRoute)) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        },
        bottomBar = {
            PillNavigationBar(
                currentRoute = currentRoute,
                onSelect = { navController.navigateTopLevel(it) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            TransfersHeader(viewModel)
            AppNavHost(navController, viewModel, Modifier.weight(1f))
        }
    }

    LaunchedEffect(sharedLink) {
        if (sharedLink != null) navController.navigateTopLevel(START_ROUTE)
    }

    if (sharedLink != null) {
        ShareOptionsSheet(
            shared = sharedLink,
            initialSettings = settings,
            onDismiss = onSharedLinkConsumed,
            onStart = { url, backend, overrides, domain, content ->
                viewModel.startDownload(url, backend, overrides, domain, content)
                onSharedLinkConsumed()
            },
        )
    }
}

@Composable
private fun TransfersHeader(viewModel: AppViewModel) {
    val transfers by viewModel.transfers.collectAsStateWithLifecycle()
    TransferBanner(transfers.values)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PillNavigationBar(
    currentRoute: String,
    onSelect: (String) -> Unit,
) {
    val motion = MaterialTheme.motionScheme
    val revealSpec = motion.defaultSpatialSpec<Float>()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge

    val selectedIndex = destinations.indexOfFirst { destination ->
        currentRoute == destination.route ||
            (destination.route == "settings" && currentRoute.startsWith("settings/"))
    }.coerceAtLeast(0)

    val labelWidths = remember(textMeasurer, labelStyle, layoutDirection, density) {
        destinations.map { destination ->
            with(density) {
                textMeasurer.measure(
                    text = AnnotatedString(destination.label),
                    style = labelStyle,
                    layoutDirection = layoutDirection,
                ).size.width.toDp()
            }
        }
    }

    val progresses = destinations.indices.map { index ->
        val raw by animateFloatAsState(
            targetValue = if (index == selectedIndex) 1f else 0f,
            animationSpec = revealSpec,
            label = "pillProgress$index",
        )
        raw.coerceIn(0f, 1f)
    }

    val itemWidths = destinations.indices.map { index ->
        labelWidths[index] + ItemPadding * 2 + (IconSlot + IconGap) * progresses[index]
    }
    val itemLefts = destinations.indices.map { index ->
        (0 until index).fold(0.dp) { acc, i -> acc + itemWidths[i] + ItemGap }
    }

    val weight = progresses.sum().takeIf { it > 0.001f } ?: 1f
    val pillLeft = destinations.indices.fold(0.dp) { acc, i -> acc + itemLefts[i] * progresses[i] } / weight
    val pillRight = destinations.indices.fold(0.dp) { acc, i ->
        acc + (itemLefts[i] + itemWidths[i]) * progresses[i]
    } / weight
    val pillWidth = (pillRight - pillLeft).coerceAtLeast(0.dp)

    val pillColor = MaterialTheme.colorScheme.secondaryContainer
    val selectedContent = MaterialTheme.colorScheme.onSecondaryContainer
    val idleContent = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 12.dp, bottom = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .padding(horizontal = 8.dp, vertical = 7.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = pillColor,
                            topLeft = Offset(pillLeft.toPx(), 0f),
                            size = Size(pillWidth.toPx(), size.height),
                            cornerRadius = CornerRadius(size.height / 2f),
                        )
                    },
                horizontalArrangement = Arrangement.spacedBy(ItemGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEachIndexed { index, destination ->
                    PillTab(
                        destination = destination,
                        progress = progresses[index],
                        labelStyle = labelStyle,
                        idleContent = idleContent,
                        selectedContent = selectedContent,
                        onSelect = { onSelect(destination.route) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PillTab(
    destination: Destination,
    progress: Float,
    labelStyle: TextStyle,
    idleContent: Color,
    selectedContent: Color,
    onSelect: () -> Unit,
) {
    val motion = MaterialTheme.motionScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = motion.fastSpatialSpec<Float>(),
        label = "pillPress",
    )
    val contentColor = lerp(idleContent, selectedContent, progress)

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onSelect,
            )
            .padding(horizontal = ItemPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (progress > 0f) {
            Icon(
                imageVector = destination.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(IconSlot * progress)
                    .alpha(progress),
            )
            Spacer(Modifier.width(IconGap * progress))
        }
        Text(
            text = destination.label,
            style = labelStyle,
            color = contentColor,
        )
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    if (route != START_ROUTE &&
        popBackStack(route, inclusive = false, saveState = true)
    ) {
        return
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = START_ROUTE,
        modifier = modifier.fillMaxWidth(),
        enterTransition = {
            fadeIn(tween(NavFadeMs)) + slideInHorizontally(tween(NavFadeMs)) { it / 12 }
        },
        exitTransition = {
            fadeOut(tween(NavFadeMs / 2)) + slideOutHorizontally(tween(NavFadeMs / 2)) { -it / 12 }
        },
        popEnterTransition = {
            fadeIn(tween(NavFadeMs)) + slideInHorizontally(tween(NavFadeMs)) { -it / 12 }
        },
        popExitTransition = {
            fadeOut(tween(NavFadeMs / 2)) + slideOutHorizontally(tween(NavFadeMs / 2)) { it / 12 }
        },
    ) {
        composable(START_ROUTE) {
            DownloadsScreen(
                viewModel = viewModel,
                onOpenSettings = { navController.navigate("settings") },
            )
        }
        composable("history") { HistoryScreen(viewModel) }
        composable("settings") {
            SettingsScreen(onOpen = { navController.navigate(it) })
        }
        composable("settings/connection") { ConnectionSettingsScreen(viewModel) }
        composable("settings/appearance") { AppearanceSettingsScreen(viewModel) }
        composable("settings/downloads") { DownloadsSettingsScreen(viewModel) }
        composable("settings/cookies") { CookiesScreen(viewModel) }
        composable("settings/ytdlp") { YtDlpSettingsScreen(viewModel) }
        composable("settings/gallerydl") { GalleryDlSettingsScreen(viewModel) }
        composable("settings/spotdl") { SpotdlSettingsScreen(viewModel) }
        composable("settings/tools") { ToolsScreen(viewModel) }
        composable("settings/about") { AboutScreen(viewModel) }
    }
}
