package com.aether.agent.domain.agent_tools

/** Diff baris sederhana berbasis LCS. Hanya menampilkan baris yang berubah. */
object LineDiff {
    private const val MAX_CELLS = 1_000_000L
    private const val MAX_LINES = 200

    fun unified(oldText: String, newText: String): String {
        if (oldText == newText) return ""
        val a = if (oldText.isEmpty()) emptyList() else oldText.lines()
        val b = if (newText.isEmpty()) emptyList() else newText.lines()
        if (a.size.toLong() * b.size > MAX_CELLS) {
            return "- ${a.size} baris lama\n+ ${b.size} baris baru (diff terlalu besar untuk ditampilkan)"
        }
        val n = a.size
        val m = b.size
        val lcs = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                lcs[i][j] = if (a[i] == b[j]) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
            }
        }
        val out = ArrayList<String>()
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                a[i] == b[j] -> { i++; j++ }
                lcs[i + 1][j] >= lcs[i][j + 1] -> { out += "- ${a[i]}"; i++ }
                else -> { out += "+ ${b[j]}"; j++ }
            }
        }
        while (i < n) { out += "- ${a[i]}"; i++ }
        while (j < m) { out += "+ ${b[j]}"; j++ }
        val shown = out.take(MAX_LINES).joinToString("\n")
        return if (out.size > MAX_LINES) "$shown\n... (${out.size - MAX_LINES} baris lagi)" else shown
    }
}
