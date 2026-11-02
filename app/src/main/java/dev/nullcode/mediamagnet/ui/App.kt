package dev.nullcode.mediamagnet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import dev.nullcode.mediamagnet.ui.screens.SettingsScreen
import dev.nullcode.mediamagnet.ui.screens.SpotdlSettingsScreen
import dev.nullcode.mediamagnet.ui.screens.ToolsScreen
import dev.nullcode.mediamagnet.ui.screens.YtDlpSettingsScreen

private data class Destination(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    Destination("downloads", "Downloads", Icons.Rounded.Download),
    Destination("history", "History", Icons.Rounded.History),
    Destination("settings", "Settings", Icons.Rounded.Settings),
)

private fun titleFor(route: String): String = when (route) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaMagnetApp(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val transfers by viewModel.transfers.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "downloads"
    val topLevel = destinations.any { it.route == currentRoute }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message.text)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleFor(currentRoute)) },
                navigationIcon = {
                    if (!topLevel) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        bottomBar = {
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
                    val density = LocalDensity.current
                    val selectedIndex = destinations.indexOfFirst { destination ->
                        currentRoute == destination.route ||
                            (destination.route == "settings" &&
                                currentRoute.startsWith("settings/"))
                    }.coerceAtLeast(0)
                    val bounds = remember {
                        mutableStateOf(List(destinations.size) { IntRect.Zero })
                    }
                    val target = bounds.value.getOrElse(selectedIndex) { IntRect.Zero }
                    val indicatorLeft = remember { Animatable(0f) }
                    val indicatorWidth = remember { Animatable(0f) }

                    LaunchedEffect(target.left) {
                        if (target != IntRect.Zero) indicatorLeft.animateTo(target.left.toFloat())
                    }
                    LaunchedEffect(target.width) {
                        if (target != IntRect.Zero) indicatorWidth.animateTo(target.width.toFloat())
                    }

                    Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
                        if (target != IntRect.Zero) {
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(indicatorLeft.value.roundToInt(), target.top)
                                    }
                                    .size(
                                        width = with(density) { indicatorWidth.value.toDp() },
                                        height = with(density) { target.height.toDp() },
                                    )
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            destinations.forEachIndexed { index, destination ->
                                val selected = index == selectedIndex
                                Row(
                                    modifier = Modifier
                                        .onGloballyPositioned { coordinates ->
                                            val position = coordinates.positionInParent()
                                            val left = position.x.roundToInt()
                                            val top = position.y.roundToInt()
                                            val rect = IntRect(
                                                left = left,
                                                top = top,
                                                right = left + coordinates.size.width,
                                                bottom = top + coordinates.size.height,
                                            )
                                            if (bounds.value[index] != rect) {
                                                bounds.value = bounds.value.toMutableList()
                                                    .also { it[index] = rect }
                                            }
                                        }
                                        .clip(CircleShape)
                                        .clickable {
                                            navController.navigate(destination.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        destination.icon,
                                        contentDescription = if (selected) null else destination.label,
                                        tint = if (selected) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(28.dp),
                                    )
                                    AnimatedVisibility(
                                        visible = selected,
                                        enter = fadeIn(tween(120)) +
                                            expandHorizontally(tween(180)),
                                        exit = fadeOut(tween(80)) +
                                            shrinkHorizontally(tween(140)),
                                    ) {
                                        Text(
                                            destination.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(start = 8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TransferBanner(transfers.values)
            NavHost(
                navController = navController,
                startDestination = "downloads",
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(80)) },
                exitTransition = { fadeOut(tween(80)) },
                popEnterTransition = { fadeIn(tween(80)) },
                popExitTransition = { fadeOut(tween(80)) },
            ) {
                composable("downloads") {
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
    }
}
