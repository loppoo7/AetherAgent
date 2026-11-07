package com.aether.agent.presentation.ui.files

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aether.agent.presentation.components.AIcon
import com.aether.agent.presentation.components.AetherIcon
import com.aether.agent.presentation.components.IconAction
import com.aether.agent.presentation.components.ThinkingSteps
import com.aether.agent.presentation.components.findMatches
import com.aether.agent.presentation.components.highlightCodeAroundLine
import com.aether.agent.presentation.ui.theme.AetherMono
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta
import com.aether.agent.presentation.viewmodel.EditorChatViewModel
import com.aether.agent.presentation.viewmodel.FileEditorViewModel
import kotlin.math.roundToInt

private val codeStyle = TextStyle(fontFamily = AetherMono, fontSize = 13.sp, lineHeight = 20.sp)
private val GutterWidth = 44.dp
private val EditorPadTop = 10.dp
private const val CURSOR_BUCKET_LINES = 200
private val CursorMargin = 24.dp
private const val MAX_HIGHLIGHT = 300
private const val MAX_NUMBER_CACHE = 2_000

/** Posisi awal tiap baris. Dihitung sekali per perubahan teks, menggantikan pemindaian prefix berulang. */
private class LineIndex(text: String) {
    private val starts: IntArray

    init {
        var newlines = 0
        for (c in text) if (c == '\n') newlines++
        starts = IntArray(newlines + 1)
        var k = 1
        for (i in text.indices) {
            if (text[i] == '\n') starts[k++] = i + 1
        }
    }

    val lineCount: Int get() = starts.size

    /** Nomor baris (mulai dari 0) untuk posisi karakter [offset]. */
    fun lineOf(offset: Int): Int {
        val target = offset.coerceAtLeast(0)
        var lo = 0
        var hi = starts.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) ushr 1
            if (starts[mid] <= target) lo = mid else hi = mid - 1
        }
        return lo
    }
}

/**
 * Pewarnaan sintaks untuk teks yang sama dicache: BasicTextField memanggil filter() lebih sering
 * dari perubahan teks, dan tanpa cache setiap panggilan memproses ulang jendela highlight.
 */
private class HighlightTransformation(
    private val matches: List<IntRange>,
    private val current: Int,
    private val bucket: Int,
) : VisualTransformation {
    private var cachedSource: String? = null
    private var cachedResult: TransformedText? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val cached = cachedResult
        if (cached != null && cachedSource == text.text) return cached
        val result = TransformedText(
            highlightCodeAroundLine(
                text.text,
                bucket * CURSOR_BUCKET_LINES + CURSOR_BUCKET_LINES / 2,
                matches,
                current,
            ),
            OffsetMapping.Identity,
        )
        cachedSource = text.text
        cachedResult = result
        return result
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FileEditorScreen(
    onBack: () -> Unit,
    viewModel: FileEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val palette = LocalAetherPalette.current
    val density = LocalDensity.current
    val padTopPx = with(density) { EditorPadTop.toPx() }
    val cursorMarginPx = with(density) { CursorMargin.toPx() }
    val lineHeightPx = with(density) { codeStyle.lineHeight.toPx() }
    val textMeasurer = rememberTextMeasurer()
    val numberStyle = codeStyle.copy(fontSize = 11.sp, color = palette.muted)
    val numberCache = remember(numberStyle) { HashMap<Int, TextLayoutResult>() }

    var field by remember { mutableStateOf(TextFieldValue("")) }
    val history = remember { EditorHistory(TextFieldValue("")) }
    var loaded by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var current by remember { mutableIntStateOf(0) }
    var navTick by remember { mutableIntStateOf(0) }
    var jumpToFirstMatch by remember { mutableStateOf(false) }
    var searchTick by remember { mutableIntStateOf(0) }
    var viewportPx by remember { mutableIntStateOf(0) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var showChat by rememberSaveable { mutableStateOf(false) }
    val chatVm: EditorChatViewModel = hiltViewModel()

    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    LaunchedEffect(state.loading) {
        if (!state.loading && !loaded) {
            field = TextFieldValue(viewModel.startText)
            history.reset(field)
            loaded = true
        }
    }

    // Indeks baris hanya dibangun ulang ketika teks berubah, bukan di setiap recompose atau pindah kursor.
    val index = remember(field.text) { LineIndex(field.text) }
    val lineCount = index.lineCount
    val dirty = loaded && field.text != state.original

    val matches = remember(field.text, query) { findMatches(field.text, query) }
    val safeCurrent = if (matches.isEmpty()) -1 else current.coerceIn(0, matches.lastIndex)
    // Hanya kecocokan di sekitar yang sedang dipilih yang diwarnai.
    val windowStart = if (matches.size <= MAX_HIGHLIGHT) 0
        else (safeCurrent - MAX_HIGHLIGHT / 2).coerceIn(0, matches.size - MAX_HIGHLIGHT)
    val visibleMatches = if (matches.size <= MAX_HIGHLIGHT) matches
        else matches.subList(windowStart, windowStart + MAX_HIGHLIGHT)
    val visibleCurrent = if (safeCurrent < 0) -1 else safeCurrent - windowStart
    val cursorBucket = index.lineOf(field.selection.end) / CURSOR_BUCKET_LINES
    val transformation = remember(visibleMatches, visibleCurrent, cursorBucket) {
        HighlightTransformation(visibleMatches, visibleCurrent, cursorBucket)
    }

    val contentHeight = with(density) { (lineCount * lineHeightPx + padTopPx * 2).toDp() }

    // Gulir ke hasil pencarian yang dipilih.
    LaunchedEffect(navTick) {
        if (navTick == 0 || safeCurrent < 0) return@LaunchedEffect
        val line = index.lineOf(matches[safeCurrent].first)
        val viewport = with(density) { 160.dp.toPx() }
        val target = padTopPx + line * lineHeightPx - viewport
        vScroll.animateScrollTo(target.coerceAtLeast(0f).roundToInt())
    }

    fun goTo(match: Int) {
        if (matches.isEmpty()) return
        current = match
        navTick++
        val r = matches[match]
        field = field.copy(selection = TextRange(r.first, r.last + 1))
    }

    // Kursor selalu digulir agar tetap terlihat, termasuk saat keyboard muncul.
    LaunchedEffect(field.selection, viewportPx, index) {
        if (viewportPx == 0) return@LaunchedEffect
        val line = index.lineOf(field.selection.end)
        val top = padTopPx + line * lineHeightPx
        val bottom = top + lineHeightPx
        val view = vScroll.value.toFloat()
        val viewH = viewportPx.toFloat()
        val target = when {
            bottom + cursorMarginPx > view + viewH -> bottom + cursorMarginPx - viewH
            top - cursorMarginPx < view -> top - cursorMarginPx
            else -> null
        }
        if (target != null) vScroll.scrollTo(target.coerceAtLeast(0f).roundToInt())
    }

    // Lompat ke kecocokan pertama. Dipicu searchTick (bukan daftar matches, yang dibandingkan per isi),
    // dan dijalankan setelah composition ini, sehingga matches yang dipakai sudah untuk kata kunci baru.
    LaunchedEffect(searchTick) {
        if (jumpToFirstMatch) {
            jumpToFirstMatch = false
            if (matches.isNotEmpty()) goTo(0)
        }
    }

    fun applyHistory(value: TextFieldValue?) {
        if (value == null) return
        field = value
        viewModel.onTextChange(value.text)
    }

    fun leave() {
        if (dirty) confirmDiscard = true else onBack()
    }

    BackHandler(enabled = dirty) { confirmDiscard = true }

    val imeVisible = WindowInsets.isImeVisible
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .windowInsetsPadding(if (imeVisible) WindowInsets.ime.union(WindowInsets.navigationBars) else WindowInsets.navigationBars),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAction(AetherIcon.Back, "Kembali", ::leave)
            Column(Modifier.weight(1f)) {
                Text(
                    viewModel.path.substringAfterLast('/'),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    viewModel.path + if (dirty) " · belum disimpan" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted,
                    maxLines = 1,
                )
            }
            IconAction(
                AetherIcon.Undo, "Urungkan",
                { applyHistory(history.undo()) },
                enabled = history.canUndo && loaded,
            )
            IconAction(
                AetherIcon.Redo, "Ulangi",
                { applyHistory(history.redo()) },
                enabled = history.canRedo && loaded,
            )
            IconAction(AetherIcon.Sparkle, if (showChat) "Tutup Chat AI" else "Tanya AI", {
                showChat = !showChat
            }, tint = Terracotta)
            IconAction(AetherIcon.Search, "Cari di file", {
                searchOpen = !searchOpen
                if (!searchOpen) query = ""
            })
        }

        AnimatedVisibility(visible = searchOpen) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.userBubble)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AIcon(AetherIcon.Search, tint = palette.muted, size = 16.dp)
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        current = 0
                        jumpToFirstMatch = it.isNotEmpty()
                        searchTick++
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(Terracotta),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (matches.isEmpty()) "0" else "${safeCurrent + 1}/${matches.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted,
                )
                IconAction(AetherIcon.ChevronDown, "Berikutnya", {
                    if (matches.isNotEmpty()) goTo((safeCurrent + 1) % matches.size)
                }, boxSize = 32.dp, iconSize = 16.dp)
            }
        }

        when {
            state.loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Terracotta)
            }
            state.error != null -> Box(Modifier.fillMaxWidth().weight(1f).padding(20.dp), contentAlignment = Alignment.Center) {
                Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            else -> {
                Row(Modifier.weight(if (showChat) 0.55f else 1f).fillMaxWidth()) {
                    // Kolom nomor baris ikut naik-turun lewat ScrollState vertikal yang sama dengan editor.
                    Box(
                        Modifier
                            .width(GutterWidth)
                            .fillMaxHeight()
                            .background(palette.codeHeader)
                            .verticalScroll(vScroll),
                    ) {
                        Canvas(Modifier.width(GutterWidth).height(contentHeight)) {
                            if (viewportPx == 0) return@Canvas
                            val padTop = EditorPadTop.toPx()
                            val first = ((vScroll.value - padTop) / lineHeightPx).toInt().coerceIn(0, lineCount - 1)
                            val last = ((vScroll.value + viewportPx - padTop) / lineHeightPx).toInt().coerceIn(0, lineCount - 1)
                            val rightEdge = GutterWidth.toPx() - 8.dp.toPx()
                            if (numberCache.size > MAX_NUMBER_CACHE) numberCache.clear()
                            for (line in first..last) {
                                val measured = numberCache.getOrPut(line + 1) {
                                    textMeasurer.measure((line + 1).toString(), numberStyle)
                                }
                                drawText(
                                    measured,
                                    topLeft = Offset(rightEdge - measured.size.width, padTop + line * lineHeightPx),
                                )
                            }
                        }
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .onSizeChanged { viewportPx = it.height }
                            .verticalScroll(vScroll),
                    ) {
                        Box(Modifier.horizontalScroll(hScroll)) {
                            BasicTextField(
                                value = field,
                                onValueChange = { raw ->
                                    val next = applyAutoIndent(field, raw)
                                    field = next
                                    history.record(next)
                                    viewModel.onTextChange(next.text)
                                },
                                textStyle = codeStyle.copy(color = palette.codeText),
                                cursorBrush = SolidColor(Terracotta),
                                visualTransformation = transformation,
                                modifier = Modifier
                                    .background(palette.codeBg)
                                    .padding(start = 8.dp, end = 12.dp, top = EditorPadTop, bottom = EditorPadTop),
                            )
                        }
                    }
                }
            }
        }

        if (showChat) {
            EditorChatPanel(
                chatVm = chatVm,
                fileName = viewModel.path.substringAfterLast('/'),
                fileText = field.text,
                onClose = { showChat = false },
                modifier = Modifier.weight(0.45f),
            )
        }

        state.notice?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { viewModel.save(restart = false, text = field.text) },
                enabled = dirty && !state.saving && !state.loading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
            ) { Text("Simpan") }
            Button(
                onClick = { viewModel.save(restart = true, text = field.text) },
                enabled = dirty && !state.saving && !state.loading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1.4f),
            ) { Text(if (state.saving) "Menyimpan…" else "Simpan & Restart") }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Buang perubahan?") },
            text = { Text("Perubahan yang belum disimpan akan hilang.") },
            confirmButton = {
                Button(onClick = {
                    confirmDiscard = false
                    onBack()
                }) { Text("Buang") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Lanjut edit") } },
        )
    }
}

/**
 * Auto indent: saat Enter ditekan, indentasi baris sebelumnya ikut turun.
 * Jika baris sebelumnya berakhir dengan ':', indentasi bertambah 4 spasi.
 */
private fun applyAutoIndent(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    val newText = new.text
    val caret = new.selection.end
    if (!new.selection.collapsed || caret < 1) return new
    if (newText.length != old.text.length + 1 || newText[caret - 1] != '\n') return new

    val prevLineStart = newText.lastIndexOf('\n', caret - 2) + 1
    val prevLine = newText.substring(prevLineStart, caret - 1)
    val indent = prevLine.takeWhile { it == ' ' || it == '\t' }
    val extra = if (prevLine.trimEnd().endsWith(":")) "    " else ""
    val insert = indent + extra
    if (insert.isEmpty()) return new

    val updated = newText.substring(0, caret) + insert + newText.substring(caret)
    return TextFieldValue(updated, TextRange(caret + insert.length))
}

/** Panel chat di bawah editor. Riwayat dan konteksnya mengikuti file yang sedang dibuka. */
@Composable
private fun EditorChatPanel(
    chatVm: EditorChatViewModel,
    fileName: String,
    fileText: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAetherPalette.current
    val messages by chatVm.messages.collectAsState()
    val running by chatVm.running.collectAsState()
    val activity by chatVm.activity.collectAsState()
    val steps by chatVm.thinkingSteps.collectAsState()
    val error by chatVm.error.collectAsState()
    val approval by chatVm.approval.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        modifier
            .fillMaxWidth()
            .background(palette.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Chat AI", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(8.dp))
            Text(
                fileName,
                style = MaterialTheme.typography.labelSmall,
                color = palette.muted,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { chatVm.clear() }, enabled = !running) { Text("Hapus") }
            IconAction(AetherIcon.Close, "Tutup chat", onClose, boxSize = 36.dp, iconSize = 18.dp)
        }

        if (messages.isEmpty()) {
            Text(
                "Tanyakan sesuatu tentang file ini. Konteksnya adalah isi editor saat ini.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(messages, key = { it.id }) { m ->
                    val mine = m.role == "user"
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.88f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (mine) palette.userBubble else palette.codeBg)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Text(m.content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        if (running) {
            ThinkingSteps(steps = steps, running = true, onStop = { chatVm.stop() })
        }
        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Tanya tentang file ini…") },
                maxLines = 4,
                modifier = Modifier.weight(1f),
            )
            IconAction(
                icon = AetherIcon.ArrowUp,
                description = "Kirim",
                onClick = {
                    chatVm.send(input, fileText)
                    input = ""
                },
                tint = Terracotta,
                enabled = input.isNotBlank() && !running,
            )
        }
    }

    approval?.let { pending ->
        AlertDialog(
            onDismissRequest = { chatVm.resolveApproval(false) },
            title = { Text(pending.title) },
            text = {
                Box(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    Text(pending.diff, style = MaterialTheme.typography.bodySmall, fontFamily = AetherMono)
                }
            },
            confirmButton = { Button(onClick = { chatVm.resolveApproval(true) }) { Text("Setujui") } },
            dismissButton = { TextButton(onClick = { chatVm.resolveApproval(false) }) { Text("Tolak") } },
        )
    }
}

