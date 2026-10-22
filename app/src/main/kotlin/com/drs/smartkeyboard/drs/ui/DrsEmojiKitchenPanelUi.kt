/*
 * Copyright (C) 2025-2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs.ui

import android.content.ClipData
import android.content.Context
import android.content.ClipboardManager as SystemClipboard
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drs.smartkeyboard.R
import com.drs.smartkeyboard.app.DrsPreferenceStore
import com.drs.smartkeyboard.drs.DrsEmojiKitchen
import com.drs.smartkeyboard.drs.DrsKitchenMemory
import com.drs.smartkeyboard.drs.DrsKitchenSendPlanner
import com.drs.smartkeyboard.drs.DrsStickerLibrary
import com.drs.smartkeyboard.drs.DrsSystems
import com.drs.smartkeyboard.drs.DrsStore
import com.drs.smartkeyboard.ime.ImeUiMode
import com.drs.smartkeyboard.ime.input.LocalInputFeedbackController
import com.drs.smartkeyboard.ime.keyboard.DrsImeSizing
import com.drs.smartkeyboard.ime.theme.DrsImeUi
import com.drs.smartkeyboard.ime.window.LocalWindowController
import com.drs.smartkeyboard.editorInstance
import com.drs.smartkeyboard.keyboardManager
import kotlinx.coroutines.launch
import org.drs.lib.android.showShortToastSync
import org.drs.lib.compose.stringRes
import org.drs.jetpref.datastore.model.collectAsState
import org.drs.lib.snygg.ui.SnyggBox
import org.drs.lib.snygg.ui.SnyggColumn
import org.drs.lib.snygg.ui.SnyggText

/**
 * مطبخ الإيموجي (the DRS Emoji Kitchen) — the seventh smart panel.
 *
 * Two tabs, one anatomy (the shared header + switcher chips + usage
 * store + incognito doctrine of every smart panel):
 * - «تركيبات»: the ORIGINAL local composition engine — pick a base
 *   emoji, the closed recipe engine ([DrsEmojiKitchen]) lists the
 *   renderable suggestions, the preview IS the sent PNG (rendered by
 *   [DrsKitchenArt], never a fake placeholder), and an uncovered pair
 *   stays honestly silent ([HonestSilenceChip]).
 * - «الملصقات»: the licensed original sticker library
 *   ([DrsStickerLibrary], docs/EMOJI_KITCHEN.md §4) with closed
 *   categories, search, favorites and history.
 *
 * The send mechanism is the DOCUMENTED commitContent API through the
 * FileProvider (no new permissions), with the honest fallback chain
 * decided by the pure [DrsKitchenSendPlanner]: image → text pair →
 * clipboard — every outcome announced by a toast.
 */

/** The persisted panel-usage namespace of the kitchen panel (local only). */
private const val USAGE_PANEL_KITCHEN = "kitchen"

/** The preview render size inside the panel (px). */
private const val PREVIEW_PX = 128

/** The grid-cell render size for composed previews (px). */
private const val CELL_PX = 96

@Composable
fun DrsEmojiKitchenPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val editorInstance by context.editorInstance()
    val prefs by DrsPreferenceStore
    val feedback = LocalInputFeedbackController.current
    val scope = rememberCoroutineScope()

    val memory by prefs.emojiKitchen.memoryData.collectAsState()
    val imagesEnabled by prefs.emojiKitchen.imagesEnabled.collectAsState()
    val recentsEnabled by prefs.panels.panelRecents.collectAsState()

    RecordPanelOpen(ImeUiMode.EMOJI_KITCHEN)

    var tab by remember { mutableIntStateOf(0) } // 0 = compose, 1 = stickers
    var base by remember { mutableStateOf(DrsEmojiKitchen.PARTICIPANTS.first().value) }
    var second by remember {
        mutableStateOf(DrsEmojiKitchen.suggestions(DrsEmojiKitchen.PARTICIPANTS.first().value).firstOrNull() ?: "")
    }
    var queryKitchen by remember { mutableStateOf("") }
    var queryStickers by remember { mutableStateOf("") }
    var stickerCategory by remember { mutableStateOf<DrsStickerLibrary.DrsStickerCategory?>(null) }
    var selectedStickerId by remember { mutableStateOf<String?>(null) }
    var memoryStamp by remember { mutableIntStateOf(0) }

    // the sticker index — loaded once from assets, fail-closed.
    var stickerIndex by remember { mutableStateOf<DrsStickerLibrary.StickerIndex?>(null) }
    LaunchedEffect(Unit) {
        stickerIndex = try {
            context.assets.open(DrsStickerLibrary.INDEX_ASSET).bufferedReader().use { reader ->
                DrsStickerLibrary.load(reader.readText())
            }
        } catch (_: Exception) {
            null
        }
    }

    val systemSpec = DrsSystems.specOfName(DrsStore.state.value.userPath)
    val accent = if (isSystemInDarkTheme()) systemSpec.accentNight else systemSpec.accent
    val windowSpec by LocalWindowController.current.activeWindowSpec.collectAsState()

    fun updateMemory(transform: (DrsKitchenMemory) -> DrsKitchenMemory) {
        scope.launch { prefs.emojiKitchen.memoryData.set(transform(memory)) }
        memoryStamp++
    }

    fun toast(res: Int) {
        context.showShortToastSync(context.getString(res))
    }

    fun copyUriToSystemClipboard(uri: Uri, label: String): Boolean = try {
        val clip = ClipData.newUri(context.contentResolver, label, uri)
        context.getSystemService(SystemClipboard::class.java)?.setPrimaryClip(clip)
        true
    } catch (_: Exception) {
        false
    }

    // ----------------------------------------------------- send & copy paths
    fun sendPlan(plan: DrsEmojiKitchen.KitchenPlan) {
        val mimes = editorInstance.activeContentMimeTypes
        when (DrsKitchenSendPlanner.plan(mimes, imagesEnabled, hasTextPair = true)) {
            DrsKitchenSendPlanner.SendPath.CONTENT -> {
                val uri = DrsKitchenArt.savePng(
                    context,
                    DrsKitchenArt.renderPlan(plan, DrsKitchenArt.MASTER_SIZE),
                    DrsKitchenArt.fileNameFor(plan.pairKey),
                )
                val committed = uri != null && editorInstance.commitImageContent(uri, plan.recipeId)
                if (committed) {
                    toast(R.string.panel__kitchen__sent_image)
                } else {
                    // the editor declined after all — the honest text pair.
                    editorInstance.commitText(plan.textPair)
                    toast(R.string.panel__kitchen__sent_text)
                }
            }
            else -> {
                editorInstance.commitText(plan.textPair)
                toast(R.string.panel__kitchen__sent_text)
            }
        }
        if (!keyboardManager.activeState.isIncognitoMode) {
            updateMemory { it.withHistoryPush(plan.pairKey) }
        }
        DrsPanelUsageStore.record(context, USAGE_PANEL_KITCHEN, plan.pairKey)
    }

    fun sendSticker(sticker: DrsStickerLibrary.Sticker) {
        val res = stickerDrawableRes(sticker.drawable) ?: return toast(R.string.panel__kitchen__send_failed)
        val mimes = editorInstance.activeContentMimeTypes
        when (DrsKitchenSendPlanner.plan(mimes, imagesEnabled, hasTextPair = false)) {
            DrsKitchenSendPlanner.SendPath.CONTENT -> {
                val uri = DrsKitchenArt.savePng(
                    context,
                    DrsKitchenArt.renderSticker(context, res, DrsKitchenArt.MASTER_SIZE),
                    DrsKitchenArt.fileNameFor(sticker.memoryKey),
                )
                val committed = uri != null && editorInstance.commitImageContent(uri, sticker.id)
                if (committed) {
                    toast(R.string.panel__kitchen__sent_image)
                } else if (uri != null && copyUriToSystemClipboard(uri, sticker.id)) {
                    toast(R.string.panel__kitchen__copied)
                } else {
                    toast(R.string.panel__kitchen__send_failed)
                }
            }
            else -> {
                // no text pair exists for a sticker — the honest clipboard path.
                val uri = DrsKitchenArt.savePng(
                    context,
                    DrsKitchenArt.renderSticker(context, res, DrsKitchenArt.MASTER_SIZE),
                    DrsKitchenArt.fileNameFor(sticker.memoryKey),
                )
                if (uri != null && copyUriToSystemClipboard(uri, sticker.id)) {
                    toast(R.string.panel__kitchen__copied)
                } else {
                    toast(R.string.panel__kitchen__send_failed)
                }
            }
        }
        if (!keyboardManager.activeState.isIncognitoMode) {
            updateMemory { it.withHistoryPush(sticker.memoryKey) }
        }
        DrsPanelUsageStore.record(context, USAGE_PANEL_KITCHEN, sticker.memoryKey)
    }

    fun copyPlan(plan: DrsEmojiKitchen.KitchenPlan) {
        val uri = DrsKitchenArt.savePng(
            context,
            DrsKitchenArt.renderPlan(plan, DrsKitchenArt.MASTER_SIZE),
            DrsKitchenArt.fileNameFor(plan.pairKey),
        )
        if (uri != null && copyUriToSystemClipboard(uri, plan.recipeId)) {
            toast(R.string.panel__kitchen__copied)
        } else {
            toast(R.string.panel__kitchen__send_failed)
        }
    }

    fun copySticker(sticker: DrsStickerLibrary.Sticker) {
        val res = stickerDrawableRes(sticker.drawable) ?: return toast(R.string.panel__kitchen__send_failed)
        val uri = DrsKitchenArt.savePng(
            context,
            DrsKitchenArt.renderSticker(context, res, DrsKitchenArt.MASTER_SIZE),
            DrsKitchenArt.fileNameFor(sticker.memoryKey),
        )
        if (uri != null && copyUriToSystemClipboard(uri, sticker.id)) {
            toast(R.string.panel__kitchen__copied)
        } else {
            toast(R.string.panel__kitchen__send_failed)
        }
    }

    // ------------------------------------------------------- derived state
    val baseOptions = remember(queryKitchen) {
        if (queryKitchen.isBlank()) {
            DrsEmojiKitchen.PARTICIPANTS.map { it.value }
        } else {
            DrsEmojiKitchen.searchEmoji(queryKitchen)
        }
    }
    val currentPlan = remember(base, second) { DrsEmojiKitchen.plan(base, second) }
    val suggestionValues = remember(base) { DrsEmojiKitchen.suggestions(base) }

    val stickers = remember(stickerIndex, stickerCategory, queryStickers) {
        val index = stickerIndex
        val category: DrsStickerLibrary.DrsStickerCategory? = stickerCategory
        when {
            index == null -> emptyList()
            queryStickers.isNotBlank() -> DrsStickerLibrary.search(index, queryStickers)
            category != null -> DrsStickerLibrary.byCategory(index, category)
            else -> index.stickers
        }
    }
    val selectedSticker = remember(selectedStickerId, stickerIndex) {
        val id = selectedStickerId ?: return@remember null
        stickerIndex?.let { DrsStickerLibrary.byId(it, id) }
    }

    val favoriteKeys = remember(memory) { memory.favorites }
    val historyKeys = remember(memory, recentsEnabled) {
        if (recentsEnabled) memory.history else emptyList()
    }

    SnyggColumn(
        modifier = modifier
            .fillMaxWidth()
            .height(DrsImeSizing.imeUiHeight()),
    ) {
        SmartPanelHeader(R.string.panel__kitchen__title, keyboardManager)
        PanelSwitcherChips(ImeUiMode.EMOJI_KITCHEN, keyboardManager, accent)

        // التبويبان — the two tabs (compose / stickers) + the honest
        // incognito note where it can be seen, never hidden.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AdviceChip(
                label = stringRes(R.string.panel__kitchen__tab_compose),
                accent = accent,
                leading = tab == 0,
                onClick = {
                    feedback.keyPress()
                    tab = 0
                },
            )
            AdviceChip(
                label = stringRes(R.string.panel__kitchen__tab_stickers),
                accent = accent,
                leading = tab == 1,
                onClick = {
                    feedback.keyPress()
                    tab = 1
                },
            )
            if (keyboardManager.activeState.isIncognitoMode) {
                AdviceChip(
                    label = stringRes(R.string.panel__kitchen__incognito_note),
                    accent = accent,
                    leading = false,
                )
            }
        }

        // المعاينة الحية — the live preview + actions: what you see IS
        // the PNG that gets sent (same renderer, same plan).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    tab == 0 && currentPlan != null -> KitchenPlanPreview(currentPlan, 64.dp)
                    tab == 0 -> HonestSilenceChip(accent)
                    tab == 1 && selectedSticker != null -> StickerImage(selectedSticker, 56.dp)
                    else -> SnyggText(
                        elementName = DrsImeUi.ClipboardSubheader.elementName,
                        text = stringRes(R.string.panel__kitchen__no_results),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            when {
                tab == 0 && currentPlan != null -> KitchenActionRow(
                    accent = accent,
                    isFavorite = currentPlan.pairKey in favoriteKeys,
                    onSend = {
                        feedback.keyPress()
                        sendPlan(currentPlan)
                    },
                    onCopy = {
                        feedback.keyPress()
                        copyPlan(currentPlan)
                    },
                    onFavorite = {
                        feedback.keyPress()
                        updateMemory { it.withFavoriteToggled(currentPlan.pairKey) }
                    },
                )
                tab == 1 && selectedSticker != null -> KitchenActionRow(
                    accent = accent,
                    isFavorite = selectedSticker.memoryKey in favoriteKeys,
                    onSend = {
                        feedback.keyPress()
                        sendSticker(selectedSticker)
                    },
                    onCopy = {
                        feedback.keyPress()
                        copySticker(selectedSticker)
                    },
                    onFavorite = {
                        feedback.keyPress()
                        updateMemory { it.withFavoriteToggled(selectedSticker.memoryKey) }
                    },
                )
            }
        }

        // المفضلة والسجل — favorites + history rows (resolved honestly).
        if (favoriteKeys.isNotEmpty()) {
            MemoryRow(
                label = stringRes(R.string.panel__kitchen__favorites),
                keys = favoriteKeys,
                accent = accent,
                stickerIndex = stickerIndex,
                onPickPair = { picked ->
                    base = picked.first
                    second = picked.second
                    tab = 0
                },
                onPickSticker = { id ->
                    selectedStickerId = id
                    tab = 1
                },
                selectedKey = if (tab == 0) currentPlan?.pairKey else selectedSticker?.memoryKey,
            )
        }
        if (historyKeys.isNotEmpty()) {
            MemoryRow(
                label = stringRes(R.string.panel__kitchen__history),
                keys = historyKeys,
                accent = accent,
                stickerIndex = stickerIndex,
                onPickPair = { picked ->
                    base = picked.first
                    second = picked.second
                    tab = 0
                },
                onPickSticker = { id ->
                    selectedStickerId = id
                    tab = 1
                },
                selectedKey = if (tab == 0) currentPlan?.pairKey else selectedSticker?.memoryKey,
            )
        }

        when (tab) {
            0 -> {
                KitchenSearchField(
                    query = queryKitchen,
                    accent = accent,
                    hintRes = R.string.panel__kitchen__search_hint,
                    onChange = { queryKitchen = it },
                )
                // شريط الأساس — the base picker (search-filtered).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    baseOptions.forEach { value ->
                        EmojiCell(
                            glyph = value,
                            selected = value == base,
                            accent = accent,
                            sizeDp = 36.dp,
                            onClick = {
                                feedback.keyPress()
                                base = value
                                second = DrsEmojiKitchen.suggestions(value).firstOrNull() ?: ""
                            },
                        )
                    }
                }
                // شريط الزينة — the second-emoji suggestions (renderable only).
                if (suggestionValues.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        suggestionValues.forEach { value ->
                            EmojiCell(
                                glyph = value,
                                selected = value == second,
                                accent = accent,
                                sizeDp = 32.dp,
                                onClick = {
                                    feedback.keyPress()
                                    second = value
                                },
                            )
                        }
                    }
                }
            }
            1 -> {
                KitchenSearchField(
                    query = queryStickers,
                    accent = accent,
                    hintRes = R.string.panel__kitchen__search_hint,
                    onChange = { queryStickers = it },
                )
                // شرائح التصنيفات — the closed category chips (الكل first).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AdviceChip(
                        label = stringRes(R.string.panel__kitchen__all),
                        accent = accent,
                        leading = stickerCategory == null,
                        onClick = {
                            feedback.keyPress()
                            stickerCategory = null
                        },
                    )
                    DrsStickerLibrary.DrsStickerCategory.entries.forEach { category ->
                        AdviceChip(
                            label = stringRes(stickerCategoryLabel(category)),
                            accent = accent,
                            leading = stickerCategory == category,
                            onClick = {
                                feedback.keyPress()
                                stickerCategory = category
                            },
                        )
                    }
                }
            }
        }

        // الشبكة — the results grid (compositions or stickers).
        SnyggBox(
            elementName = DrsImeUi.ClipboardContent.elementName,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when {
                tab == 0 && currentPlan == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        HonestSilenceChip(accent)
                    }
                }
                tab == 0 -> LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = windowSpec.keyMarginH),
                    columns = GridCells.Fixed(4),
                ) {
                    items(suggestionValues, key = { it }) { candidate ->
                        val plan = remember(base, candidate) { DrsEmojiKitchen.plan(base, candidate) }
                        if (plan != null) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        feedback.keyPress()
                                        second = candidate
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                KitchenPlanPreview(plan, CELL_PX.dp)
                            }
                        }
                    }
                }
                tab == 1 && stickers.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SnyggText(
                            elementName = DrsImeUi.ClipboardSubheader.elementName,
                            text = stringRes(R.string.panel__kitchen__no_results),
                        )
                    }
                }
                else -> LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = windowSpec.keyMarginH),
                    columns = GridCells.Fixed(4),
                ) {
                    items(stickers, key = { it.id }) { sticker ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    feedback.keyPress()
                                    selectedStickerId = sticker.id
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            StickerIcon(sticker)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

// ------------------------------------------------------------ helpers

/** The closed drawable-name → resource map for the shipped original pack. */
internal fun stickerDrawableRes(name: String): Int? = when (name) {
    "st_kitchen_star_face" -> R.drawable.st_kitchen_star_face
    "st_kitchen_cool_face" -> R.drawable.st_kitchen_cool_face
    "st_kitchen_paper_heart" -> R.drawable.st_kitchen_paper_heart
    "st_kitchen_spark_heart" -> R.drawable.st_kitchen_spark_heart
    "st_kitchen_round_cat" -> R.drawable.st_kitchen_round_cat
    "st_kitchen_sleepy_panda" -> R.drawable.st_kitchen_sleepy_panda
    "st_kitchen_coffee_cup" -> R.drawable.st_kitchen_coffee_cup
    "st_kitchen_watermelon_slice" -> R.drawable.st_kitchen_watermelon_slice
    "st_kitchen_rocket" -> R.drawable.st_kitchen_rocket
    "st_kitchen_gift_box" -> R.drawable.st_kitchen_gift_box
    "st_kitchen_party_cone" -> R.drawable.st_kitchen_party_cone
    "st_kitchen_confetti_star" -> R.drawable.st_kitchen_confetti_star
    else -> null
}

/** The localized label of a closed sticker category. */
@Composable
private fun stickerCategoryLabel(category: DrsStickerLibrary.DrsStickerCategory): Int = when (category) {
    DrsStickerLibrary.DrsStickerCategory.SMILEYS -> R.string.panel__kitchen__cat_smileys
    DrsStickerLibrary.DrsStickerCategory.HEARTS -> R.string.panel__kitchen__cat_hearts
    DrsStickerLibrary.DrsStickerCategory.ANIMALS -> R.string.panel__kitchen__cat_animals
    DrsStickerLibrary.DrsStickerCategory.FOOD -> R.string.panel__kitchen__cat_food
    DrsStickerLibrary.DrsStickerCategory.OBJECTS -> R.string.panel__kitchen__cat_objects
    DrsStickerLibrary.DrsStickerCategory.CELEBRATION -> R.string.panel__kitchen__cat_celebration
}

// ------------------------------------------------------------ small composables
@Composable
private fun KitchenPlanPreview(plan: DrsEmojiKitchen.KitchenPlan, size: androidx.compose.ui.unit.Dp) {
    val bitmap = remember(plan) { DrsKitchenArt.renderPlan(plan, PREVIEW_PX) }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = plan.recipeId,
        modifier = Modifier.size(size),
    )
}

@Composable
private fun StickerImage(sticker: DrsStickerLibrary.Sticker, size: androidx.compose.ui.unit.Dp) {
    val res = stickerDrawableRes(sticker.drawable) ?: return
    Image(
        painter = painterResource(res),
        contentDescription = sticker.id,
        modifier = Modifier.size(size),
    )
}

@Composable
private fun StickerIcon(sticker: DrsStickerLibrary.Sticker) {
    val res = stickerDrawableRes(sticker.drawable) ?: return
    Image(
        painter = painterResource(res),
        contentDescription = sticker.id,
        modifier = Modifier.size(52.dp),
    )
}

@Composable
private fun KitchenActionRow(
    accent: Color,
    isFavorite: Boolean,
    onSend: () -> Unit,
    onCopy: () -> Unit,
    onFavorite: () -> Unit,
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AdviceChip(
                label = stringRes(R.string.panel__kitchen__send),
                accent = accent,
                leading = true,
                onClick = onSend,
            )
            AdviceChip(
                label = stringRes(R.string.panel__kitchen__copy),
                accent = accent,
                leading = true,
                onClick = onCopy,
            )
        }
        Spacer(Modifier.height(2.dp))
        AdviceChip(
            label = stringRes(
                if (isFavorite) R.string.panel__kitchen__unfavorite else R.string.panel__kitchen__favorite,
            ),
            accent = accent,
            leading = isFavorite,
            onClick = onFavorite,
        )
    }
}

@Composable
private fun KitchenSearchField(
    query: String,
    accent: Color,
    hintRes: Int,
    onChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp),
            cursorBrush = SolidColor(accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) {
                        SnyggText(
                            elementName = DrsImeUi.ClipboardSubheader.elementName,
                            text = stringRes(hintRes),
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun EmojiCell(
    glyph: String,
    selected: Boolean,
    accent: Color,
    sizeDp: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(if (selected) accent.copy(alpha = 0.22f) else accent.copy(alpha = 0.06f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = glyph,
            fontSize = (sizeDp.value * 0.55f).sp,
        )
    }
}

@Composable
private fun HonestSilenceChip(accent: Color) {
    AdviceChip(
        label = stringRes(R.string.panel__kitchen__no_recipe),
        accent = accent,
        leading = false,
    )
}

/** One favorites/history row: pair keys render mini previews, stickers render their icon. */
@Composable
private fun MemoryRow(
    label: String,
    keys: List<String>,
    accent: Color,
    stickerIndex: DrsStickerLibrary.StickerIndex?,
    onPickPair: (Pair<String, String>) -> Unit,
    onPickSticker: (String) -> Unit,
    selectedKey: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SnyggText(
            elementName = DrsImeUi.ClipboardSubheader.elementName,
            text = label,
            modifier = Modifier.width(56.dp),
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            keys.forEach { key ->
                val parts = remember(key) { key.removePrefix("pair:").split("\u0000") }
                when {
                    key.startsWith("pair:") && parts.size == 2 -> {
                        val plan = remember(key) { DrsEmojiKitchen.plan(parts[0], parts[1]) }
                        if (plan != null) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (key == selectedKey) accent.copy(alpha = 0.22f)
                                        else accent.copy(alpha = 0.06f),
                                    )
                                    .clickable { onPickPair(parts[0] to parts[1]) },
                                contentAlignment = Alignment.Center,
                            ) {
                                KitchenPlanPreview(plan, 36.dp)
                            }
                        }
                    }
                    key.startsWith("sticker:") -> {
                        val id = key.removePrefix("sticker:")
                        val sticker = remember(key, stickerIndex) {
                            stickerIndex?.let { DrsStickerLibrary.byId(it, id) }
                        }
                        if (sticker != null) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (key == selectedKey) accent.copy(alpha = 0.22f)
                                        else accent.copy(alpha = 0.06f),
                                    )
                                    .clickable { onPickSticker(id) },
                                contentAlignment = Alignment.Center,
                            ) {
                                StickerIcon(sticker)
                            }
                        }
                    }
                }
            }
        }
    }
}
