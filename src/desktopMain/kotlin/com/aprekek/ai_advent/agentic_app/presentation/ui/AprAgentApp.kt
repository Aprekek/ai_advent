package com.aprekek.ai_advent.agentic_app.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.darkColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMode
import com.aprekek.ai_advent.agentic_app.domain.model.StateMachineDoneStatus
import com.aprekek.ai_advent.agentic_app.domain.model.StateMachineSession
import com.aprekek.ai_advent.agentic_app.domain.model.StateMachineStage
import com.aprekek.ai_advent.agentic_app.domain.model.ThemeMode
import com.aprekek.ai_advent.agentic_app.presentation.state.AppViewModel
import java.awt.Cursor

@Composable
fun AprAgentApp(viewModel: AppViewModel) {
    val state = viewModel.state
    val isDark = state.themeMode == ThemeMode.DARK
    val colors = if (isDark) {
        darkColors(
            primary = Color(0xFF86B7FF),
            secondary = Color(0xFF64D8CB),
            background = Color(0xFF121417),
            surface = Color(0xFF1A1E23)
        )
    } else {
        lightColors(
            primary = Color(0xFF0F62FE),
            secondary = Color(0xFF007D79),
            background = Color(0xFFF6F8FB),
            surface = Color(0xFFFFFFFF)
        )
    }

    var messageInput by remember { mutableStateOf("") }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showCreateChatDialog by remember { mutableStateOf(false) }
    var editProfileId by remember { mutableStateOf<String?>(null) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showChatContextDialog by remember { mutableStateOf(false) }
    val editingProfile = editProfileId?.let { id -> state.profiles.firstOrNull { it.id == id } }
    val selectedChatContextItems = state.chats
        .firstOrNull { it.id == state.selectedChatId }
        ?.contextItems
        ?.map { it.value }
        .orEmpty()

    MaterialTheme(colors = colors) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
            Toolbar(
                state = state,
                onCreateProfileClick = { showProfileDialog = true },
                onEditProfileClick = { profileId -> editProfileId = profileId },
                onDeleteProfileClick = viewModel::deleteProfile,
                onProfileSelected = viewModel::switchProfile,
                onCreateChatClick = { showCreateChatDialog = true },
                onApiKeyClick = { showApiKeyDialog = true },
                onToggleTheme = viewModel::toggleTheme,
                onToggleLeftPanel = {
                    viewModel.updatePanelLayout(
                        state.panelLayoutState.copy(
                            leftPanelVisible = !state.panelLayoutState.leftPanelVisible
                        )
                    )
                },
                onToggleRightPanel = {
                    viewModel.updatePanelLayout(
                        state.panelLayoutState.copy(
                            rightPanelVisible = !state.panelLayoutState.rightPanelVisible
                        )
                    )
                }
            )

            if (state.errorMessage != null) {
                ErrorBanner(
                    message = state.errorMessage,
                    onClose = viewModel::clearError
                )
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                DesktopLayout(
                    state = state,
                    messageInput = messageInput,
                    onMessageInput = { messageInput = it },
                    onSendMessage = {
                        viewModel.sendMessage(messageInput)
                        messageInput = ""
                    },
                    onStopMessage = viewModel::stopStreaming,
                    onStateMachineApprovePlan = viewModel::stateMachineApprovePlan,
                    onStateMachineSkipClarification = viewModel::stateMachineSkipClarification,
                    onStateMachineContinue = viewModel::stateMachineContinue,
                    onStateMachineValidationRework = viewModel::stateMachineValidationRework,
                    onStateMachineValidationAcceptCurrent = viewModel::stateMachineValidationAcceptCurrent,
                    onChatSelected = viewModel::selectChat,
                    onDeleteChat = viewModel::deleteChat,
                    chatContextItems = selectedChatContextItems,
                    onOpenChatContext = { showChatContextDialog = true },
                    onResizeLeft = { dragDeltaPx ->
                        val current = state.panelLayoutState
                        val updatedWidth = (current.leftPanelWidthPx + dragDeltaPx).coerceIn(220f, 460f)
                        viewModel.updatePanelLayout(current.copy(leftPanelWidthPx = updatedWidth))
                    },
                    onResizeRight = { dragDeltaPx ->
                        val current = state.panelLayoutState
                        val updatedWidth = (current.rightPanelWidthPx - dragDeltaPx).coerceIn(220f, 500f)
                        viewModel.updatePanelLayout(current.copy(rightPanelWidthPx = updatedWidth))
                    }
                )
            }
        }

        if (showProfileDialog) {
            ProfileDialog(
                title = "Новый профиль",
                confirmTitle = "Создать",
                initialName = "",
                initialDescriptionItems = emptyList(),
                onDismiss = { showProfileDialog = false },
                onConfirm = { name, items ->
                    viewModel.createProfile(name, items)
                    showProfileDialog = false
                }
            )
        }

        if (editingProfile != null) {
            ProfileDialog(
                title = "Редактировать профиль",
                confirmTitle = "Сохранить",
                initialName = editingProfile.name,
                initialDescriptionItems = editingProfile.descriptionItems.map { it.value },
                onDismiss = { editProfileId = null },
                onConfirm = { name, items ->
                    viewModel.updateProfile(editingProfile.id, name, items)
                    editProfileId = null
                }
            )
        }

        if (showApiKeyDialog) {
            ApiKeyDialog(
                onDismiss = { showApiKeyDialog = false },
                onConfirm = {
                    viewModel.saveApiKey(it)
                    showApiKeyDialog = false
                }
            )
        }

        if (showCreateChatDialog) {
            CreateChatDialog(
                onDismiss = { showCreateChatDialog = false },
                onConfirm = { mode ->
                    viewModel.createChat(mode)
                    showCreateChatDialog = false
                }
            )
        }

        if (showChatContextDialog) {
            ChatContextDialog(
                chatId = state.selectedChatId,
                initialItems = selectedChatContextItems,
                onDismiss = { showChatContextDialog = false },
                onSave = { items ->
                    viewModel.updateSelectedChatContextItems(items)
                    showChatContextDialog = false
                }
            )
        }
    }
}

@Composable
private fun Toolbar(
    state: com.aprekek.ai_advent.agentic_app.presentation.state.AppUiState,
    onCreateProfileClick: () -> Unit,
    onEditProfileClick: (String) -> Unit,
    onDeleteProfileClick: (String) -> Unit,
    onProfileSelected: (String) -> Unit,
    onCreateChatClick: () -> Unit,
    onApiKeyClick: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleLeftPanel: () -> Unit,
    onToggleRightPanel: () -> Unit
) {
    var showProfilesMenu by remember { mutableStateOf(false) }
    val activeProfileName = state.profiles.firstOrNull { it.id == state.activeProfileId }?.name ?: "Profile"

    Row(
        modifier = Modifier.fillMaxWidth().padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            OutlinedButton(onClick = { showProfilesMenu = true }) {
                Text("Профиль: $activeProfileName")
            }

            DropdownMenu(
                expanded = showProfilesMenu,
                onDismissRequest = { showProfilesMenu = false }
            ) {
                state.profiles.forEach { profile ->
                    DropdownMenuItem(onClick = {
                        showProfilesMenu = false
                        onProfileSelected(profile.id)
                    }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(profile.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                showProfilesMenu = false
                                onEditProfileClick(profile.id)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Редактировать профиль ${profile.name}"
                                )
                            }
                            IconButton(onClick = {
                                showProfilesMenu = false
                                onDeleteProfileClick(profile.id)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Удалить профиль ${profile.name}"
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedButton(onClick = onCreateProfileClick) {
            Text("Новый профиль")
        }
        OutlinedButton(onClick = onCreateChatClick) {
            Text("Новый чат")
        }
        OutlinedButton(onClick = onApiKeyClick) {
            Text(if (state.hasApiKey) "API key: настроен" else "Добавить API key")
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(onClick = onToggleLeftPanel) {
            Text("Левая панель")
        }
        OutlinedButton(onClick = onToggleRightPanel) {
            Text("Правая панель")
        }
        OutlinedButton(onClick = onToggleTheme) {
            Text(if (state.themeMode == ThemeMode.DARK) "Светлая" else "Тёмная")
        }
    }

    Divider()
}

@Composable
private fun ErrorBanner(
    message: String,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFE4E4)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = Color(0xFF5A0000)
            )
            TextButton(onClick = onClose) {
                Text("Скрыть")
            }
        }
    }
}

@Composable
private fun DesktopLayout(
    state: com.aprekek.ai_advent.agentic_app.presentation.state.AppUiState,
    messageInput: String,
    onMessageInput: (String) -> Unit,
    onSendMessage: () -> Unit,
    onStopMessage: () -> Unit,
    onStateMachineApprovePlan: () -> Unit,
    onStateMachineSkipClarification: () -> Unit,
    onStateMachineContinue: () -> Unit,
    onStateMachineValidationRework: () -> Unit,
    onStateMachineValidationAcceptCurrent: () -> Unit,
    onChatSelected: (String) -> Unit,
    onDeleteChat: (String) -> Unit,
    chatContextItems: List<String>,
    onOpenChatContext: () -> Unit,
    onResizeLeft: (Float) -> Unit,
    onResizeRight: (Float) -> Unit
) {
    val density = LocalDensity.current

    Row(modifier = Modifier.fillMaxSize()) {
        if (state.panelLayoutState.leftPanelVisible) {
            val leftWidth = with(density) { state.panelLayoutState.leftPanelWidthPx.toDp() }
            ChatListPanel(
                modifier = Modifier.width(leftWidth).fillMaxHeight(),
                state = state,
                onChatSelected = onChatSelected,
                onDeleteChat = onDeleteChat
            )
            VerticalSplitter(onDrag = onResizeLeft)
        }

        ChatPanel(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            state = state,
            messageInput = messageInput,
            onMessageInput = onMessageInput,
            onSend = onSendMessage,
            onStop = onStopMessage,
            onStateMachineApprovePlan = onStateMachineApprovePlan,
            onStateMachineSkipClarification = onStateMachineSkipClarification,
            onStateMachineContinue = onStateMachineContinue,
            onStateMachineValidationRework = onStateMachineValidationRework,
            onStateMachineValidationAcceptCurrent = onStateMachineValidationAcceptCurrent
        )

        if (state.panelLayoutState.rightPanelVisible) {
            VerticalSplitter(onDrag = onResizeRight)
            val rightWidth = with(density) { state.panelLayoutState.rightPanelWidthPx.toDp() }
            RightToolsPanel(
                modifier = Modifier.width(rightWidth).fillMaxHeight(),
                chatId = state.selectedChatId,
                contextItems = chatContextItems,
                onOpenChatContext = onOpenChatContext
            )
        }
    }
}

@Composable
private fun ChatListPanel(
    modifier: Modifier,
    state: com.aprekek.ai_advent.agentic_app.presentation.state.AppUiState,
    onChatSelected: (String) -> Unit,
    onDeleteChat: (String) -> Unit
) {
    Surface(modifier = modifier, color = MaterialTheme.colors.surface) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Чаты",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
            Divider()
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.chats) { chat ->
                    val selected = chat.id == state.selectedChatId
                    val bg = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else Color.Transparent
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bg)
                            .clickable { onChatSelected(chat.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (chat.mode == ChatMode.STATE_MACHINE) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "State machine chat",
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                            Text(chat.title, maxLines = 1, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onDeleteChat(chat.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Удалить чат"
                                )
                            }
                        }
                    }
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun ChatPanel(
    modifier: Modifier,
    state: com.aprekek.ai_advent.agentic_app.presentation.state.AppUiState,
    messageInput: String,
    onMessageInput: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onStateMachineApprovePlan: () -> Unit,
    onStateMachineSkipClarification: () -> Unit,
    onStateMachineContinue: () -> Unit,
    onStateMachineValidationRework: () -> Unit,
    onStateMachineValidationAcceptCurrent: () -> Unit
) {
    val listState = rememberLazyListState()
    val messages = state.messages
    var autoScrollEnabled by remember(state.selectedChatId) { mutableStateOf(true) }
    var isProgrammaticScroll by remember(state.selectedChatId) { mutableStateOf(false) }
    val lastMessageContent = messages.lastOrNull()?.content.orEmpty()
    val nestedScrollConnection = remember(state.selectedChatId, listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.Drag || source == NestedScrollSource.Fling) {
                    autoScrollEnabled = false
                }
                return Offset.Zero
            }
        }
    }
    val fsm = state.stateMachineSession
    val isStateMachineChat = state.selectedChatMode == ChatMode.STATE_MACHINE
    val stopEnabled = if (isStateMachineChat) {
        val stage = fsm?.stage
        stage == StateMachineStage.PLANNING ||
            stage == StateMachineStage.CLARIFICATION ||
            stage == StateMachineStage.EXECUTION ||
            stage == StateMachineStage.VALIDATION ||
            state.isStreaming
    } else {
        state.isStreaming
    }
    val canTypeInInput = when {
        state.isStreaming -> false
        !isStateMachineChat -> true
        fsm == null -> true
        else -> fsm.waitingForUserInput &&
            (fsm.stage == StateMachineStage.PLANNING || fsm.stage == StateMachineStage.CLARIFICATION || fsm.stage == StateMachineStage.DONE)
    }

    LaunchedEffect(state.selectedChatId) {
        autoScrollEnabled = true
    }

    LaunchedEffect(messages.size, lastMessageContent, state.selectedChatId, autoScrollEnabled) {
        if (messages.isEmpty() || state.selectedChatId == null) return@LaunchedEffect
        if (!autoScrollEnabled) return@LaunchedEffect
        val targetIndex = messages.lastIndex
        if (targetIndex < 0) return@LaunchedEffect
        isProgrammaticScroll = true
        runCatching { listState.scrollToItem(targetIndex) }
        isProgrammaticScroll = false
    }

    LaunchedEffect(listState, state.selectedChatId) {
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.isScrollInProgress
            )
        }.collect { (_, _, isScrolling) ->
            val atBottom = isAtBottom(listState)
            if (isScrolling && !isProgrammaticScroll) {
                // Any manual scroll activity (including fast wheel/fling) disables auto-follow.
                autoScrollEnabled = false
            } else if (atBottom && !isScrolling) {
                autoScrollEnabled = true
            } else if (!atBottom && !isProgrammaticScroll) {
                autoScrollEnabled = false
            }
        }
    }

    Surface(modifier = modifier, color = MaterialTheme.colors.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (state.selectedChatId == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Выберите чат слева или создайте новый")
                }
            } else {
                if (isStateMachineChat && fsm != null) {
                    StateMachineHeader(
                        session = fsm
                    )
                } else if (isStateMachineChat) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        color = MaterialTheme.colors.surface
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.HourglassTop, contentDescription = "Planning")
                            Text("Planning: отправьте первичный промпт")
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .nestedScroll(nestedScrollConnection),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    state = listState
                ) {
                    items(messages) { msg ->
                        val isUser = msg.role == ChatRole.USER
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                color = if (isUser) {
                                    MaterialTheme.colors.primary.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colors.surface
                                }
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = msg.content,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (state.isStreaming && state.isAwaitingFirstToken) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(color = MaterialTheme.colors.surface) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.width(16.dp))
                                        Text("Агент думает...")
                                    }
                                }
                            }
                        }
                    }
                    if (isStateMachineChat && fsm != null) {
                        item {
                            StateMachineInlineActions(
                                session = fsm,
                                isStreaming = state.isStreaming,
                                onApprovePlan = onStateMachineApprovePlan,
                                onSkipClarification = onStateMachineSkipClarification,
                                onContinue = onStateMachineContinue,
                                onValidationRework = onStateMachineValidationRework,
                                onValidationAcceptCurrent = onStateMachineValidationAcceptCurrent
                            )
                        }
                    }
                }
            }

            Divider()

            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                if (state.reconnectAttempt != null) {
                    Text(
                        text = "Переподключение к SSE... попытка ${state.reconnectAttempt}",
                        color = MaterialTheme.colors.secondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = messageInput,
                        onValueChange = onMessageInput,
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .onPreviewKeyEvent { keyEvent ->
                                val isCommandEnter =
                                    keyEvent.type == KeyEventType.KeyDown &&
                                        keyEvent.isMetaPressed &&
                                        (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                if (isCommandEnter && canTypeInInput && messageInput.isNotBlank() && state.selectedChatId != null) {
                                    onSend()
                                    true
                                } else {
                                    false
                                }
                            },
                        enabled = canTypeInInput,
                        placeholder = {
                            if (canTypeInInput) {
                                Text("Введите сообщение...")
                            } else {
                                Text("Ожидание этапа state-machine...")
                            }
                        }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onSend,
                            enabled = canTypeInInput && messageInput.isNotBlank() && state.selectedChatId != null
                        ) {
                            Text("Отправить")
                        }
                        OutlinedButton(
                            onClick = onStop,
                            enabled = stopEnabled
                        ) {
                            Text("Стоп")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StateMachineHeader(
    session: StateMachineSession
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        color = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StageBadge(title = "Planning", status = mainStageStatus(MainStage.PLANNING, session))
                Text("→")
                StageBadge(title = "Execution", status = mainStageStatus(MainStage.EXECUTION, session))
                Text("→")
                StageBadge(title = "Validation", status = mainStageStatus(MainStage.VALIDATION, session))
                Text("→")
                StageBadge(title = "Done", status = mainStageStatus(MainStage.DONE, session))
            }

            if (session.stage == StateMachineStage.DONE) {
                val done = when (session.doneStatus ?: StateMachineDoneStatus.DONE) {
                    StateMachineDoneStatus.DONE -> "Done"
                    StateMachineDoneStatus.CANCELED -> "Canceled"
                    StateMachineDoneStatus.FAILED -> "Failed: ${session.lastFailureReason.orEmpty()}"
                }
                Text(done)
            }
        }
    }
}

@Composable
private fun StateMachineInlineActions(
    session: StateMachineSession,
    isStreaming: Boolean,
    onApprovePlan: () -> Unit,
    onSkipClarification: () -> Unit,
    onContinue: () -> Unit,
    onValidationRework: () -> Unit,
    onValidationAcceptCurrent: () -> Unit
) {
    val showApprove = !isStreaming &&
        session.waitingForUserInput &&
        (session.stage == StateMachineStage.PLANNING || session.stage == StateMachineStage.CLARIFICATION) &&
        session.planDraft.isNotBlank() &&
        session.hasFullContext
    val showSkip = !isStreaming &&
        session.waitingForUserInput &&
        (session.stage == StateMachineStage.PLANNING || session.stage == StateMachineStage.CLARIFICATION) &&
        session.planDraft.isNotBlank() &&
        !session.hasFullContext
    val showContinue = !isStreaming &&
        !session.waitingForUserInput &&
        (session.stage == StateMachineStage.EXECUTION || session.stage == StateMachineStage.VALIDATION)
    val showValidationChoice = !isStreaming &&
        session.stage == StateMachineStage.VALIDATION &&
        session.waitingForUserInput

    if (!showApprove && !showSkip && !showContinue && !showValidationChoice) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(color = MaterialTheme.colors.primary.copy(alpha = 0.12f)) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (showApprove) {
                    OutlinedButton(onClick = onApprovePlan) {
                        Text("Перейти к выполнению")
                    }
                }
                if (showSkip) {
                    OutlinedButton(onClick = onSkipClarification) {
                        Text("Пропустить уточнения и перейти к выполнению с текущим контекстом")
                    }
                }
                if (showContinue) {
                    OutlinedButton(onClick = onContinue) {
                        Text("Продолжить выполнение")
                    }
                }
                if (showValidationChoice) {
                    OutlinedButton(onClick = onValidationRework) {
                        Text("Переделать execution")
                    }
                    OutlinedButton(onClick = onValidationAcceptCurrent) {
                        Text("Принять текущее")
                    }
                }
            }
        }
    }
}

@Composable
private fun StageBadge(
    title: String,
    status: StageVisualStatus
) {
    val icon = when (status) {
        StageVisualStatus.PENDING -> Icons.Default.RadioButtonUnchecked
        StageVisualStatus.IN_PROGRESS -> Icons.Default.HourglassTop
        StageVisualStatus.COMPLETED -> Icons.Default.CheckCircle
        StageVisualStatus.CANCELED -> Icons.Default.Cancel
        StageVisualStatus.FAILED -> Icons.Default.Error
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = title)
        Text(title)
    }
}

private enum class MainStage {
    PLANNING, EXECUTION, VALIDATION, DONE
}

private enum class StageVisualStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELED,
    FAILED
}

private fun mainStageStatus(mainStage: MainStage, session: StateMachineSession): StageVisualStatus {
    if (mainStage == MainStage.DONE && session.stage == StateMachineStage.DONE) {
        return when (session.doneStatus ?: StateMachineDoneStatus.DONE) {
            StateMachineDoneStatus.DONE -> StageVisualStatus.COMPLETED
            StateMachineDoneStatus.CANCELED -> StageVisualStatus.CANCELED
            StateMachineDoneStatus.FAILED -> StageVisualStatus.FAILED
        }
    }

    val currentMainStage = when (session.stage) {
        StateMachineStage.PLANNING, StateMachineStage.CLARIFICATION -> MainStage.PLANNING
        StateMachineStage.EXECUTION -> MainStage.EXECUTION
        StateMachineStage.VALIDATION -> MainStage.VALIDATION
        StateMachineStage.DONE -> MainStage.DONE
    }

    return when {
        mainStage.ordinal < currentMainStage.ordinal -> StageVisualStatus.COMPLETED
        mainStage.ordinal == currentMainStage.ordinal && mainStage != MainStage.DONE -> StageVisualStatus.IN_PROGRESS
        else -> StageVisualStatus.PENDING
    }
}

@Composable
private fun RightToolsPanel(
    modifier: Modifier,
    chatId: String?,
    contextItems: List<String>,
    onOpenChatContext: () -> Unit
) {
    Surface(modifier = modifier, color = MaterialTheme.colors.surface) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Инструменты чата", style = MaterialTheme.typography.h6)
            Divider()

            if (chatId == null) {
                Text("Выберите чат слева")
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colors.primary.copy(alpha = 0.08f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Chat Context", style = MaterialTheme.typography.subtitle1)
                        Text(
                            text = summarizeContextItems(contextItems),
                            style = MaterialTheme.typography.body2
                        )
                        OutlinedButton(onClick = onOpenChatContext) {
                            Text("Chat Context")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatContextDialog(
    chatId: String?,
    initialItems: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var draft by remember(chatId) { mutableStateOf("") }
    val editableItems = remember(chatId, initialItems) {
        mutableStateListOf<String>().apply { addAll(initialItems) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chat Context") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (chatId == null) {
                    Text("Выберите чат слева")
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            label = { Text("Context item") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                val normalized = draft.trim()
                                if (normalized.isNotEmpty()) {
                                    editableItems.add(normalized)
                                    draft = ""
                                }
                            }
                        ) {
                            Text("Добавить")
                        }
                    }

                    editableItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = item,
                                onValueChange = { updated -> editableItems[index] = updated },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                if (index in editableItems.indices) {
                                    editableItems.removeAt(index)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Удалить context item"
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(editableItems.toList()) },
                enabled = chatId != null
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

private fun summarizeContextItems(items: List<String>): String {
    if (items.isEmpty()) return "Контекст пока не добавлен."
    return items.joinToString(separator = ", ")
}

private fun isAtBottom(listState: androidx.compose.foundation.lazy.LazyListState): Boolean {
    val layoutInfo = listState.layoutInfo
    val totalCount = layoutInfo.totalItemsCount
    if (totalCount == 0) return true
    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull() ?: return false
    if (lastVisible.index != totalCount - 1) return false
    val viewportEnd = layoutInfo.viewportEndOffset
    val lastItemEnd = lastVisible.offset + lastVisible.size
    return lastItemEnd <= viewportEnd
}

@Composable
private fun VerticalSplitter(onDrag: (Float) -> Unit) {
    val dragState = rememberDraggableState { delta -> onDrag(delta) }
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(6.dp)
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal
            )
    )
}

@Composable
private fun ProfileDialog(
    title: String,
    confirmTitle: String,
    initialName: String,
    initialDescriptionItems: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var descriptionDraft by remember { mutableStateOf("") }
    val descriptionItems = remember(initialDescriptionItems) {
        mutableStateListOf<String>().apply { addAll(initialDescriptionItems) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = descriptionDraft,
                        onValueChange = { descriptionDraft = it },
                        label = { Text("Profile description item") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            val normalized = descriptionDraft.trim()
                            if (normalized.isNotEmpty()) {
                                descriptionItems.add(normalized)
                                descriptionDraft = ""
                            }
                        }
                    ) {
                        Text("Добавить")
                    }
                }

                if (descriptionItems.isNotEmpty()) {
                    Text("Список description items")
                    descriptionItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = item,
                                onValueChange = { updated -> descriptionItems[index] = updated },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                if (index in descriptionItems.indices) {
                                    descriptionItems.removeAt(index)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Удалить description item"
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, descriptionItems.toList()) },
                enabled = name.isNotBlank()
            ) {
                Text(confirmTitle)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun ApiKeyDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("DeepSeek API key") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun CreateChatDialog(
    onDismiss: () -> Unit,
    onConfirm: (ChatMode) -> Unit
) {
    var mode by remember { mutableStateOf(ChatMode.STANDARD) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый чат") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { mode = ChatMode.STANDARD },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (mode == ChatMode.STANDARD) "Standard ✓" else "Standard")
                }
                OutlinedButton(
                    onClick = { mode = ChatMode.STATE_MACHINE },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (mode == ChatMode.STATE_MACHINE) "State machine ✓" else "State machine")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(mode) }) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
