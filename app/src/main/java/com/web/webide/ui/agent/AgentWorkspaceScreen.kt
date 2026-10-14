package com.web.webide.ui.agent

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.web.webide.R
import com.web.webide.model.*
import com.web.webide.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentWorkspaceScreen(
    navController: NavController,
    projectPath: String? = null,
    agentViewModel: AgentViewModel = viewModel()
) {
    val state by agentViewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val chatListState = rememberLazyListState()

    LaunchedEffect(projectPath) {
        if (!projectPath.isNullOrBlank()) {
            val name = java.io.File(projectPath).name
            agentViewModel.openProject(
                Project(
                    id = projectPath,
                    name = name,
                    path = projectPath,
                    kind = ProjectKind.WEB,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    var showGitHubDialog by rememberSaveable { mutableStateOf(false) }

    if (showGitHubDialog) {
        com.web.webide.ui.components.GitHubCloneDialog(
            state = state,
            onStartGitHubLogin = { agentViewModel.startGitHubLogin() },
            onGenerateNewGitHubCode = { agentViewModel.generateNewGitHubCode() },
            onRefreshGitHub = { agentViewModel.refreshGitHubRepositories() },
            onDisconnectGitHub = { agentViewModel.disconnectGitHub() },
            onCloneGitHub = { repo ->
                agentViewModel.cloneGitHubRepository(repo)
                showGitHubDialog = false
            },
            onCloneGitUrl = { url ->
                agentViewModel.clonePublicGitRepository(url)
                showGitHubDialog = false
            },
            onDismiss = { showGitHubDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.activeProject?.name ?: "WebIDE AI Agent",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (state.isRunning) PocketOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = state.selectedAgent.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (state.isRunning) PocketOrange else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (state.runtimeInstalled) "Ubuntu 20.04 LTS Subsystem Ready" else "Ubuntu Subsystem Setup Needed",
                            fontSize = 11.sp,
                            color = if (state.runtimeInstalled) PocketGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showGitHubDialog = true
                        if (state.githubAuthStatus == com.web.webide.ui.agent.GitHubAuthStatus.CONNECTED && state.githubRepositories.isEmpty()) {
                            agentViewModel.refreshGitHubRepositories()
                        }
                    }) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "GitHub Repositories")
                    }
                    IconButton(onClick = { navController.navigate("terminal_project") }) {
                        Icon(Icons.Default.Terminal, contentDescription = "Open CLI Terminal")
                    }
                    IconButton(onClick = { selectedTab = 1 }) {
                        Icon(Icons.Default.Tune, contentDescription = "AI Setup")
                    }
                    IconButton(onClick = { selectedTab = 2 }) {
                        Icon(Icons.Default.Memory, contentDescription = "Ubuntu & Packages")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Chat, null) },
                    label = { Text("Агент") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AutoAwesome, null) },
                    label = { Text("Провайдеры") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Memory, null) },
                    label = { Text("Ubuntu / Пакеты") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Difference, null) },
                    label = { Text("Diff") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> AgentChatTab(
                    state = state,
                    listState = chatListState,
                    onSend = { prompt ->
                        agentViewModel.sendMessage(prompt)
                    },
                    onStop = { agentViewModel.stopRunningTask() },
                    onRunInTerminal = { cmd ->
                        navController.navigate("terminal")
                    }
                )
                1 -> AgentScreen(
                    state = state,
                    onSaveProvider = { profile, key ->
                        agentViewModel.saveProviderProfile(profile, key)
                    },
                    onDiscoverModels = { profile, key ->
                        agentViewModel.discoverModels(profile, key)
                    },
                    onValidateProvider = { profile, key, models ->
                        agentViewModel.validateProvider(profile, key, models)
                    },
                    onPing = { agentViewModel.pingProvider() },
                    getSavedApiKey = { kind -> agentViewModel.getApiKey(kind) },
                    getSavedApiKeys = { kind -> agentViewModel.getApiKeys(kind) },
                    onAddApiKey = { kind, label, key -> agentViewModel.addApiKey(kind, label, key) },
                    onActivateApiKey = { kind, id -> agentViewModel.activateApiKey(kind, id) },
                    onRemoveApiKey = { kind, id -> agentViewModel.removeApiKey(kind, id) },
                    onSelectAgent = { agent -> agentViewModel.selectAgent(agent) },
                    onInstallAgent = { agent -> agentViewModel.installAgent(agent) },
                    onCheckAgentUpdates = { agentViewModel.checkForAgentUpdates() },
                    onUpdateAgent = { agent -> agentViewModel.updateAgent(agent) },
                    onStartAntigravityLogin = { agentViewModel.startAntigravityLogin() },
                    onSubmitAntigravityCode = { agentViewModel.submitAntigravityCode(it) },
                    onLogoutAntigravity = { agentViewModel.logoutAntigravity() },
                    onRefreshAntigravityModels = { agentViewModel.refreshAntigravityModels() },
                    onSetAntigravityModel = { agentViewModel.setAntigravityModel(it) },
                    onSetAntigravityEffort = { agentViewModel.setAntigravityEffort(it) }
                )
                2 -> UbuntuSubsystemTab(
                    state = state,
                    onInstallRuntime = { agentViewModel.startRuntimeInstall() },
                    onInstallStack = { stack -> agentViewModel.installDevStack(stack) },
                    onRemoveStack = { stack -> agentViewModel.removeDevStack(stack) }
                )
                3 -> ChangesTab(
                    changes = state.activeChanges,
                    onUndo = { agentViewModel.undoAllChanges() },
                    onKeep = { agentViewModel.keepAllChanges() },
                    onUndoFile = { path -> agentViewModel.undoFileChange(path) },
                    onKeepFile = { path -> agentViewModel.keepFileChange(path) }
                )
            }
        }
    }
}

@Composable
fun AgentChatTab(
    state: AppUiState,
    listState: LazyListState,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onRunInTerminal: (String) -> Unit
) {
    var prompt by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        if (!state.runtimeInstalled) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Подсистема Ubuntu не установлена. Перейдите во вкладку 'Ubuntu / Пакеты' для установки.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Box(Modifier.weight(1f)) {
            if (state.projectMessages.isEmpty() && state.liveProcess.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = PocketOrange
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "ИИ-Ассистент разработки",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Задайте задачу агенту: написать код, исправить ошибку, запустить сервер или выполнить команды Linux в Ubuntu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = { prompt = "Создай интерактивный счетчик в index.html" },
                            label = { Text("Создать счетчик") }
                        )
                        SuggestionChip(
                            onClick = { prompt = "Запусти локальный сервер и покажи предпросмотр" },
                            label = { Text("Запустить веб-сервер") }
                        )
                        SuggestionChip(
                            onClick = { prompt = "Проверь проект на ошибки и исправь" },
                            label = { Text("Исправить ошибки") }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.projectMessages, key = { it.id }) { message ->
                        MessageBubble(message, onRunInTerminal)
                    }
                    if (state.liveProcess.isNotEmpty() || state.thinkingActive) {
                        item(key = "live-process") {
                            LiveProcessCard(state.liveProcess, state.isRunning, state.thinkingActive)
                        }
                    }
                }
            }
        }

        // Input Bar
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Опишите задачу агенту...") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp)
                )
                if (state.isRunning) {
                    IconButton(
                        onClick = onStop,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (prompt.isNotBlank()) {
                                val text = prompt
                                prompt = ""
                                onSend(text)
                            }
                        },
                        enabled = prompt.isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = PocketOrange)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, onRunInTerminal: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.fromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                SelectionContainer {
                    if (message.fromUser) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        MarkdownText(
                            markdown = message.text,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            onRunCode = onRunInTerminal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveProcessCard(items: List<ActivityItem>, isRunning: Boolean, thinkingActive: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PocketOrange)
                } else {
                    Icon(Icons.Default.CheckCircle, null, tint = PocketGreen, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (thinkingActive) "Агент размышляет..." else "Выполнение инструкций...",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (items.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                items.takeLast(5).forEach { item ->
                    Text(
                        text = "• ${item.title}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun UbuntuSubsystemTab(
    state: AppUiState,
    onInstallRuntime: () -> Unit,
    onInstallStack: (DevStack) -> Unit,
    onRemoveStack: (DevStack) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = PocketOrange, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Ubuntu 20.04 LTS (PRoot)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (state.runtimeInstalled) "Установлено и готово к работе" else "Требуется загрузка подсистемы",
                            fontSize = 12.sp,
                            color = if (state.runtimeInstalled) PocketGreen else MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Изолированная Linux-среда PRoot с поддержкой Node.js, Python, Git, менеджера APT пакетов и компиляторов.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onInstallRuntime,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PocketOrange)
                ) {
                    Text(if (state.runtimeInstalled) "Переустановить Ubuntu" else "Установить Ubuntu 20.04 LTS")
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Инструменты разработки (DevStacks)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DevStack.entries) { stack ->
                val installed = stack in state.installedStacks
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stack.localizedLabel(), fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (installed) "Установлен" else "Не установлен",
                                fontSize = 11.sp,
                                color = if (installed) PocketGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (installed) {
                            OutlinedButton(onClick = { onRemoveStack(stack) }) {
                                Text("Удалить")
                            }
                        } else {
                            Button(onClick = { onInstallStack(stack) }) {
                                Text("Установить")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChangesTab(
    changes: List<ChangeItem>,
    onUndo: () -> Unit,
    onKeep: () -> Unit,
    onUndoFile: (String) -> Unit,
    onKeepFile: (String) -> Unit
) {
    if (changes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Difference, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("Нет несохраненных изменений", fontWeight = FontWeight.Medium)
                Text("Изменения файлов агентом будут отображаться здесь.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(changes, key = { it.path }) { change ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, null)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(change.path, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text("+${change.additions} / -${change.deletions}", fontSize = 11.sp, color = PocketGreen)
                        }
                        IconButton(onClick = { onUndoFile(change.path) }) {
                            Icon(Icons.Default.Undo, "Undo")
                        }
                    }
                }
            }
        }
    }
}
