package com.aether.agent.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

private val KeywordColor = Color(0xFF7DD3FC)
private val StringColor = Color(0xFF9FE870)
private val CommentColor = Color(0xFF6B7280)
private val NumberColor = Color(0xFFFDBA74)
private val KeyColor = Color(0xFFC4B5FD)
private val MatchColor = Color(0x55FACC15)
private val CurrentMatchColor = Color(0xAAFACC15)

private val keywordRegex = Regex(
    "\\b(fun|val|var|class|object|interface|import|package|return|if|else|when|for|while|true|false|null|" +
        "def|let|const|async|await|from|export|function|public|private|static|void|using|namespace|enabled|disabled)\\b",
)
private val stringRegex = Regex("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'")
private val commentRegex = Regex("(//|#).*")
private val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
private val keyRegex = Regex("(?m)^\\s*([A-Za-z_][\\w.-]*)\\s*:")

/** Highlight sederhana untuk kode dan konfigurasi (JSON, YAML, properties, Kotlin, dll). */
fun highlightCode(text: String, matches: List<IntRange> = emptyList(), current: Int = -1): AnnotatedString =
    buildAnnotatedString {
        append(text)
        fun paint(regex: Regex, color: Color, group: Int = 0) {
            regex.findAll(text).forEach { m ->
                val g = m.groups[group] ?: m.groups[0] ?: return@forEach
                addStyle(SpanStyle(color = color), g.range.first, g.range.last + 1)
            }
        }
        paint(numberRegex, NumberColor)
        paint(keyRegex, KeyColor, 1)
        paint(keywordRegex, KeywordColor)
        paint(stringRegex, StringColor)
        paint(commentRegex, CommentColor)
        matches.forEachIndexed { i, r ->
            addStyle(
                SpanStyle(background = if (i == current) CurrentMatchColor else MatchColor),
                r.first,
                r.last + 1,
            )
        }
    }

/** Cari semua kemunculan [query] (tidak peka huruf besar-kecil). */
fun findMatches(text: String, query: String): List<IntRange> {
    if (query.isEmpty()) return emptyList()
    val out = ArrayList<IntRange>()
    var from = 0
    while (true) {
        val idx = text.indexOf(query, from, ignoreCase = true)
        if (idx < 0) break
        out += idx until idx + query.length
        from = idx + query.length
    }
    return out
}
