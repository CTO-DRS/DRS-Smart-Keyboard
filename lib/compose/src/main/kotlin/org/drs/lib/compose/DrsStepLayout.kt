/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drs.lib.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp

private val stepHeaderVerticalPad = 8.dp
private val stepBadgeDiameter = 40.dp
private val stepBadgeGap = 16.dp
private val stepTitleBoxHeight = 32.dp
private val stepTitleInnerPad = 16.dp

/** One wizard step: a stable [id], a [title], and composable [content]. */
data class DrsStep(
    val id: Int, val title: String,
    val content: @androidx.compose.runtime.Composable DrsStepLayoutScope.() -> Unit,
)

/** Scope handed to step headers/contents, tinting buttons with the accent. */
class DrsStepLayoutScope(
    columnScope: androidx.compose.foundation.layout.ColumnScope,
    private val primaryColor: androidx.compose.ui.graphics.Color,
) : androidx.compose.foundation.layout.ColumnScope by columnScope {

    /** Justified body text for instruction copy. */
    @androidx.compose.runtime.Composable
    fun StepText(text: String,
        modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
        fontStyle: androidx.compose.ui.text.font.FontStyle =
            androidx.compose.ui.text.font.FontStyle.Normal,
    ) {
        androidx.compose.material3.Text(
            text = text, textAlign = androidx.compose.ui.text.style.TextAlign.Justify,
            fontStyle = fontStyle, modifier = modifier,
        )
    }

    /** Accent-filled primary action of the current step. */
    @androidx.compose.runtime.Composable
    fun StepButton(label: String,
        modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
        onClick: () -> Unit,
    ) {
        androidx.compose.material3.Button(
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = primaryColor),
            modifier = modifier
                .align(androidx.compose.ui.Alignment.CenterHorizontally).padding(top = 16.dp),
            onClick = onClick,
        ) {
            androidx.compose.material3.Text(text = label)
        }
    }
}

/**
 * Tracks the automatic step progression plus an optional manual override;
 * the manual value wins while it is at or after the automatic position.
 */
@Suppress("unused") class DrsStepState private constructor(
    private val currentAuto: androidx.compose.runtime.MutableState<Int>,
    private val currentManual: androidx.compose.runtime.MutableState<Int> =
        androidx.compose.runtime.mutableIntStateOf(-1),
) {
    companion object {
        fun new(init: Int): DrsStepState =
            DrsStepState(androidx.compose.runtime.mutableIntStateOf(init))

        val Saver: androidx.compose.runtime.saveable.Saver<DrsStepState, ArrayList<Int>> =
            androidx.compose.runtime.saveable.Saver(
                save = { state ->
                    arrayListOf(state.currentAuto.value, state.currentManual.value)
                },
                restore = { saved ->
                    DrsStepState(
                        androidx.compose.runtime.mutableIntStateOf(saved[0]),
                        androidx.compose.runtime.mutableIntStateOf(saved[1]),
                    )
                },
            )
    }

    /** The effective step: manual override if active, automatic otherwise. */
    fun getCurrent(): androidx.compose.runtime.State<Int> {
        return when {
            currentManual.value >= 0 && currentAuto.value >= currentManual.value -> currentManual
            else -> currentAuto
        }
    }

    fun getCurrentAuto(): androidx.compose.runtime.State<Int> = currentAuto

    fun getCurrentManual(): androidx.compose.runtime.State<Int> = currentManual

    /** Advances (or rewinds) the automatic progression to [value]. */
    fun setCurrentAuto(value: Int) { currentAuto.value = value }

    /** Pins a manual step, or clears the pin when it equals the automatic one. */
    fun setCurrentManual(value: Int) {
        currentManual.value = when {
            currentAuto.value == value -> -1
            else -> value
        }
    }
}

/**
 * Vertical wizard: renders [header], then every step from [steps] with its
 * numbered pill header, expanding only the active step between them, then
 * [footer]. Steps are keyed by [DrsStep.id] to preserve state.
 */
@androidx.compose.runtime.Composable
fun DrsStepLayout(
    stepState: DrsStepState,
    steps: List<DrsStep>,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    primaryColor: androidx.compose.ui.graphics.Color =
        androidx.compose.material3.MaterialTheme.colorScheme.primary,
    header: @androidx.compose.runtime.Composable DrsStepLayoutScope.() -> Unit = { },
    footer: @androidx.compose.runtime.Composable DrsStepLayoutScope.() -> Unit = { },
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxSize(),
    ) {
        val scope = DrsStepLayoutScope(this, primaryColor)
        header.invoke(scope)
        steps.forEachIndexed { index, step ->
            androidx.compose.runtime.key(step.id) {
                Step(
                    ownStepId = step.id, index = index + 1,
                    stepState = stepState, title = step.title, primaryColor = primaryColor,
                ) {
                    step.content(DrsStepLayoutScope(this, primaryColor))
                }
            }
        }
        footer.invoke(scope)
    }
}

@kotlin.OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@androidx.compose.runtime.Composable
private fun androidx.compose.foundation.layout.ColumnScope.Step(
    ownStepId: Int, index: Int, stepState: DrsStepState, title: String,
    primaryColor: androidx.compose.ui.graphics.Color,
    content: @androidx.compose.runtime.Composable
        androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val currentStepId = stepState.getCurrent().value
    val autoStepId = stepState.getCurrentAuto().value
    val contentVisible = currentStepId == ownStepId
    val stepTint = when (ownStepId) {
        currentStepId -> primaryColor
        else -> primaryColor.copy(alpha = 0.38f)
    }
    StepHeader(
        backgroundColor = stepTint,
        step = index, title = title,
        modifier = when {
            ownStepId <= autoStepId ->
                androidx.compose.ui.Modifier.clickable(enabled = !contentVisible) {
                    stepState.setCurrentManual(ownStepId)
                }
            else -> androidx.compose.ui.Modifier.alpha(alpha = 0.38f)
        },
    )
    val animSpec = androidx.compose.animation.core.spring<Float>(
        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
    )
    val weight = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (contentVisible) 1.0f else 0.00001f,
        animationSpec = animSpec,
    ).value
    androidx.compose.animation.AnimatedVisibility(
        visible = contentVisible,
        enter = androidx.compose.animation.fadeIn(animationSpec = animSpec),
        exit = androidx.compose.animation.fadeOut(animationSpec = animSpec),
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .weight(weight = weight),
    ) {
        val onBackground = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .padding(start = 56.dp)
                .drawBehind {
                    val strokeWidth = 2.dp
                    val connectorX = -(stepBadgeGap +
                        (stepBadgeDiameter / 2 - strokeWidth / 2))
                    drawLine(
                        color = onBackground,
                        start = androidx.compose.ui.geometry.Offset(connectorX.toPx(), 0f),
                        end = androidx.compose.ui.geometry.Offset(
                            connectorX.toPx(), size.height,
                        ),
                        strokeWidth = strokeWidth.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(2.dp.toPx(), 10.dp.toPx()),
                        ),
                        alpha = 0.12f,
                    )
                }) {
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .drsVerticalScroll()
                    .padding(end = 8.dp),
            ) {
                content()
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun StepHeader(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    backgroundColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color =
        androidx.compose.material3.contentColorFor(backgroundColor),
    step: Int, title: String,
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        modifier = modifier.padding(vertical = stepHeaderVerticalPad),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .padding(end = stepBadgeGap)
                .size(size = stepBadgeDiameter)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(color = backgroundColor),
        ) {
            androidx.compose.material3.Text(
                text = step.toString(), color = contentColor,
                modifier = androidx.compose.ui.Modifier
                    .align(alignment = androidx.compose.ui.Alignment.Center),
            )
        }

        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .height(height = stepTitleBoxHeight)
                .weight(weight = 1.0f)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(color = backgroundColor),
        ) {
            androidx.compose.material3.Text(
                text = title,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                maxLines = 1, color = contentColor,
                modifier = androidx.compose.ui.Modifier
                    .align(alignment = androidx.compose.ui.Alignment.Center)
                    .padding(horizontal = stepTitleInnerPad),
            )
        }
    }
}
