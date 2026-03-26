package br.com.wave.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import br.com.wave.flow_wrapper_kmp.FlowWrapper
import br.com.wave.flow_wrapper_kmp.RenderBlock
import br.com.wave.flow_wrapper_kmp.SDKEvent

private const val SDK_TAG = "WaveSdkSample"
private const val SAMPLE_API_KEY = "YOUR_API_KEY"
private const val INITIAL_FLOW_ID = "home"
private const val NAVBAR_FLOW_ID = "navbar"
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

private sealed interface SdkHostAction {
    data class Navigate(val componentId: String) : SdkHostAction
    data class OpenExternal(val url: String) : SdkHostAction
    data object Back : SdkHostAction
    data object None : SdkHostAction
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        WaveSdkSampleApp()
    }
}

@Composable
private fun WaveSdkSampleApp() {
    val msisdnOptions = remember { TEST_MSISDN_OPTIONS }
    var selectedMsisdn by remember { mutableStateOf(msisdnOptions.first()) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var sdkStarted by remember { mutableStateOf(false) }
    var startupError by remember { mutableStateOf<String?>(null) }

    val msisdnWithPrefix = "$MSISDN_PREFIX$selectedMsisdn"
    val componentStack = remember(selectedMsisdn) { mutableStateListOf<String>() }
    val currentComponentId = componentStack.lastOrNull()
    val currentEntryId = currentComponentId ?: INITIAL_FLOW_ID
    val selectionSpacing = 12.dp
    val density = LocalDensity.current
    var selectionHeight by remember { mutableStateOf(0) }
    val renderContentPadding = maxOf(with(density) { selectionHeight.toDp() }, selectionSpacing)

    NativeBackHandler(enabled = currentComponentId != null) {
        popComponentFromHostStack(
            componentStack = componentStack,
            reason = "native back button",
        )
    }

    LaunchedEffect(msisdnWithPrefix) {
        sdkStarted = false
        startupError = null
        runCatching {
            FlowWrapper.start(
                apiKey = SAMPLE_API_KEY,
                msisdn = msisdnWithPrefix,
            )
        }.onSuccess {
            sdkStarted = true
            logSdk(
                SDK_TAG,
                "SDK initialized with FlowWrapper.start(apiKey, msisdn=$msisdnWithPrefix)",
            )
        }.onFailure { throwable ->
            startupError = throwable.message ?: throwable::class.simpleName ?: "Unknown startup error"
            logSdk(SDK_TAG, "SDK initialization failed: $startupError")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .onGloballyPositioned { coordinates ->
                        selectionHeight = coordinates.size.height
                    },
        ) {
            MsisdnSelectionBox(
                options = msisdnOptions,
                selectedMsisdn = selectedMsisdn,
                expanded = isDropdownExpanded,
                onSelectionChange = { selectedMsisdn = it },
                onExpandedChange = { isDropdownExpanded = it },
            )
            Spacer(modifier = Modifier.height(selectionSpacing))
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = renderContentPadding),
        ) {
            val reproduceRenderError = true
            if (reproduceRenderError) {
                Column(modifier = Modifier.fillMaxSize()) {
                    RenderBlock(
                        modifier = Modifier.weight(1f),
                        componentId = null,
                        flowId = INITIAL_FLOW_ID,
                        onEvent = logRenderEvent("home"),
                    )
                    RenderBlock(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(NAVBAR_HEIGHT),
                        componentId = null,
                        flowId = NAVBAR_FLOW_ID,
                        onEvent = logRenderEvent("navbar"),
                    )
                }
            } else {
                when {
                    startupError != null -> Text(
                        text = "Initialization error: $startupError",
                        modifier = Modifier.align(Alignment.Center),
                    )

                    !sdkStarted -> Box(modifier = Modifier.fillMaxSize())
                    else -> key(msisdnWithPrefix) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(bottom = NAVBAR_HEIGHT),
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    key(currentEntryId) {
                                        if (currentComponentId == null) {
                                            RenderBlock(
                                                flowId = INITIAL_FLOW_ID,
                                                modifier = Modifier.fillMaxSize(),
                                                onEvent = { event ->
                                                    handleSdkEvent(
                                                        event = event,
                                                        currentComponentId = currentEntryId,
                                                        componentStack = componentStack,
                                                    )
                                                },
                                            )
                                        } else {
                                            RenderBlock(
                                                componentId = currentComponentId,
                                                modifier = Modifier.fillMaxSize(),
                                                onEvent = { event ->
                                                    handleSdkEvent(
                                                        event = event,
                                                        currentComponentId = currentComponentId,
                                                        componentStack = componentStack,
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }
                            }

                            key(NAVBAR_FLOW_ID) {
                                RenderBlock(
                                    flowId = NAVBAR_FLOW_ID,
                                    modifier =
                                        Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .height(NAVBAR_HEIGHT),
                                    onEvent = { event ->
                                        handleSdkEvent(
                                            event = event,
                                            currentComponentId = currentEntryId,
                                            componentStack = componentStack,
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

private fun logRenderEvent(flowId: String): (SDKEvent) -> Unit = { event ->
    logSdk(SDK_TAG, "$flowId event=${event::class.simpleName}")
}

@Composable
private fun MsisdnSelectionBox(
    options: List<String>,
    selectedMsisdn: String,
    expanded: Boolean,
    onSelectionChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
) {
    require(options.isNotEmpty())

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onExpandedChange(!expanded) },
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
                            Divider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "MSISDN atual: +$MSISDN_PREFIX$selectedMsisdn",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "SDK v$FLOW_WRAPPER_VERSION",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
}

private fun handleSdkEvent(
    event: SDKEvent,
    currentComponentId: String,
    componentStack: MutableList<String>,
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
            when (val action = resolveSdkHostAction(event, currentComponentId)) {
                is SdkHostAction.Navigate -> {
                    componentStack += action.componentId
                    logSdk(
                        SDK_TAG,
                        "Host navigate push componentId=${action.componentId} stackSize=${componentStack.size}",
                    )
                }

                SdkHostAction.Back -> {
                    popComponentFromHostStack(
                        componentStack = componentStack,
                        reason = "SDK callback",
                    )
                }

                is SdkHostAction.OpenExternal -> {
                    logSdk(SDK_TAG, "Host external navigation requested url=${action.url}")
                }

                SdkHostAction.None -> Unit
            }
        }
    }
}

private fun resolveSdkHostAction(
    event: SDKEvent.Callback,
    currentComponentId: String,
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
        return when (payloadActionType) {
            "navigation" -> {
                if (!nextComponentId.isNullOrBlank() && nextComponentId != currentComponentId) {
                    SdkHostAction.Navigate(nextComponentId)
                } else {
                    SdkHostAction.None
                }
            }

            "back", "close" -> SdkHostAction.Back
            "external" -> {
                if (!externalUrl.isNullOrBlank()) {
                    SdkHostAction.OpenExternal(externalUrl)
                } else {
                    SdkHostAction.None
                }
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
    componentStack: MutableList<String>,
    reason: String,
) {
    val popped = componentStack.removeLastOrNull()
    logSdk(
        SDK_TAG,
        "Host back requested by $reason, popped component=$popped stackSize=${componentStack.size}",
    )
}

private fun extractJsonObjectField(json: String, fieldName: String): String? {
    val quotedFieldName = "\"$fieldName\""
    val fieldStart = json.indexOf(quotedFieldName)
    if (fieldStart < 0) return null

    val colonIndex = json.indexOf(':', startIndex = fieldStart + quotedFieldName.length)
    if (colonIndex < 0) return null

    var valueStart = colonIndex + 1
    while (valueStart < json.length && json[valueStart].isWhitespace()) {
        valueStart++
    }
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
    val quotedFieldName = "\"$fieldName\""
    val fieldStart = json.indexOf(quotedFieldName)
    if (fieldStart < 0) return null

    val colonIndex = json.indexOf(':', startIndex = fieldStart + quotedFieldName.length)
    if (colonIndex < 0) return null

    var valueStart = colonIndex + 1
    while (valueStart < json.length && json[valueStart].isWhitespace()) {
        valueStart++
    }
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
