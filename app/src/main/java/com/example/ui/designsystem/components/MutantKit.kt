package com.example.ui.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantMotion
import com.example.ui.designsystem.MutantShapeTokens
import com.example.ui.designsystem.MutantType

/** Mono uppercase label used above values and sections. */
@Composable
fun MutantEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MutantColors.TextMetadata,
    style: TextStyle = MutantType.Eyebrow
) {
    Text(text, modifier = modifier, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

enum class MutantButtonStyle { Primary, Outline, Surface, Danger, Quiet }

/** One button shape for every call to action: 14dp corners, sentence-case Inter. */
@Composable
fun MutantButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MutantButtonStyle = MutantButtonStyle.Primary,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = 56.dp,
    textStyle: TextStyle = if (style == MutantButtonStyle.Primary || style == MutantButtonStyle.Danger) MutantType.Button
        else MutantType.Button.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(MutantMotion.Feedback), label = "MutantButtonScale")
    val (container, content, border) = when (style) {
        MutantButtonStyle.Primary -> Triple(MutantColors.Primary, MutantColors.OnPrimary, null)
        MutantButtonStyle.Danger -> Triple(MutantColors.Error, MutantColors.OnError, null)
        MutantButtonStyle.Outline -> Triple(Color.Transparent, MutantColors.TextPrimary, BorderStroke(1.dp, MutantColors.Outline))
        MutantButtonStyle.Surface -> Triple(MutantColors.SurfaceContainer, MutantColors.TextPrimary, BorderStroke(1.dp, MutantColors.Line))
        MutantButtonStyle.Quiet -> Triple(Color.Transparent, MutantColors.TextSecondary, null)
    }
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = interaction,
        shape = if (height < 48.dp) RoundedCornerShape(height / 2) else MutantShapeTokens.Button,
        border = border,
        contentPadding = PaddingValues(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container, contentColor = content,
            disabledContainerColor = if (container == Color.Transparent) container else container.copy(alpha = 0.4f),
            disabledContentColor = content.copy(alpha = 0.5f)
        ),
        modifier = modifier.height(height).scale(scale)
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = content, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = textStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailingIcon != null && !loading) {
            Spacer(Modifier.width(8.dp))
            Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(22.dp))
        }
    }
}

/** Selected: faint purple fill with purple border. Idle: outline only. */
@Composable
fun MutantChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 32.dp,
    cornerRadius: Dp = height / 2,
    textStyle: TextStyle = MutantType.Chip,
    horizontalPadding: Dp = 12.dp
) {
    Surface(
        modifier = modifier
            .height(height)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(cornerRadius),
        color = if (selected) MutantColors.PrimarySelected else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) MutantColors.Primary else MutantColors.Line)
    ) {
        Box(Modifier.padding(horizontal = horizontalPadding), contentAlignment = Alignment.Center) {
            Text(
                label, style = textStyle, maxLines = 1,
                color = when {
                    !enabled -> MutantColors.TextMetadata
                    selected -> MutantColors.TextPrimary
                    else -> MutantColors.TextSecondary
                }
            )
        }
    }
}

/** Eyebrow over a value, optionally with a caption. Used in stat rows. */
@Composable
fun MutantStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    valueColor: Color = MutantColors.TextPrimary,
    valueStyle: TextStyle = MutantType.Title.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp)
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        MutantEyebrow(label, style = MutantType.Eyebrow.copy(fontSize = 9.5.sp))
        Text(value, style = valueStyle, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (caption != null) Text(caption, style = MutantType.Caption, color = MutantColors.TextSecondary, maxLines = 1)
    }
}

/** Filled well with a stat inside, used in sheets. */
@Composable
fun MutantStatWell(label: String, value: String, modifier: Modifier = Modifier, valueStyle: TextStyle = MutantType.MonoValue.copy(fontSize = 16.sp, lineHeight = 18.sp)) {
    Column(
        modifier
            .background(MutantColors.Background, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MutantEyebrow(label, style = MutantType.Eyebrow.copy(fontSize = 9.5.sp))
        Text(value, style = valueStyle, color = MutantColors.TextPrimary)
    }
}

/**
 * Minus / value / plus. The value stays editable for fast keyboard entry;
 * steppers cover the common case.
 */
@Composable
fun MutantStepper(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "stepper",
    allowDecimal: Boolean = true,
    enabled: Boolean = true,
    valueDescription: String = label,
    buttonWidth: Dp = 48.dp,
    // Next moves to the following field, Done closes the keyboard.
    imeAction: ImeAction = ImeAction.Done
) {
    val focusManager = LocalFocusManager.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MutantEyebrow(label, modifier = Modifier.padding(start = 4.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(MutantColors.Background, RoundedCornerShape(16.dp))
                .border(1.dp, MutantColors.Line, RoundedCornerShape(16.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepperButton(Icons.Rounded.Remove, "Decrease $valueDescription", onMinus, enabled, "${tag}_minus_button", buttonWidth)
            BasicTextField(
                value = value,
                onValueChange = { input ->
                    val normalized = input.replace(',', '.')
                    if (normalized.length <= 7 && normalized.all { it.isDigit() || (allowDecimal && it == '.') } &&
                        normalized.count { it == '.' } <= 1
                    ) onValueChange(normalized)
                },
                enabled = enabled,
                singleLine = true,
                textStyle = MutantType.MonoValue.copy(color = MutantColors.TextPrimary, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(MutantColors.Primary),
                keyboardOptions = KeyboardOptions(keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number, imeAction = imeAction),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) },
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = valueDescription }
                    .testTag("${tag}_input")
            )
            StepperButton(Icons.Rounded.Add, "Increase $valueDescription", onPlus, enabled, "${tag}_plus_button", buttonWidth)
        }
    }
}

/** Tap steps once; holding repeats and accelerates so a 20 kg change is not twenty taps. */
@Composable
private fun StepperButton(icon: ImageVector, description: String, onStep: () -> Unit, enabled: Boolean, tag: String, width: Dp) {
    val currentOnClick by rememberUpdatedState(onStep)
    var repeated by remember { mutableStateOf(false) }
    Box(
        Modifier
            .width(width)
            .fillMaxHeight()
            .testTag(tag)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = description
                if (enabled) onClick(label = description) { currentOnClick(); true } else disabled()
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        repeated = false
                        coroutineScope {
                            val job = launch {
                                delay(HoldDelayMs)
                                repeated = true
                                var gap = 180L
                                while (true) {
                                    currentOnClick()
                                    delay(gap)
                                    gap = (gap * 0.85f).toLong().coerceAtLeast(45L)
                                }
                            }
                            tryAwaitRelease()
                            job.cancel()
                        }
                    },
                    onTap = { if (!repeated) currentOnClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp),
            tint = if (enabled) MutantColors.TextSecondary else MutantColors.TextMetadata)
    }
}

private const val HoldDelayMs = 400L

/** Labelled text input in the sheet style: dark well, line border, eyebrow label. */
@Composable
fun MutantTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minHeight: Dp = 52.dp,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    testTag: String? = null
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MutantEyebrow(label, modifier = Modifier.padding(start = 4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .background(MutantColors.Background, MutantShapeTokens.Panel)
                .border(1.dp, MutantColors.Line, MutantShapeTokens.Panel)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
        ) {
            BasicTextField(
                value = value, onValueChange = onValueChange, enabled = enabled, singleLine = singleLine,
                textStyle = MutantType.Body.copy(color = MutantColors.TextPrimary),
                cursorBrush = SolidColor(MutantColors.Primary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth().then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, style = MutantType.Body, color = MutantColors.TextMetadata)
                    inner()
                }
            )
        }
    }
}

/** The redesign's bottom sheet: dark surface, 30dp top corners, short handle, heavy scrim. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutantBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val canDismiss by rememberUpdatedState(dismissible)
    val state = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || canDismiss }
    )
    ModalBottomSheet(
        onDismissRequest = { if (canDismiss) onDismiss() },
        sheetState = state,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = MutantColors.SurfaceContainer,
        contentColor = MutantColors.TextPrimary,
        scrimColor = MutantColors.Scrim,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 16.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(MutantColors.Handle, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            content = content
        )
    }
}

/** Short confirmations; the light surface stands out against the dark UI. */
class MutantToastState {
    var message by mutableStateOf<String?>(null)
        private set
    var actionLabel by mutableStateOf<String?>(null)
        private set
    private var action: (() -> Unit)? = null
    private var serial by mutableIntStateOf(0)

    /** With an action (e.g. Undo) the toast stays a little longer. */
    fun show(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        message = text
        this.actionLabel = actionLabel?.takeIf { onAction != null }
        action = onAction
        serial++
    }

    fun performAction() {
        val run = action
        dismiss()
        run?.invoke()
    }

    fun dismiss() {
        message = null
        actionLabel = null
        action = null
    }

    @Composable
    internal fun AutoHide() {
        val current = serial
        LaunchedEffect(current) {
            if (message != null) {
                kotlinx.coroutines.delay(if (actionLabel != null) 4500 else 2400)
                if (serial == current) dismiss()
            }
        }
    }
}

val LocalMutantToast = staticCompositionLocalOf { MutantToastState() }

@Composable
fun MutantToastHost(state: MutantToastState, modifier: Modifier = Modifier) {
    state.AutoHide()
    androidx.compose.animation.AnimatedVisibility(
        visible = state.message != null,
        enter = androidx.compose.animation.fadeIn(tween(MutantMotion.State)) +
            androidx.compose.animation.slideInVertically(tween(MutantMotion.State)) { it / 2 },
        exit = androidx.compose.animation.fadeOut(tween(MutantMotion.Feedback)),
        modifier = modifier
    ) {
        var last by remember { mutableStateOf("") }
        state.message?.let { last = it }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .background(MutantColors.Toast, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag("mutant_toast"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Rounded.CheckCircle, contentDescription = null,
                tint = MutantColors.ToastAccent, modifier = Modifier.size(20.dp)
            )
            Text(last, style = MutantType.BodySmall.copy(fontWeight = FontWeight.SemiBold), color = MutantColors.OnToast,
                modifier = Modifier.weight(1f))
            state.actionLabel?.let { label ->
                TextButton(
                    onClick = state::performAction,
                    modifier = Modifier.height(32.dp).testTag("mutant_toast_action"),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MutantColors.ToastAccent)
                ) { Text(label, style = MutantType.ButtonSmall.copy(fontWeight = FontWeight.ExtraBold)) }
            }
        }
    }
}
