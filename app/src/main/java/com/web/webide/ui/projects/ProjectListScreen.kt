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


package com.web.webide.ui.projects

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.web.webide.R
import com.web.webide.core.utils.WorkspaceManager
import compose.icons.SimpleIcons
import compose.icons.simpleicons.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.web.webide.safeNavigate
import com.web.webide.ui.agent.AgentViewModel
import com.web.webide.ui.components.GitHubCloneDialog

// --- 常量定义 ---
private const val PREFS_NAME = "project_prefs"
private const val KEY_PINNED_PROJECTS = "pinned_projects"
private const val KEY_SORT_ORDER = "sort_order"
private const val KEY_SEARCH_HISTORY = "search_history"

// --- 数据模型 ---
enum class ProjectTechType(
    val displayName: String,
    val badgeColor: Color,
    val icon: ImageVector
) {
    ALL("Все", Color(0xFF607D8B), Icons.Default.AllInclusive),
    FLUTTER("Flutter", Color(0xFF02569B), SimpleIcons.Flutter),
    WEB("Web", Color(0xFFE34F26), SimpleIcons.Html5),
    NODE("Node.js", Color(0xFF339933), SimpleIcons.Javascript),
    PYTHON("Python", Color(0xFF3776AB), SimpleIcons.Python),
    ANDROID("Android", Color(0xFF3DDC84), Icons.Default.Android),
    OTHER("Проект", Color(0xFF78909C), Icons.Default.Folder)
}

data class ProjectItem(
    val name: String,
    val lastModified: Long,
    val techType: ProjectTechType = ProjectTechType.OTHER,
    val gitBranch: String? = null
)

fun detectProjectTech(dir: File): Pair<ProjectTechType, String?> {
    var branch: String? = null
    val gitHead = File(dir, ".git/HEAD")
    if (gitHead.exists()) {
        try {
            val headContent = gitHead.readText().trim()
            if (headContent.startsWith("ref: refs/heads/")) {
                branch = headContent.removePrefix("ref: refs/heads/")
            }
        } catch (_: Exception) {}
    }

    val tech = when {
        File(dir, "pubspec.yaml").exists() -> ProjectTechType.FLUTTER
        File(dir, "build.gradle").exists() || File(dir, "build.gradle.kts").exists() -> ProjectTechType.ANDROID
        File(dir, "package.json").exists() -> ProjectTechType.NODE
        File(dir, "main.py").exists() || File(dir, "requirements.txt").exists() -> ProjectTechType.PYTHON
        File(dir, "index.html").exists() || File(dir, "webapp.json").exists() -> ProjectTechType.WEB
        else -> ProjectTechType.OTHER
    }
    return tech to branch
}

fun formatRelativeTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        diff < 0 || seconds < 60 -> "Только что"
        minutes < 60 -> "$minutes мин назад"
        hours < 24 -> "$hours ч назад"
        days == 1L -> "Вчера"
        days < 7 -> "$days дн назад"
        else -> {
            val sdf = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
            sdf.format(java.util.Date(millis))
        }
    }
}

// --- 排序规则枚举 ---
enum class SortOrder(@StringRes val displayNameRes: Int) {
    NAME_ASC(R.string.sort_name_asc),
    NAME_DESC(R.string.sort_name_desc),
    DATE_NEWEST(R.string.sort_date_newest),
    DATE_OLDEST(R.string.sort_date_oldest);

    companion object {
        fun fromOrdinal(ordinal: Int): SortOrder = entries.getOrElse(ordinal) { NAME_ASC }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectListScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val projectDirPath = WorkspaceManager.getWorkspacePath(context)
    val projectDir = File(projectDirPath)

    // --- 核心数据状态 ---
    var projectList by remember { mutableStateOf<List<ProjectItem>>(emptyList()) }
    var pinnedProjects by remember { mutableStateOf<Set<String>>(emptySet()) }
    var currentSortOrder by remember { mutableStateOf(SortOrder.NAME_ASC) }
    var selectedFilter by remember { mutableStateOf(ProjectTechType.ALL) }

    // --- 搜索相关状态 ---
    var isSearchActive by remember { mutableStateOf(false) } // 是否处于搜索模式
    var searchQuery by remember { mutableStateOf("") }
    var searchHistory by remember { mutableStateOf<List<String>>(emptyList()) }

    // --- UI 交互状态 ---
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<String?>(null) }

    val agentViewModel: AgentViewModel = viewModel()
    val agentState by agentViewModel.state.collectAsState()
    var showGitHubDialog by remember { mutableStateOf(false) }


    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val sortLabels = SortOrder.entries.associateWith { stringResource(it.displayNameRes) }

    // FAB 逻辑：搜索时不显示，非搜索模式下根据滑动显示
    val isFabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    // 按下物理返回键时，如果是搜索模式，则退出搜索模式
    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    // --- 持久化工具函数 ---
    fun saveSearchHistory(query: String) {
        if (query.isBlank()) return
        val newHistory = (listOf(query) + searchHistory)
            .distinct() // 去重
            .take(10)   // 只保留最近10条

        searchHistory = newHistory
        // 简单持久化：用换行符分隔保存 (假设项目名不含换行符)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_SEARCH_HISTORY, newHistory.joinToString("\n")) }
    }

    fun deleteHistoryItem(item: String) {
        val newHistory = searchHistory - item
        searchHistory = newHistory
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_SEARCH_HISTORY, newHistory.joinToString("\n")) }
    }

    fun loadHistory() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyStr = prefs.getString(KEY_SEARCH_HISTORY, "") ?: ""
        if (historyStr.isNotEmpty()) {
            searchHistory = historyStr.split("\n")
        }
    }

    // --- 列表刷新逻辑 ---
    fun refreshList() {
        scope.launch(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedPinned = prefs.getStringSet(KEY_PINNED_PROJECTS, emptySet()) ?: emptySet()
            val sortOrdinal = prefs.getInt(KEY_SORT_ORDER, SortOrder.NAME_ASC.ordinal)
            val sortOrder = SortOrder.fromOrdinal(sortOrdinal)

            withContext(Dispatchers.Main) {
                pinnedProjects = savedPinned
                currentSortOrder = sortOrder
            }

            if (projectDir.exists() && projectDir.isDirectory) {
                val rawFiles = projectDir.listFiles { file ->
                    file.isDirectory && file.name != "logs"
                } ?: emptyArray()

                val items = rawFiles.map { file ->
                    val (tech, branch) = detectProjectTech(file)
                    ProjectItem(
                        name = file.name,
                        lastModified = file.lastModified(),
                        techType = tech,
                        gitBranch = branch
                    )
                }

                // 先排序
                val sortedList = items.sortedWith(
                    compareByDescending<ProjectItem> { it.name in savedPinned }
                        .then(
                            when (sortOrder) {
                                SortOrder.NAME_ASC -> compareBy { it.name.lowercase() }
                                SortOrder.NAME_DESC -> compareByDescending { it.name.lowercase() }
                                SortOrder.DATE_NEWEST -> compareByDescending { it.lastModified }
                                SortOrder.DATE_OLDEST -> compareBy { it.lastModified }
                            }
                        )
                )
                withContext(Dispatchers.Main) { projectList = sortedList }
            } else {
                withContext(Dispatchers.Main) { projectList = emptyList() }
            }
        }
    }

    if (showGitHubDialog) {
        GitHubCloneDialog(
            state = agentState,
            onStartGitHubLogin = { agentViewModel.startGitHubLogin() },
            onGenerateNewGitHubCode = { agentViewModel.generateNewGitHubCode() },
            onRefreshGitHub = { agentViewModel.refreshGitHubRepositories() },
            onDisconnectGitHub = { agentViewModel.disconnectGitHub() },
            onCloneGitHub = { repo ->
                agentViewModel.cloneGitHubRepository(repo)
                showGitHubDialog = false
                refreshList()
            },
            onCloneGitUrl = { url ->
                agentViewModel.clonePublicGitRepository(url)
                showGitHubDialog = false
                refreshList()
            },
            onDismiss = {
                showGitHubDialog = false
                refreshList()
            }
        )
    }

    // --- 操作逻辑 ---
    fun togglePin(folderName: String) {
        val newPinned = pinnedProjects.toMutableSet()
        if (newPinned.contains(folderName)) newPinned.remove(folderName) else newPinned.add(folderName)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putStringSet(KEY_PINNED_PROJECTS, newPinned) }
        refreshList()
    }

    fun changeSortOrder(newOrder: SortOrder) {
        currentSortOrder = newOrder
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putInt(KEY_SORT_ORDER, newOrder.ordinal) }
        refreshList()
    }

    @SuppressLint("LocalContextGetResourceValueCall")
    fun deleteProject(folderName: String) {
        scope.launch(Dispatchers.IO) {
            val targetDir = File(projectDir, folderName)
            val success = targetDir.deleteRecursively()
            withContext(Dispatchers.Main) {
                if (success) {
                    if (pinnedProjects.contains(folderName)) togglePin(folderName) else refreshList()
                    snackbarHostState.showSnackbar(context.getString(R.string.snackbar_project_deleted))
                } else {
                    snackbarHostState.showSnackbar(context.getString(R.string.snackbar_project_delete_failed))
                }
            }
        }
    }

    // 初始化
    LaunchedEffect(projectDir) {
        refreshList()
        loadHistory()
    }

    // --- 计算当前显示的列表 ---
    // 如果在搜索模式且有输入，过滤列表；否则显示完整排序列表
    val displayList = remember(projectList, isSearchActive, searchQuery, selectedFilter) {
        val filteredByTech = if (selectedFilter == ProjectTechType.ALL) {
            projectList
        } else {
            projectList.filter { it.techType == selectedFilter }
        }
        if (isSearchActive && searchQuery.isNotEmpty()) {
            filteredByTech.filter { it.name.contains(searchQuery, ignoreCase = true) }
        } else {
            filteredByTech
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    if (targetState) {
                        (fadeIn(tween(300)) + slideInVertically(tween(300)) { -it }).togetherWith(fadeOut(tween(300)))
                    } else {
                        fadeIn(tween(300)).togetherWith(fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it })
                    }
                },
                label = "TopBarAnimation"
            ) { active ->
                if (active) {
                    // --- 搜索模式 TopBar ---
                    TopAppBar(
                        title = {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(stringResource(R.string.projects_search_placeholder), style = MaterialTheme.typography.bodyLarge) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    saveSearchHistory(searchQuery)
                                    focusManager.clearFocus()
                                }),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                isSearchActive = false
                                searchQuery = ""
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.content_desc_exit_search))
                            }
                        },
                        actions = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, stringResource(R.string.action_clear))
                                }
                            }
                        }
                    )
                } else {
                    // --- 常规模式 TopBar ---
                    LargeTopAppBar(
                        title = { Text(stringResource(R.string.projects_title)) },
                        scrollBehavior = scrollBehavior,
                        actions = {
                            // 搜索按钮
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, stringResource(R.string.action_search))
                            }
                            // 排序按钮
                                Box {
                                    IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.content_desc_options))
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    SortOrder.entries.forEach { order ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (order == currentSortOrder) {
                                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(8.dp))
                                                    } else {
                                                        Spacer(Modifier.width(24.dp))
                                                    }
                                                    Text(sortLabels.getValue(order))
                                                }
                                            },
                                            onClick = {
                                                changeSortOrder(order)
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                            // GitHub Репозитории / Клонирование
                            IconButton(onClick = {
                                showGitHubDialog = true
                                if (agentState.githubAuthStatus == com.web.webide.ui.agent.GitHubAuthStatus.CONNECTED && agentState.githubRepositories.isEmpty()) {
                                    agentViewModel.refreshGitHubRepositories()
                                }
                            }) {
                                Icon(Icons.Default.CloudDownload, "GitHub / Git Clone")
                            }
                            // AI Agent 入口
                            IconButton(onClick = { navController.safeNavigate("agent") }) {
                                Icon(Icons.Default.AutoAwesome, "AI Agent")
                            }
                            // 终端入口：从主界面直接打开终端
                            IconButton(onClick = { navController.safeNavigate("terminal") }) {
                                Icon(Icons.Default.Terminal, stringResource(R.string.action_terminal))
                            }
                            IconButton(onClick = { navController.safeNavigate("settings") }) {
                                Icon(Icons.Default.Settings, stringResource(R.string.action_settings))
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            // 搜索时不显示 FAB
            AnimatedVisibility(
                visible = !isSearchActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { navController.safeNavigate("new_project") },
                    icon = { Icon(Icons.Default.Add, stringResource(R.string.projects_new_project)) },
                    text = { Text(stringResource(R.string.projects_new_project)) },
                    expanded = isFabExpanded
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val showHistory = isSearchActive && searchQuery.isEmpty()
            AnimatedContent(
                targetState = showHistory,
                transitionSpec = { fadeIn(tween(300)).togetherWith(fadeOut(tween(300))) },
                label = "ContentAnim"
            ) { isHistory ->
                // --- 场景 1：搜索模式且无输入 -> 显示历史记录 ---
                if (isHistory) {
                if (searchHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.projects_no_search_history), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            Text(
                                stringResource(R.string.projects_search_history),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(searchHistory) { historyItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchQuery = historyItem // 填充搜索
                                        // 可选：点击历史立即触发搜索并移动到历史首位
                                        saveSearchHistory(historyItem)
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(historyItem, style = MaterialTheme.typography.bodyLarge)
                                }
                                IconButton(
                                    onClick = { deleteHistoryItem(historyItem) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.content_desc_delete_history),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        item {
                            TextButton(
                                onClick = {
                                    searchHistory = emptyList()
                                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                        .edit { remove(KEY_SEARCH_HISTORY) }
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Text(stringResource(R.string.projects_clear_history), color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
            // --- 场景 2：显示项目列表 (常规 或 搜索过滤中) ---
            else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Filter Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProjectTechType.entries.forEach { tech ->
                            val isSelected = selectedFilter == tech
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = tech },
                                label = { Text(tech.displayName) },
                                leadingIcon = if (tech != ProjectTechType.ALL) {
                                    {
                                        Icon(
                                            imageVector = tech.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else tech.badgeColor
                                        )
                                    }
                                } else null
                            )
                        }
                    }

                    if (displayList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isSearchActive) stringResource(R.string.projects_no_matching) else stringResource(R.string.projects_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            state = listState,
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = 4.dp,
                                end = 16.dp,
                                bottom = if (isSearchActive) 16.dp else 100.dp // 搜索时不需要底部留白给 FAB
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(displayList, key = { it.name }) { item ->
                                ProjectCard(
                                    modifier = Modifier.animateItem(
                                        placementSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ),
                                    item = item,
                                    isPinned = pinnedProjects.contains(item.name),
                                    onClick = {
                                        // 点击项目时，如果需要保存此次搜索记录，可以在这里调用 saveSearchHistory(searchQuery)
                                        navController.safeNavigate("code_edit/${item.name}")
                                    },
                                    onTogglePin = { togglePin(item.name) },
                                    onDelete = {
                                        projectToDelete = item.name
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
            }
        }

        // ... (弹窗代码保持不变) ...
        if (showDeleteDialog && projectToDelete != null) {
            val deleteTitleText = stringResource(R.string.projects_delete_title)
            val deleteConfirmText = stringResource(R.string.projects_delete_confirm, projectToDelete ?: "")
            val deleteActionText = stringResource(R.string.action_delete)
            val cancelText = stringResource(R.string.action_cancel)
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(deleteTitleText) },
                text = { Text(deleteConfirmText) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deleteProject(projectToDelete!!)
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text(deleteActionText) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text(cancelText) }
                }
            )
        }
    }
}

@Composable
fun ProjectCard(
    modifier: Modifier = Modifier,
    item: ProjectItem,
    isPinned: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val moreOptionsDescription = stringResource(R.string.action_more_options)
    val pinText = stringResource(R.string.projects_pin)
    val unpinText = stringResource(R.string.projects_unpin)
    val deleteTitleText = stringResource(R.string.projects_delete_title)
    val pinnedDescription = stringResource(R.string.content_desc_pinned)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(
            1.dp,
            if (isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App / Tech Icon Container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.techType.badgeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.techType.icon,
                    contentDescription = item.techType.displayName,
                    tint = item.techType.badgeColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = pinnedDescription,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(45f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tech Badge Pill
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = item.techType.badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.techType.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = item.techType.badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Git branch if available
                    if (!item.gitBranch.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = SimpleIcons.Git,
                                contentDescription = null,
                                tint = Color(0xFFF05032),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = item.gitBranch,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Relative time
                    Text(
                        text = "•  ${formatRelativeTime(item.lastModified)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // Dropdown menu
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = moreOptionsDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isPinned) unpinText else pinText) },
                        leadingIcon = {
                            Icon(if (isPinned) Icons.Default.PushPin else Icons.Default.VerticalAlignTop, null)
                        },
                        onClick = {
                            menuExpanded = false
                            onTogglePin()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(deleteTitleText, color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
