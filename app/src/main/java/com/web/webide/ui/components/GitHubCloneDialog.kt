package com.web.webide.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.web.webide.network.GitHubRepository
import com.web.webide.ui.agent.AppUiState
import com.web.webide.ui.agent.GitHubAuthStatus

private val GitHubOrange = Color(0xFFF28C52)
private val GitHubGreen = Color(0xFF3DDC84)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubCloneDialog(
    state: AppUiState,
    onStartGitHubLogin: () -> Unit,
    onGenerateNewGitHubCode: () -> Unit,
    onRefreshGitHub: () -> Unit,
    onDisconnectGitHub: () -> Unit,
    onCloneGitHub: (GitHubRepository) -> Unit,
    onCloneGitUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var customGitUrl by rememberSaveable { mutableStateOf("") }
    var showCustomUrlInput by rememberSaveable { mutableStateOf(false) }

    val filteredRepositories = remember(state.githubRepositories, searchQuery) {
        if (searchQuery.isBlank()) {
            state.githubRepositories
        } else {
            state.githubRepositories.filter { repo ->
                repo.fullName.contains(searchQuery, ignoreCase = true) ||
                    repo.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!state.gitCloneRunning) onDismiss()
        },
        icon = {
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = null,
                tint = if (state.githubAuthStatus == GitHubAuthStatus.CONNECTED) GitHubGreen else GitHubOrange,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (state.githubAuthStatus == GitHubAuthStatus.CONNECTED) {
                    "GitHub · @${state.githubLogin ?: "пользователь"}"
                } else {
                    "GitHub репозитории"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Если идёт клонирование
                if (state.gitCloneRunning) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                            Text(
                                text = state.gitCloneMessage ?: "Клонирование репозитория в рабочую область...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    when (state.githubAuthStatus) {
                        GitHubAuthStatus.DISCONNECTED, GitHubAuthStatus.ERROR -> {
                            if (state.githubAuthStatus == GitHubAuthStatus.ERROR && !state.githubMessage.isNullOrBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = state.githubMessage,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Войдите через GitHub Device Flow, чтобы мгновенно просматривать и скачивать свои проекты прямо в IDE.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = onStartGitHubLogin,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Login, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Войти в GitHub")
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = "Или укажите любой публичный Git URL:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            OutlinedTextField(
                                value = customGitUrl,
                                onValueChange = { customGitUrl = it },
                                placeholder = { Text("https://github.com/owner/repo.git", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                            )

                            OutlinedButton(
                                onClick = {
                                    if (customGitUrl.isNotBlank()) {
                                        onCloneGitUrl(customGitUrl)
                                    }
                                },
                                enabled = customGitUrl.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Клонировать по ссылке")
                            }
                        }

                        GitHubAuthStatus.STARTING -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                Text(
                                    text = state.githubMessage ?: "Подключение к GitHub...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        GitHubAuthStatus.AWAITING_USER -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "1. Скопируйте код и введите его на странице авторизации GitHub:",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            state.githubUserCode?.let {
                                                clipboard.setText(AnnotatedString(it))
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = state.githubUserCode.orEmpty(),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp,
                                            letterSpacing = 2.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Копировать",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Нажмите на код, чтобы скопировать",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = {
                                        val uri = Uri.parse(state.githubVerificationUri ?: "https://github.com/login/device")
                                        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        runCatching { context.startActivity(intent) }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Открыть страницу входа")
                                }

                                TextButton(
                                    onClick = onGenerateNewGitHubCode,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Сгенерировать новый код", fontSize = 12.sp)
                                }
                            }
                        }

                        GitHubAuthStatus.CONNECTED -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Поиск репозиториев...", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                                    )

                                    Spacer(Modifier.width(6.dp))

                                    IconButton(
                                        onClick = onRefreshGitHub,
                                        enabled = !state.githubRepositoriesLoading
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                                    }
                                }

                                if (state.githubRepositoriesLoading) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }

                                if (filteredRepositories.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (state.githubRepositories.isEmpty()) "Репозитории не найдены или загружаются..." else "Ничего не найдено",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 280.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(filteredRepositories, key = { it.fullName }) { repo ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onCloneGitHub(repo) },
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            if (repo.private) Icons.Default.Lock else Icons.Default.Code,
                                                            contentDescription = null,
                                                            tint = if (repo.private) GitHubOrange else MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            text = repo.fullName,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 13.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                        ) {
                                                            Text(
                                                                text = repo.defaultBranch,
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }

                                                    if (repo.description.isNotBlank()) {
                                                        Spacer(Modifier.height(4.dp))
                                                        Text(
                                                            text = repo.description,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showCustomUrlInput = !showCustomUrlInput }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (showCustomUrlInput) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Клонировать другой Git URL",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                AnimatedVisibility(visible = showCustomUrlInput) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = customGitUrl,
                                            onValueChange = { customGitUrl = it },
                                            placeholder = { Text("https://github.com/...", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                if (customGitUrl.isNotBlank()) onCloneGitUrl(customGitUrl)
                                            },
                                            enabled = customGitUrl.isNotBlank()
                                        ) {
                                            Icon(Icons.Default.Download, null)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (state.githubAuthStatus == GitHubAuthStatus.CONNECTED) {
                TextButton(onClick = onDismiss) {
                    Text("Закрыть")
                }
            }
        },
        dismissButton = {
            if (state.githubAuthStatus == GitHubAuthStatus.CONNECTED) {
                TextButton(onClick = { onDisconnectGitHub(); onDismiss() }) {
                    Text("Выйти из аккаунта", color = MaterialTheme.colorScheme.error)
                }
            } else if (!state.gitCloneRunning && state.githubAuthStatus != GitHubAuthStatus.STARTING) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        }
    )
}
