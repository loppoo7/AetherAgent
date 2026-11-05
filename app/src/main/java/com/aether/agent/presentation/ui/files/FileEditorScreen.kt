package com.aether.agent.presentation.ui.files

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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

private val codeStyle = TextStyle(fontFamily = AetherMono, fontSize = 13.sp, lineHeight = 20.sp)

@Composable
fun FileEditorScreen(
    onBack: () -> Unit,
    viewModel: FileEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val palette = LocalAetherPalette.current
    val density = LocalDensity.current

    var field by remember { mutableStateOf(TextFieldValue("")) }
    var loaded by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var current by remember { mutableIntStateOf(0) }
    var navTick by remember { mutableIntStateOf(0) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }

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
    val lineCount = field.text.count { it == '\n' } + 1

    // Gulir ke hasil pencarian yang dipilih. Hanya jalan saat pengguna pindah hasil, bukan saat mengetik.
    LaunchedEffect(navTick) {
        if (navTick == 0 || safeCurrent < 0) return@LaunchedEffect
        val l = layout ?: return@LaunchedEffect
        val line = l.getLineForOffset(matches[safeCurrent].first)
        val lineTop = l.getLineTop(line)
        val offsetTop = with(density) { (6.dp + 10.dp).toPx() }
        val viewport = with(density) { 160.dp.toPx() }
        vScroll.animateScrollTo((offsetTop + lineTop - viewport).coerceAtLeast(0f).toInt())
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

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
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
            else -> Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(vScroll)
                    .padding(top = 6.dp),
            ) {
                // Nomor baris: tinggi tiap baris sama dengan lineHeight editor, dan editor tidak membungkus baris,
                // jadi nomor selalu sejajar dengan barisnya. Baris panjang digulir ke samping.
                Column(
                    Modifier
                        .background(palette.codeHeader)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    for (i in 1..lineCount) {
                        Text(
                            i.toString(),
                            style = codeStyle.copy(fontSize = 11.sp, color = palette.muted),
                            fontFamily = AetherMono,
                        )
                    }
                }
                Box(Modifier.weight(1f).horizontalScroll(hScroll)) {
                    BasicTextField(
                        value = field,
                        onValueChange = {
                            field = it
                            viewModel.onTextChange(it.text)
                        },
                        textStyle = codeStyle.copy(color = palette.codeText),
                        cursorBrush = SolidColor(Terracotta),
                        visualTransformation = transformation,
                        onTextLayout = { layout = it },
                        modifier = Modifier
                            .background(palette.codeBg)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
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
