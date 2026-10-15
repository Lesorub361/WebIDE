





/*
 * WebIDE - A powerful IDE for Android web development.
 * Copyright (C) 2025  如日中天  <3382198490@qq.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
/*
 * ResizablePanelLayout.kt
 */
package com.web.webide.ui.editor.components

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.rk.terminal.ui.screens.terminal.TerminalBackEnd
import com.termux.terminal.TextStyle
import com.termux.view.TerminalView
import com.web.webide.core.utils.LogCatcher
import com.web.webide.core.utils.LogEntry
import com.web.webide.ui.editor.viewmodel.CodeEditorState
import com.web.webide.ui.editor.viewmodel.EditorViewModel
import com.web.webide.ui.editor.viewmodel.MediaEditorState
import com.web.webide.ui.terminal.SessionManager
import com.web.webide.ui.terminal.SessionType
import com.web.webide.ui.terminal.TerminalConfig
import com.web.webide.ui.terminal.TerminalFontManager
import com.web.webide.R
import compose.icons.SimpleIcons
import compose.icons.simpleicons.Flutter
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PanelPage(@StringRes val titleRes: Int) {
    TERMINAL(R.string.panel_terminal),
    DIAGNOSTICS(R.string.panel_diagnostics),
    FLUTTER(R.string.panel_flutter),
    BUILD_LOG(R.string.panel_build),
}

@SuppressLint("FrequentlyChangingValue")
@Suppress("DEPRECATION")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditorPanelLayout(
    viewModel: EditorViewModel,
    symbols: List<String>,
    modifier: Modifier = Modifier,
    projectPath: String = "",
    peekHeight: Dp = 86.dp,
    content: @Composable () -> Unit
) {
    val activeTab = viewModel.openFiles.getOrNull(viewModel.activeFileIndex)
    val isMedia = activeTab is MediaEditorState
    val hasActiveEditor = viewModel.openFiles.isNotEmpty() && !isMedia
    
    val density = LocalDensity.current
    val minPeekHeightDp = if (hasActiveEditor) peekHeight else 50.dp
    val animatedMinPeekHeight by animateDpAsState(targetValue = minPeekHeightDp, animationSpec = tween(durationMillis = 300), label = "MinPeekHeight")

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layoutHeight = constraints.maxHeight.toFloat()
        val minPeekHeightPx = with(density) { animatedMinPeekHeight.toPx() }
        var panelHeightPx by remember { mutableFloatStateOf(minPeekHeightPx) }

        // Use a side effect to update panelHeightPx when minPeekHeightPx changes significantly
        // This ensures the panel snaps to the new minimum if it was collapsed, 
        // but doesn't override user expansion if they dragged it up.
        LaunchedEffect(minPeekHeightPx) {
            // Only force update if the current height is close to the OLD minimum (collapsed state)
            // or if we are switching to a mode with a smaller minimum (like media viewer)
            if (panelHeightPx < minPeekHeightPx + 100 || minPeekHeightPx < panelHeightPx) {
                 // But wait, if we are shrinking (e.g. going to media view), we should probably respect that
                 // If we are expanding (e.g. going to code view), we should also respect that
                 panelHeightPx = minPeekHeightPx
            }
        }

        fun clampHeight(newHeight: Float): Float = newHeight.coerceIn(minPeekHeightPx, layoutHeight)
        val draggableState = rememberDraggableState { delta -> panelHeightPx = clampHeight(panelHeightPx - delta) }
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.y
                    val oldHeight = panelHeightPx
                    panelHeightPx = clampHeight(panelHeightPx - delta)
                    return Offset(0f, oldHeight - panelHeightPx)
                }
            }
        }

        val currentPanelHeightDp = with(density) { panelHeightPx.toDp() }
        val hideThresholdPx = layoutHeight * 0.8f
        val showSymbolBar = hasActiveEditor && (panelHeightPx < hideThresholdPx)
        val contentAlpha = remember(panelHeightPx, minPeekHeightPx) {
            val diff = panelHeightPx - minPeekHeightPx
            val fadeDistance = density.density * 50
            (diff / fadeDistance).coerceIn(0f, 1f)
        }

        BackHandler(enabled = panelHeightPx > minPeekHeightPx) { panelHeightPx = minPeekHeightPx }

        Box(modifier = Modifier.fillMaxSize().padding(bottom = currentPanelHeightDp)) { content() }

        Surface(
            modifier = Modifier.fillMaxWidth().height(currentPanelHeightDp).align(Alignment.BottomCenter).nestedScroll(nestedScrollConnection),
            shape = RoundedCornerShape(0.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth().draggable(state = draggableState, orientation = Orientation.Vertical).background(MaterialTheme.colorScheme.surface)) {
                    PanelTopBar(viewModel = viewModel, hasActiveEditor = hasActiveEditor, showLspAndCursor = showSymbolBar)
                }

                AnimatedVisibility(visible = showSymbolBar, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    SymbolBarRow(symbols) { viewModel.insertSymbol(it) }
                }

                val effectiveProjectPath = remember(projectPath, viewModel.currentProjectPath, viewModel.openFiles.size) {
                    if (projectPath.isNotBlank()) projectPath
                    else if (viewModel.currentProjectPath.isNotBlank()) viewModel.currentProjectPath
                    else viewModel.openFiles.firstOrNull()?.file?.parentFile?.absolutePath.orEmpty()
                }

                var selectedTabIndex by remember { mutableIntStateOf(0) }
                val tabs = PanelPage.entries.toTypedArray()

                AnimatedVisibility(visible = hasActiveEditor, enter = fadeIn(), exit = fadeOut()) {
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }

                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.height(48.dp).alpha(contentAlpha).draggable(state = draggableState, orientation = Orientation.Vertical),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 8.dp,
                    indicator = { tabPositions ->
                        if (selectedTabIndex in tabPositions.indices) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    divider = { },
                    tabs = {
                        tabs.forEachIndexed { index, page ->
                            val title = stringResource(page.titleRes)
                            val badge = if (page == PanelPage.DIAGNOSTICS) {
                                val count = (activeTab as? CodeEditorState)?.diagnostics?.size ?: 0
                                if (count > 0) " ($count)" else ""
                            } else ""
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val icon: ImageVector = when (page) {
                                            PanelPage.TERMINAL -> Icons.Default.Terminal
                                            PanelPage.DIAGNOSTICS -> Icons.Default.Warning
                                            PanelPage.FLUTTER -> SimpleIcons.Flutter
                                            PanelPage.BUILD_LOG -> Icons.Default.Article
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "$title$badge",
                                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                enabled = contentAlpha > 0.5f
                            )
                        }
                    }
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surface).alpha(contentAlpha)) {
                    when (tabs[selectedTabIndex]) {
                        PanelPage.TERMINAL -> EmbeddedTerminalPanel(projectPath = effectiveProjectPath)
                        PanelPage.DIAGNOSTICS -> DiagnosticsPanel(viewModel)
                        PanelPage.FLUTTER -> FlutterToolsPanel(
                            projectPath = effectiveProjectPath,
                            onRunCommand = { cmd ->
                                selectedTabIndex = 0
                                SessionManager.currentSession?.write("$cmd\n")
                            }
                        )
                        PanelPage.BUILD_LOG -> BuildLogPanel()
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticsPanel(viewModel: EditorViewModel) {
    val activeTab = viewModel.openFiles.getOrNull(viewModel.activeFileIndex)
    val diagnostics = if (activeTab is CodeEditorState) activeTab.diagnostics else emptyList()

    if (diagnostics.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.panel_no_diagnostics), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(diagnostics) { diagnostic ->
                DiagnosticItem(diagnostic) {
                    // On click, jump to line
                    val line = diagnostic.range.start.line
                    val column = diagnostic.range.start.character
                    viewModel.jumpTo(line, column)
                }
            }
        }
    }
}

@Composable
fun DiagnosticItem(diagnostic: Diagnostic, onClick: () -> Unit) {
    val color = when (diagnostic.severity) {
        DiagnosticSeverity.Error -> MaterialTheme.colorScheme.error
        DiagnosticSeverity.Warning -> Color(0xFFFFA000)
        DiagnosticSeverity.Information -> MaterialTheme.colorScheme.primary
        DiagnosticSeverity.Hint -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        // Icon or Color indicator
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = diagnostic.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${diagnostic.source ?: "LSP"} (${diagnostic.range.start.line + 1}, ${diagnostic.range.start.character + 1})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PanelTopBar(
    viewModel: EditorViewModel,
    hasActiveEditor: Boolean,
    showLspAndCursor: Boolean
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("WebIDE_Editor_Settings", Context.MODE_PRIVATE) }
    val lspEnabled = prefs.getBoolean("editor_lsp_enabled", false)

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp).height(24.dp)
    ) {
        AnimatedVisibility(
            visible = hasActiveEditor && lspEnabled && showLspAndCursor,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // 🔥 修复报错: 先检查是否是 CodeEditorState 再访问 lspEditor
                val activeTab = viewModel.openFiles.getOrNull(viewModel.activeFileIndex)
                val lspConnected = if (activeTab is CodeEditorState) activeTab.lspEditor != null else false

                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (lspConnected) Color(0xFF4CAF50) else Color(0xFFF44336)))
                Text(
                    text = stringResource(if (lspConnected) R.string.panel_lsp_success else R.string.panel_lsp_error),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(modifier = Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)).align(Alignment.Center))

        AnimatedVisibility(
            visible = hasActiveEditor && showLspAndCursor,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            var cursorPosition by remember { mutableStateOf(Pair(1, 1)) }
            LaunchedEffect(viewModel.activeFileIndex) {
                while (true) {
                    cursorPosition = viewModel.getCursorPosition()
                    kotlinx.coroutines.delay(100)
                }
            }
            Text(
                text = stringResource(R.string.panel_cursor_position, cursorPosition.first, cursorPosition.second),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SymbolBarRow(symbols: List<String>, onSymbolClick: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(48.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(8.dp))
        symbols.forEach { symbol ->
            Box(modifier = Modifier.clickable { onSymbolClick(symbol) }.padding(horizontal = 14.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text(text = symbol, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
fun BuildLogPanel() {
    val logs = remember { mutableStateListOf<LogEntry>() }
    val listState = rememberLazyListState()

    // 监听日志流
    LaunchedEffect(Unit) {
        // 1. 先加载历史记录
        logs.clear()
        logs.addAll(LogCatcher.getBuildLogs())
        // 滚动到底部
        if (logs.isNotEmpty()) {
            listState.scrollToItem(logs.size - 1)
        }

        // 2. 监听新日志
        LogCatcher.logFlow.collect { entry ->
            // 只显示 ApkBuilder 或 Build 相关的日志
            if (entry.tag == "ApkBuilder" || entry.tag == "Build") {
                // 如果是新构建的开始，清理本地显示的旧日志
                if (entry.message.contains("========== 开始构建")) {
                    logs.clear()
                }
                
                logs.add(entry)
                // 自动滚动到底部
                try {
                    listState.scrollToItem(logs.size - 1)
                } catch (_: Exception) {}
            }
        }
    }

    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.panel_no_build_logs), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(logs) { log ->
                val time = remember(log.timestamp) {
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                }
                
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(60.dp)
                    )
                    Text(
                        text = log.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = when(log.level) {
                            "ERROR" -> MaterialTheme.colorScheme.error
                            "WARN" -> Color(0xFFFFA000) // Orange
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmbeddedTerminalPanel(
    projectPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val sessions = SessionManager.sessions
    val currentSession = SessionManager.currentSession

    LaunchedEffect(sessions.size, projectPath) {
        if (sessions.isEmpty()) {
            SessionManager.addNewSession(context, SessionType.NORMAL, followProject = projectPath)
        }
    }

    Column(modifier = modifier.fillMaxSize().background(Color(TerminalConfig.getBackgroundColor(isDark)))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val chips = listOf("ls -la", "pwd", "git status", "clear")
            chips.forEach { cmd ->
                Surface(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clickable { currentSession?.write("$cmd\n") },
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = cmd,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = { SessionManager.addNewSession(context, SessionType.NORMAL, followProject = projectPath) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Terminal", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }

        if (currentSession != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(TerminalConfig.getBackgroundColor(isDark)))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        TerminalView(ctx, null).apply {
                            setTextSize(36)
                            setTypeface(TerminalFontManager.getTypeface(ctx))
                            keepScreenOn = true
                            isFocusable = true
                            isFocusableInTouchMode = true
                            attachSession(currentSession)
                            val client = TerminalBackEnd(this, ctx)
                            setTerminalViewClient(client)
                            currentSession.updateTerminalSessionClient(client)
                        }
                    },
                    update = { view ->
                        view.setTypeface(TerminalFontManager.getTypeface(context))
                        view.setBackgroundColor(TerminalConfig.getBackgroundColor(isDark))
                        view.mEmulator?.let { emulator ->
                            val colors = emulator.mColors.mCurrentColors
                            colors[TextStyle.COLOR_INDEX_FOREGROUND] = TerminalConfig.getForegroundColor(isDark)
                            colors[TextStyle.COLOR_INDEX_BACKGROUND] = TerminalConfig.getBackgroundColor(isDark)
                            colors[TextStyle.COLOR_INDEX_CURSOR] = TerminalConfig.getCursorColor(isDark)
                        }
                        if (view.currentSession != currentSession) {
                            view.attachSession(currentSession)
                            val client = TerminalBackEnd(view, context)
                            view.setTerminalViewClient(client)
                            currentSession.updateTerminalSessionClient(client)
                            view.onScreenUpdated()
                        } else {
                            view.invalidate()
                        }
                    }
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
fun FlutterToolsPanel(
    projectPath: String,
    onRunCommand: (String) -> Unit
) {
    val isFlutterProject = remember(projectPath) {
        if (projectPath.isBlank()) false else File(projectPath, "pubspec.yaml").exists()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(SimpleIcons.Flutter, contentDescription = null, tint = Color(0xFF02569B), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Управление Flutter проектом", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (isFlutterProject) "Обнаружен pubspec.yaml — быстрый запуск команд" else "pubspec.yaml не найден в корне проекта",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text("Быстрые команды", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlutterActionButton(
                    title = "Pub Get",
                    subtitle = "Скачать пакеты",
                    icon = Icons.Default.CloudDownload,
                    modifier = Modifier.weight(1f),
                    onClick = { onRunCommand("flutter pub get") }
                )
                FlutterActionButton(
                    title = "Веб-сервер",
                    subtitle = "flutter run web",
                    icon = Icons.Default.PlayArrow,
                    modifier = Modifier.weight(1f),
                    onClick = { onRunCommand("flutter run -d web-server --web-port=8080") }
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlutterActionButton(
                    title = "Сборка APK",
                    subtitle = "flutter build apk",
                    icon = Icons.Default.Build,
                    modifier = Modifier.weight(1f),
                    onClick = { onRunCommand("flutter build apk --release") }
                )
                FlutterActionButton(
                    title = "Очистка кэша",
                    subtitle = "flutter clean",
                    icon = Icons.Default.CleaningServices,
                    modifier = Modifier.weight(1f),
                    onClick = { onRunCommand("flutter clean && flutter pub get") }
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlutterActionButton(
                    title = "Flutter Doctor",
                    subtitle = "Проверка SDK",
                    icon = Icons.Default.HealthAndSafety,
                    modifier = Modifier.weight(1f),
                    onClick = { onRunCommand("flutter doctor -v") }
                )
                FlutterActionButton(
                    title = "Анализ кода",
                    subtitle = "flutter analyze",
                    icon = Icons.Default.CheckCircleOutline,
                    modifier = Modifier.weight(1f),
                    onClick = { onRunCommand("flutter analyze") }
                )
            }
        }
    }
}

@Composable
private fun FlutterActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
