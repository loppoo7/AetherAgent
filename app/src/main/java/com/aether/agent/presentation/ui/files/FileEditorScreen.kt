package com.aether.agent.presentation.ui.files

import com.aether.agent.presentation.components.ThinkingSteps

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.aether.agent.presentation.viewmodel.EditorChatViewModel
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.aether.agent.presentation.components.findMatches
import com.aether.agent.presentation.components.highlightCode
import com.aether.agent.presentation.ui.theme.AetherMono
import com.aether.agent.presentation.ui.theme.LocalAetherPalette
import com.aether.agent.presentation.ui.theme.Terracotta
import com.aether.agent.presentation.viewmodel.FileEditorViewModel
import kotlin.math.roundToInt

private val codeStyle = TextStyle(fontFamily = AetherMono, fontSize = 13.sp, lineHeight = 20.sp)
private val GutterWidth = 44.dp
private val EditorPadTop = 10.dp
private val CursorMargin = 24.dp

@Composable
fun FileEditorScreen(
    onBack: () -> Unit,
    viewModel: FileEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val palette = LocalAetherPalette.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val numberStyle = codeStyle.copy(fontSize = 11.sp, color = palette.muted)

    var field by remember { mutableStateOf(TextFieldValue("")) }
    var loaded by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var current by remember { mutableIntStateOf(0) }
    var navTick by remember { mutableIntStateOf(0) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var viewportPx by remember { mutableIntStateOf(0) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var showChat by rememberSaveable { mutableStateOf(false) }
    val chatVm: EditorChatViewModel = hiltViewModel()

    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    LaunchedEffect(state.loading) {
        if (!state.loading && !loaded) {
            field = TextFieldValue(state.text)
            loaded = true
        }
    }

    val matches = remember(field.text, query) { findMatches(field.text, query) }
    val safeCurrent = if (matches.isEmpty()) -1 else current.coerceIn(0, matches.lastIndex)
    val transformation = remember(matches, safeCurrent) {
        object : VisualTransformation {
            override fun filter(text: AnnotatedString): TransformedText =
                TransformedText(highlightCode(text.text, matches, safeCurrent), OffsetMapping.Identity)
        }
    }

    // Gulir ke hasil pencarian yang dipilih. Hanya jalan saat pengguna pindah hasil, bukan saat mengetik.
    LaunchedEffect(navTick) {
        if (navTick == 0 || safeCurrent < 0) return@LaunchedEffect
        val l = layout ?: return@LaunchedEffect
        val line = l.getLineForOffset(matches[safeCurrent].first)
        val lineTop = l.getLineTop(line)
        val offsetTop = with(density) { EditorPadTop.toPx() }
        val viewport = with(density) { 160.dp.toPx() }
        vScroll.animateScrollTo((offsetTop + lineTop - viewport).coerceAtLeast(0f).roundToInt())
    }

    // Kursor selalu digulir agar tetap terlihat, termasuk saat keyboard muncul dan menutupi layar.
    LaunchedEffect(field.selection, layout, viewportPx) {
        val l = layout ?: return@LaunchedEffect
        if (viewportPx == 0) return@LaunchedEffect
        val offset = field.selection.end.coerceIn(0, field.text.length)
        val rect = l.getCursorRect(offset)
        val padTop = with(density) { EditorPadTop.toPx() }
        val margin = with(density) { CursorMargin.toPx() }
        val top = padTop + rect.top
        val bottom = padTop + rect.bottom
        val view = vScroll.value.toFloat()
        val viewH = viewportPx.toFloat()
        val target = when {
            bottom + margin > view + viewH -> bottom + margin - viewH
            top - margin < view -> top - margin
            else -> null
        }
        if (target != null) vScroll.scrollTo(target.coerceAtLeast(0f).roundToInt())
    }

    fun goTo(index: Int) {
        if (matches.isEmpty()) return
        current = index
        navTick++
        val r = matches[index]
        field = field.copy(selection = TextRange(r.first, r.last + 1))
    }

    fun leave() {
        if (state.dirty) confirmDiscard = true else onBack()
    }

    BackHandler(enabled = state.dirty) { confirmDiscard = true }

    Column(Modifier.fillMaxSize().statusBarsPadding().windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))) {
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
                    viewModel.path + if (state.dirty) " · belum disimpan" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted,
                    maxLines = 1,
                )
            }
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
                        if (it.isNotEmpty()) goTo(0)
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
                // Tinggi konten editor, dipakai agar kolom nomor baris punya tinggi scroll yang sama.
                val contentHeight = with(density) {
                    ((layout?.size?.height ?: 0) + EditorPadTop.toPx() * 2).toDp()
                }
                Row(Modifier.weight(if (showChat) 0.55f else 1f).fillMaxWidth()) {
                    // Kolom nomor baris: tidak ikut digeser ke samping, dan ikut bergerak naik-turun
                    // bersama editor karena memakai ScrollState vertikal yang sama.
                    Box(
                        Modifier
                            .width(GutterWidth)
                            .fillMaxHeight()
                            .background(palette.codeHeader)
                            .verticalScroll(vScroll),
                    ) {
                        Canvas(Modifier.width(GutterWidth).height(contentHeight)) {
                            val l = layout ?: return@Canvas
                            if (l.lineCount == 0) return@Canvas
                            val padTop = EditorPadTop.toPx()
                            val firstY = (vScroll.value - padTop).coerceAtLeast(0f)
                            val lastY = (vScroll.value + viewportPx - padTop).coerceAtLeast(0f)
                            val first = l.getLineForVerticalPosition(firstY)
                            val last = l.getLineForVerticalPosition(lastY).coerceAtMost(l.lineCount - 1)
                            val rightEdge = GutterWidth.toPx() - 8.dp.toPx()
                            for (line in first..last) {
                                val measured = textMeasurer.measure((line + 1).toString(), numberStyle)
                                drawText(
                                    measured,
                                    topLeft = Offset(
                                        rightEdge - measured.size.width,
                                        padTop + l.getLineTop(line),
                                    ),
                                )
                            }
                        }
                    }
                    // Editor: digeser vertikal bersama kolom nomor, dan horizontal sendiri.
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
                                onValueChange = {
                                    val next = applyAutoIndent(field, it)
                                    field = next
                                    viewModel.onTextChange(next.text)
                                },
                                textStyle = codeStyle.copy(color = palette.codeText),
                                cursorBrush = SolidColor(Terracotta),
                                visualTransformation = transformation,
                                onTextLayout = { layout = it },
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
                onClick = { viewModel.save(restart = false) },
                enabled = state.dirty && !state.saving && !state.loading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
            ) { Text("Simpan") }
            Button(
                onClick = { viewModel.save(restart = true) },
                enabled = state.dirty && !state.saving && !state.loading,
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
