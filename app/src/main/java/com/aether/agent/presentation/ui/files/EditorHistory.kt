package com.aether.agent.presentation.ui.files

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Riwayat Undo/Redo editor. Ketikan huruf beruntun digabung menjadi satu langkah, jadi satu kali Undo
 * tidak hanya membatalkan satu karakter. Spasi, Enter, hapus, paste, dan pindah baris selalu menjadi batas langkah.
 * Setiap langkah menyimpan teks DAN posisi kursor, sehingga kursor kembali ke tempat yang benar.
 */
class EditorHistory(initial: TextFieldValue, private val limit: Int = 300) {

    private enum class Kind { NONE, TYPE, DELETE, OTHER }

    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private var present: TextFieldValue = initial.clean()
    private var lastKind = Kind.NONE

    /** Dibaca di composable; berubah hanya saat kemampuan undo/redo berubah. */
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    fun reset(value: TextFieldValue) {
        undoStack.clear()
        redoStack.clear()
        present = value.clean()
        lastKind = Kind.NONE
        refresh()
    }

    /** Dipanggil setiap editor berubah. Perubahan kursor saja tidak masuk riwayat. */
    fun record(next: TextFieldValue) {
        val old = present
        if (next.text == old.text) {
            present = next.clean()
            return
        }
        val kind = classify(old.text.length, next)
        val continuesGroup = kind != Kind.OTHER && kind == lastKind
        if (!continuesGroup) {
            undoStack.addLast(old)
            if (undoStack.size > limit) undoStack.removeFirst()
            redoStack.clear()
        }
        lastKind = kind
        present = next.clean()
        refresh()
    }

    fun undo(): TextFieldValue? {
        val prev = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(present)
        present = prev
        lastKind = Kind.NONE
        refresh()
        return prev
    }

    fun redo(): TextFieldValue? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(present)
        present = next
        lastKind = Kind.NONE
        refresh()
        return next
    }

    /** Klasifikasi dari selisih panjang dan kursor. Hanya memengaruhi pengelompokan, bukan keaslian teks. */
    private fun classify(oldLen: Int, next: TextFieldValue): Kind {
        val delta = next.text.length - oldLen
        val caret = next.selection.end
        return when {
            delta == 1 && next.selection.collapsed && caret >= 1 && !next.text[caret - 1].isWhitespace() -> Kind.TYPE
            delta == -1 -> Kind.DELETE
            else -> Kind.OTHER
        }
    }

    private fun refresh() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    private fun TextFieldValue.clean(): TextFieldValue = copy(composition = null)
}
