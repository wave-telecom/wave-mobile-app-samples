package br.com.wave.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import br.com.wave.flows.kmp.FlowWrapper
import br.com.wave.flows.kmp.FlowsSdkBuildInfo
import br.com.wave.flows.kmp.RenderBlock
import br.com.wave.flows.kmp.SDKEvent
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import androidx.compose.runtime.withFrameNanos

private const val SDK_TAG = "WaveSdkSample"
private const val SAMPLE_API_KEY =
    "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImR1MFVjYWxyZlNQckRCNU1qZ3VvMyJ9.eyJjbGllbnQiOiJ0ZWxjZWwtc3BlZWR5IiwiZW52aXJvbm1lbnQiOiJERVYiLCJpc3MiOiJodHRwczovL3dhdmUtdGVjaC1kZXYudXMuYXV0aDAuY29tLyIsInN1YiI6InJ0UFNJeTByOFlJT3hnYjJwakRWSzNZcFN3VmdQTGtRQGNsaWVudHMiLCJhdWQiOiJodHRwczovL2l6emktYWN0aXZhdGlvbi1kZXYtMGVkMi51Yy5yLmFwcHNwb3QuY29tLyIsImlhdCI6MTc3MjcxNzMxMCwiZXhwIjoxNzcyODAzNzEwLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMiLCJhenAiOiJydFBTSXkwcjhZSU94Z2IycGpEVkszWXBTd1ZnUExrUSJ9.FRG72ttH0iPM0tXDx_G0nqjjwAXhKPXjmes20yVmfqP0pyhRkX5j_3hDcIUWZzV0sQ728voygxkTrN2evHh-FYfvrHIvkJ7W2QMBApogIwvjv6AeNLtuqK1NEEpi1vKlqAd6Er8Qn1cofOmlcWwL1qj71HBlmklPkwjNzL_oAWWDzOYYTwF5j4grgKHQKAGP5UjZvVPiCMkblkFjzp2FmFJUXIb_2I6BpRAbR166fner_C0tt_yqy0xRqYd8S6D4zqMHZ5qngyMNKl8VmXKo2O65dCDofxDYb2YqkUMwE_A88_Tlxhf-mBP3AJps305DseTj-L9N9c9M_WSQi_dY0g"
private const val INITIAL_FLOW_ID = "home"
private const val NAVBAR_FLOW_ID = "navbar"
private val TEMP_BLOCKED_TARGET_IDS = setOf("consumos-screen-1")
private val NAVBAR_HEIGHT = 88.dp
private const val MSISDN_PREFIX = "52"
private val TEST_MSISDN_OPTIONS = listOf(
    "5510108894",
    "5516748497",
    "5529478626",
    "5530188033",
    "5540544448",
    "5554007347",
    "5564764190",
    "5580651242",
)

private enum class AppScreen {
    Login,
    Home,
    Config,
}

private enum class InitializationContext {
    Login,
    Config,
}

private enum class RenderSource {
    Content,
    Navbar,
}

private sealed interface ContentTarget {
    data class Component(val componentId: String) : ContentTarget

    data class Flow(val flowId: String) : ContentTarget
}

private sealed interface SdkHostAction {
    data class Navigate(val target: ContentTarget) : SdkHostAction
    data class BlockedNavigation(val targetId: String) : SdkHostAction
    data class OpenExternal(val url: String) : SdkHostAction
    data object Back : SdkHostAction
    data object None : SdkHostAction
}

@Composable
@Preview
fun App(
    onDebugSnapshot: (String) -> Unit = {},
) {
    MaterialTheme {
        WaveSdkSampleApp(onDebugSnapshot = onDebugSnapshot)
    }
}

@Composable
private fun WaveSdkSampleApp(
    onDebugSnapshot: (String) -> Unit,
) {
    val msisdnOptions = remember { TEST_MSISDN_OPTIONS }
    var currentScreen by remember { mutableStateOf(AppScreen.Login) }
    var userName by remember { mutableStateOf("") }
    var draftName by remember { mutableStateOf("") }
    var draftPassword by rememberSaveable { mutableStateOf("") }
    var selectedMsisdn by remember { mutableStateOf(msisdnOptions.first()) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var pendingInitializationContext by remember { mutableStateOf<InitializationContext?>(null) }
    var startupError by remember { mutableStateOf<String?>(null) }
    var readyMsisdn by remember { mutableStateOf<String?>(null) }
    var loginTappedAt by remember(selectedMsisdn) { mutableStateOf<TimeMark?>(null) }
    var homeShownAt by remember(selectedMsisdn) { mutableStateOf<TimeMark?>(null) }
    var navbarReadyAt by remember(selectedMsisdn) { mutableStateOf<TimeMark?>(null) }
    var homeFrameCount by remember(selectedMsisdn) { mutableStateOf(0) }
    var navbarFirstLayoutFrame by remember(selectedMsisdn) { mutableStateOf<Int?>(null) }

    val msisdnWithPrefix = "$MSISDN_PREFIX$selectedMsisdn"
    var isNavbarReadyToUnblockContent by remember(msisdnWithPrefix) { mutableStateOf(true) }
    val contentTargetStack = remember(selectedMsisdn) { mutableStateListOf<ContentTarget>() }
    val currentContentTarget = contentTargetStack.lastOrNull()
    val currentEntryId =
        when (currentContentTarget) {
            is ContentTarget.Component -> currentContentTarget.componentId
            is ContentTarget.Flow -> currentContentTarget.flowId
            null -> INITIAL_FLOW_ID
        }
    val showTopBar = currentScreen != AppScreen.Login
    val isLoginInitializing = pendingInitializationContext == InitializationContext.Login
    val isConfigInitializing = pendingInitializationContext == InitializationContext.Config

    LaunchedEffect(currentScreen, currentEntryId) {
        onDebugSnapshot("${currentScreen.name.lowercase()}|$currentEntryId")
        if (currentScreen == AppScreen.Home && homeShownAt == null) {
            homeShownAt = TimeSource.Monotonic.markNow()
            val homeSnapshot =
                buildLatencyLog(
                    label = "home_first_frame",
                    startMark = loginTappedAt,
                    extra = "currentEntryId=$currentEntryId",
                )
            logSdk(SDK_TAG, homeSnapshot)
            onDebugSnapshot(homeSnapshot)
        }
    }

    LaunchedEffect(currentScreen, selectedMsisdn) {
        if (currentScreen != AppScreen.Home) {
            homeFrameCount = 0
            return@LaunchedEffect
        }

        homeFrameCount = 0
        while (currentScreen == AppScreen.Home && navbarReadyAt == null) {
            withFrameNanos {
                homeFrameCount += 1
            }
        }
    }

    val startSdkForMsisdn: (String, InitializationContext) -> Unit = start@{ targetMsisdn, context ->
        if (pendingInitializationContext != null) {
            return@start
        }

        if (SAMPLE_API_KEY.isBlank()) {
            startupError = "Missing Flow Wrapper API key"
            logSdk(SDK_TAG, "SDK initialization failed: $startupError")
            return@start
        }

        val targetMsisdnWithPrefix = "$MSISDN_PREFIX$targetMsisdn"
        if (readyMsisdn == targetMsisdnWithPrefix) {
            if (context == InitializationContext.Login) {
                currentScreen = AppScreen.Home
            }
            return@start
        }

        pendingInitializationContext = context
        startupError = null
        contentTargetStack.clear()

        runCatching {
            FlowWrapper.start(
                apiKey = SAMPLE_API_KEY,
                msisdn = targetMsisdnWithPrefix,
                onReady = {
                    readyMsisdn = targetMsisdnWithPrefix
                    pendingInitializationContext = null
                    if (context == InitializationContext.Login) {
                        currentScreen = AppScreen.Home
                    }
                    logSdk(
                        SDK_TAG,
                        "SDK ready with FlowWrapper.start(apiKey, msisdn=$targetMsisdnWithPrefix)",
                    )
                },
            )
        }.onSuccess {
            logSdk(
                SDK_TAG,
                "SDK initialization started with FlowWrapper.start(apiKey, msisdn=$targetMsisdnWithPrefix)",
            )
        }.onFailure { throwable ->
            pendingInitializationContext = null
            startupError = throwable.message ?: throwable::class.simpleName ?: "Unknown startup error"
            logSdk(SDK_TAG, "SDK initialization failed: $startupError")
        }
    }

    NativeBackHandler(enabled = currentScreen == AppScreen.Config || currentContentTarget != null) {
        when {
            currentScreen == AppScreen.Config && !isConfigInitializing -> {
                isDropdownExpanded = false
                currentScreen = AppScreen.Home
            }

            currentContentTarget != null -> {
                popComponentFromHostStack(
                    contentTargetStack = contentTargetStack,
                    reason = "native back button",
                )
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize(),
    ) {
        if (!showTopBar) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
            ) {
                LoginScreen(
                    draftName = draftName,
                    onDraftNameChange = { draftName = it },
                    draftPassword = draftPassword,
                    onDraftPasswordChange = { draftPassword = it },
                    onLogin = {
                        userName = draftName.ifBlank { "Usuario" }.trim()
                        loginTappedAt = TimeSource.Monotonic.markNow()
                        homeShownAt = null
                        navbarReadyAt = null
                        navbarFirstLayoutFrame = null
                        homeFrameCount = 0
                        val loginSnapshot =
                            buildLatencyLog(
                                label = "login_tapped",
                                startMark = loginTappedAt,
                            )
                        logSdk(SDK_TAG, loginSnapshot)
                        onDebugSnapshot(loginSnapshot)
                        startupError = null
                        startSdkForMsisdn(selectedMsisdn, InitializationContext.Login)
                    },
                    isLoading = isLoginInitializing,
                    errorMessage = startupError,
                )
            }
        } else {
            Scaffold(
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    AppTopBar(
                        userName = userName,
                        selectedMsisdn = msisdnWithPrefix,
                        currentScreen = currentScreen,
                        onOpenConfig = {
                            isDropdownExpanded = false
                            currentScreen = AppScreen.Config
                        },
                        onCloseConfig = {
                            if (!isConfigInitializing) {
                                isDropdownExpanded = false
                                currentScreen = AppScreen.Home
                            }
                        },
                        configNavigationEnabled = !isConfigInitializing,
                    )
                },
            ) { innerPadding ->
                when (currentScreen) {
                    AppScreen.Login -> Unit
                    AppScreen.Config -> {
                        ConfigScreen(
                            contentPadding = innerPadding,
                            options = msisdnOptions,
                            selectedMsisdn = selectedMsisdn,
                            expanded = isDropdownExpanded,
                            onSelectionChange = { updatedMsisdn ->
                                isDropdownExpanded = false
                                if (updatedMsisdn != selectedMsisdn) {
                                    selectedMsisdn = updatedMsisdn
                                    startSdkForMsisdn(updatedMsisdn, InitializationContext.Config)
                                }
                            },
                            onExpandedChange = { isDropdownExpanded = it },
                            onLogout = {
                                isDropdownExpanded = false
                                userName = ""
                                draftName = ""
                                draftPassword = ""
                                pendingInitializationContext = null
                                readyMsisdn = null
                                startupError = null
                                isNavbarReadyToUnblockContent = false
                                contentTargetStack.clear()
                                currentScreen = AppScreen.Login
                            },
                            isLoading = isConfigInitializing,
                            errorMessage = startupError,
                        )
                    }

                    AppScreen.Home -> Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                    ) {
                        val density = LocalDensity.current
                        val navigationBarHeight =
                            with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
                        val navbarHostHeight = NAVBAR_HEIGHT + navigationBarHeight

                        when {
                            startupError != null -> Text(
                                text = "Initialization error: $startupError",
                                modifier = Modifier.align(Alignment.Center),
                            )

                            else -> key(msisdnWithPrefix) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    key(NAVBAR_FLOW_ID) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .align(Alignment.BottomStart)
                                                    .fillMaxWidth()
                                                    .height(navbarHostHeight)
                                                    .onGloballyPositioned {
                                                        if (navbarFirstLayoutFrame != null) return@onGloballyPositioned

                                                        navbarFirstLayoutFrame = homeFrameCount
                                                        val navbarLayoutSnapshot =
                                                            buildFrameLog(
                                                                label = "navbar_first_layout",
                                                                frameCount = homeFrameCount,
                                                                extra =
                                                                    "homeFirstFrameMs=${homeShownAt?.elapsedNow()?.inWholeMilliseconds}",
                                                            )
                                                        logSdk(SDK_TAG, navbarLayoutSnapshot)
                                                        onDebugSnapshot(navbarLayoutSnapshot)
                                                    },
                                            contentAlignment = Alignment.TopStart,
                                        ) {
                                            RenderBlock(
                                                flowId = NAVBAR_FLOW_ID,
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height(NAVBAR_HEIGHT),
                                                onEvent = { event ->
                                                    handleSdkEvent(
                                                        event = event,
                                                        currentTarget = currentContentTarget,
                                                        contentTargetStack = contentTargetStack,
                                                        source = RenderSource.Navbar,
                                                    )
                                                    if (event is SDKEvent.ComponentLoaded || event is SDKEvent.Error) {
                                                        if (navbarReadyAt == null) {
                                                            navbarReadyAt = TimeSource.Monotonic.markNow()
                                                            val navbarReadyFrame = homeFrameCount
                                                            val navbarSnapshot =
                                                                buildLatencyLog(
                                                                    label = "navbar_ready",
                                                                    startMark = loginTappedAt,
                                                                    extra =
                                                                        "event=${event::class.simpleName} homeFirstFrameMs=${homeShownAt?.elapsedNow()?.inWholeMilliseconds} " +
                                                                            "homeFrame=$navbarReadyFrame firstLayoutFrame=$navbarFirstLayoutFrame",
                                                                )
                                                            val navbarFrameSnapshot =
                                                                buildFrameLog(
                                                                    label = "navbar_ready",
                                                                    frameCount = navbarReadyFrame,
                                                                    extra =
                                                                        "event=${event::class.simpleName} firstLayoutFrame=$navbarFirstLayoutFrame",
                                                                )
                                                            logSdk(SDK_TAG, navbarSnapshot)
                                                            onDebugSnapshot(navbarSnapshot)
                                                            logSdk(SDK_TAG, navbarFrameSnapshot)
                                                            onDebugSnapshot(navbarFrameSnapshot)
                                                        }
                                                        isNavbarReadyToUnblockContent = true
                                                    }
                                                },
                                            )
                                        }
                                    }

                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .padding(bottom = navbarHostHeight),
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            key(currentEntryId) {
                                                when (currentContentTarget) {
                                                    null -> {
                                                        RenderBlock(
                                                            flowId = INITIAL_FLOW_ID,
                                                            modifier = Modifier.fillMaxSize(),
                                                            onEvent = { event ->
                                                                handleSdkEvent(
                                                                    event = event,
                                                                    currentTarget = null,
                                                                    contentTargetStack = contentTargetStack,
                                                                    source = RenderSource.Content,
                                                                )
                                                            },
                                                        )
                                                    }

                                                    is ContentTarget.Flow -> {
                                                        RenderBlock(
                                                            flowId = currentContentTarget.flowId,
                                                            modifier = Modifier.fillMaxSize(),
                                                            onEvent = { event ->
                                                                handleSdkEvent(
                                                                    event = event,
                                                                    currentTarget = currentContentTarget,
                                                                    contentTargetStack = contentTargetStack,
                                                                    source = RenderSource.Content,
                                                                )
                                                            },
                                                        )
                                                    }

                                                    is ContentTarget.Component -> {
                                                        RenderBlock(
                                                            componentId = currentContentTarget.componentId,
                                                            modifier = Modifier.fillMaxSize(),
                                                            onEvent = { event ->
                                                                handleSdkEvent(
                                                                    event = event,
                                                                    currentTarget = currentContentTarget,
                                                                    contentTargetStack = contentTargetStack,
                                                                    source = RenderSource.Content,
                                                                )
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppTopBar(
    userName: String,
    selectedMsisdn: String,
    currentScreen: AppScreen,
    onOpenConfig: () -> Unit,
    onCloseConfig: () -> Unit,
    configNavigationEnabled: Boolean,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "+$selectedMsisdn",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        navigationIcon = {
            if (currentScreen == AppScreen.Config) {
                TextButton(
                    onClick = onCloseConfig,
                    enabled = configNavigationEnabled,
                ) {
                    Text("Voltar")
                }
            }
        },
        actions = {
            if (currentScreen != AppScreen.Config) {
                TextButton(onClick = onOpenConfig) {
                    Text("\u2699")
                }
            }
        },
    )
}

@Composable
private fun LoginScreen(
    draftName: String,
    onDraftNameChange: (String) -> Unit,
    draftPassword: String,
    onDraftPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
        ) {
            Text(
                text = "Login",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Entre com seu nome para acessar a home.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = draftName,
                onValueChange = onDraftNameChange,
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = draftPassword,
                onValueChange = onDraftPasswordChange,
                label = { Text("Senha") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                visualTransformation =
                    if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                trailingIcon = {
                    PasswordVisibilityToggle(
                        passwordVisible = passwordVisible,
                        onToggle = { passwordVisible = !passwordVisible },
                    )
                },
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("Entrar")
                }
            }
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Initialization error: $errorMessage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ConfigScreen(
    contentPadding: PaddingValues,
    options: List<String>,
    selectedMsisdn: String,
    expanded: Boolean,
    onSelectionChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "Configuracoes",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Escolha o MSISDN de teste usado para inicializar o SDK.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(20.dp))
        MsisdnSelectionBox(
            options = options,
            selectedMsisdn = selectedMsisdn,
            expanded = expanded,
            onSelectionChange = onSelectionChange,
            onExpandedChange = onExpandedChange,
            enabled = !isLoading,
        )
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Atualizando SDK para o novo MSISDN...",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Initialization error: $errorMessage",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "SDK version",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = FLOW_WRAPPER_VERSION,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "SDK snapshot build",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = FlowsSdkBuildInfo.DESCRIPTION,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
        ) {
            Text("Sair")
        }
    }
}

private fun logRenderEvent(flowId: String): (SDKEvent) -> Unit = { event ->
    logSdk(SDK_TAG, "$flowId event=${event::class.simpleName}")
}

private fun buildLatencyLog(
    label: String,
    startMark: TimeMark?,
    extra: String? = null,
): String {
    val elapsedMs = startMark?.elapsedNow()?.inWholeMilliseconds
    return buildString {
        append("timing ")
        append(label)
        append('=')
        append(elapsedMs?.toString() ?: "unknown")
        append("ms")
        if (!extra.isNullOrBlank()) {
            append(' ')
            append(extra)
        }
    }
}

private fun buildFrameLog(
    label: String,
    frameCount: Int?,
    extra: String? = null,
): String {
    return buildString {
        append("frames ")
        append(label)
        append('=')
        append(frameCount?.toString() ?: "unknown")
        if (!extra.isNullOrBlank()) {
            append(' ')
            append(extra)
        }
    }
}

@Composable
private fun MsisdnSelectionBox(
    options: List<String>,
    selectedMsisdn: String,
    expanded: Boolean,
    onSelectionChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    require(options.isNotEmpty())

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { onExpandedChange(!expanded) },
        ) {
            OutlinedTextField(
                value = "+$MSISDN_PREFIX$selectedMsisdn",
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("MSISDN de teste") },
                trailingIcon = {
                    Text("▾")
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (expanded) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                        ),
            ) {
                options.forEachIndexed { index, option ->
                    Text(
                        text = "+$MSISDN_PREFIX$option",
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectionChange(option)
                                    onExpandedChange(false)
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    if (index < options.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun handleSdkEvent(
    event: SDKEvent,
    currentTarget: ContentTarget?,
    contentTargetStack: MutableList<ContentTarget>,
    source: RenderSource,
) {
    when (event) {
        is SDKEvent.ComponentLoaded -> {
            logSdk(SDK_TAG, "SDKEvent.ComponentLoaded component=${event.componentId}")
        }

        is SDKEvent.Error -> {
            logSdk(
                SDK_TAG,
                "SDKEvent.Error component=${event.componentId} code=${event.code} message=${event.message}",
            )
        }

        is SDKEvent.Callback -> {
            logSdk(SDK_TAG, "SDKEvent.Callback type=${event.type} payload=${event.payload}")
            when (val action = resolveSdkHostAction(event, currentTarget)) {
                is SdkHostAction.Navigate -> {
                    if (source == RenderSource.Navbar) {
                        contentTargetStack.clear()
                        if (action.target is ContentTarget.Flow && action.target.flowId == INITIAL_FLOW_ID) {
                            logSdk(SDK_TAG, "Host navbar navigate reset to initial flowId=$INITIAL_FLOW_ID")
                        } else {
                            contentTargetStack += action.target
                            logSdk(SDK_TAG, "Host navbar navigate replace target=${action.target.asLogLabel()}")
                        }
                    } else {
                        contentTargetStack += action.target
                        logSdk(
                            SDK_TAG,
                            "Host navigate push target=${action.target.asLogLabel()} stackSize=${contentTargetStack.size}",
                        )
                    }
                }

                SdkHostAction.Back -> {
                    popComponentFromHostStack(
                        contentTargetStack = contentTargetStack,
                        reason = "SDK callback",
                    )
                }

                is SdkHostAction.OpenExternal -> {
                    logSdk(SDK_TAG, "Host external navigation requested url=${action.url}")
                }

                is SdkHostAction.BlockedNavigation -> {
                    logSdk(
                        SDK_TAG,
                        "Host blocked navigation target=${action.targetId} (temporary sample safeguard)",
                    )
                }

                SdkHostAction.None -> Unit
            }
        }
    }
}

private fun resolveSdkHostAction(
    event: SDKEvent.Callback,
    currentTarget: ContentTarget?,
): SdkHostAction {
    val normalizedType = event.type.lowercase()
    val callbackPayload = extractJsonObjectField(event.payload, "payload") ?: event.payload
    val payloadActionType = extractJsonStringField(callbackPayload, "type")?.lowercase()
    val nextComponentId =
        extractJsonStringField(callbackPayload, "nextComponentId")
            ?: extractJsonStringField(callbackPayload, "targetComponentId")
            ?: extractJsonStringField(callbackPayload, "destinationComponentId")
    val externalUrl =
        extractJsonStringField(callbackPayload, "url")
            ?: extractJsonStringField(callbackPayload, "externalUrl")

    if (normalizedType == "render_block_navigate") {
        return when {
            !nextComponentId.isNullOrBlank() -> {
                if (nextComponentId in TEMP_BLOCKED_TARGET_IDS) {
                    return SdkHostAction.BlockedNavigation(nextComponentId)
                }
                val nextTarget = resolveContentTarget(nextComponentId)
                if (nextTarget != null && nextTarget.id() != currentTarget.idOrNull()) {
                    SdkHostAction.Navigate(nextTarget)
                } else {
                    SdkHostAction.None
                }
            }

            payloadActionType in setOf("back", "close") -> SdkHostAction.Back

            payloadActionType == "external" && !externalUrl.isNullOrBlank() -> {
                SdkHostAction.OpenExternal(externalUrl)
            }

            else -> SdkHostAction.None
        }
    }

    if ("back" in normalizedType || "close" in normalizedType) {
        return SdkHostAction.Back
    }

    return SdkHostAction.None
}

private fun popComponentFromHostStack(
    contentTargetStack: MutableList<ContentTarget>,
    reason: String,
) {
    val popped = contentTargetStack.removeLastOrNull()
    logSdk(
        SDK_TAG,
        "Host back requested by $reason, popped target=${popped?.asLogLabel()} stackSize=${contentTargetStack.size}",
    )
}

private fun resolveContentTarget(
    nextComponentId: String?,
): ContentTarget? {
    val normalizedComponentId = nextComponentId?.trim().orEmpty()
    if (normalizedComponentId.isNotEmpty()) {
        return when (normalizedComponentId) {
            "home-screen-1" -> ContentTarget.Flow(INITIAL_FLOW_ID)
            else -> ContentTarget.Component(normalizedComponentId)
        }
    }
    return null
}

private fun ContentTarget.id(): String = when (this) {
    is ContentTarget.Component -> componentId
    is ContentTarget.Flow -> flowId
}

private fun ContentTarget?.idOrNull(): String? = this?.id()

private fun ContentTarget.asLogLabel(): String = when (this) {
    is ContentTarget.Component -> "component:$componentId"
    is ContentTarget.Flow -> "flow:$flowId"
}

private fun extractJsonObjectField(json: String, fieldName: String): String? {
    val valueStart = findJsonFieldValueStart(json, fieldName) ?: return null
    if (valueStart >= json.length || json[valueStart] != '{') return null

    var depth = 0
    var index = valueStart
    var inString = false
    var escaped = false

    while (index < json.length) {
        val char = json[index]
        when {
            escaped -> escaped = false
            char == '\\' -> escaped = true
            char == '"' -> inString = !inString
            !inString && char == '{' -> depth++
            !inString && char == '}' -> {
                depth--
                if (depth == 0) {
                    return json.substring(valueStart, index + 1)
                }
            }
        }
        index++
    }

    return null
}

private fun extractJsonStringField(json: String, fieldName: String): String? {
    val valueStart = findJsonFieldValueStart(json, fieldName) ?: return null
    if (valueStart >= json.length || json[valueStart] != '"') return null

    val result = StringBuilder()
    var index = valueStart + 1
    var escaped = false

    while (index < json.length) {
        val char = json[index++]
        when {
            escaped -> {
                result.append(char)
                escaped = false
            }

            char == '\\' -> escaped = true
            char == '"' -> return result.toString()
            else -> result.append(char)
        }
    }

    return null
}

private fun findJsonFieldValueStart(json: String, fieldName: String): Int? {
    var index = 0

    while (index < json.length) {
        if (json[index] != '"') {
            index++
            continue
        }

        val stringEnd = findJsonStringEnd(json, index) ?: return null
        val candidateFieldName = decodeJsonString(json.substring(index + 1, stringEnd))
        index = stringEnd + 1

        if (candidateFieldName != fieldName) {
            continue
        }

        while (index < json.length && json[index].isWhitespace()) {
            index++
        }
        if (index >= json.length || json[index] != ':') {
            continue
        }

        index++
        while (index < json.length && json[index].isWhitespace()) {
            index++
        }
        return index
    }

    return null
}

private fun findJsonStringEnd(json: String, startQuoteIndex: Int): Int? {
    var index = startQuoteIndex + 1
    var escaped = false

    while (index < json.length) {
        val char = json[index]
        when {
            escaped -> escaped = false
            char == '\\' -> escaped = true
            char == '"' -> return index
        }
        index++
    }

    return null
}

private fun decodeJsonString(value: String): String {
    val result = StringBuilder(value.length)
    var index = 0
    var escaped = false

    while (index < value.length) {
        val char = value[index++]
        when {
            escaped -> {
                result.append(char)
                escaped = false
            }

            char == '\\' -> escaped = true
            else -> result.append(char)
        }
    }

    return result.toString()
}
