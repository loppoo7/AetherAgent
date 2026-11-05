package com.aether.agent.domain.agent_tools

import com.aether.agent.domain.repository.GithubGateway
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject

class ListFilesTool @Inject constructor(private val github: GithubGateway) : AgentTool {
    override val name = "github_list_files"
    override val description = "Tampilkan daftar file dari semua repository target yang dipilih."
    override val parameters = objectSchema()
    override fun describe(args: JsonObject) = "Membaca struktur repo"
    override suspend fun execute(args: JsonObject): String {
        val files = github.listFiles()
        if (files.isEmpty()) return "Repo kosong."
        val shown = files.take(400).joinToString("\n")
        return if (files.size > 400) "$shown\n... (${files.size - 400} file lagi)" else shown
    }
}

class ReadFileTool @Inject constructor(private val github: GithubGateway) : AgentTool {
    override val name = "github_read_file"
    override val description = "Baca isi file dari repository target yang dipilih; jika beberapa repo dipilih, tampilkan tiap versinya."
    override val parameters = objectSchema(mapOf("path" to "Path file, contoh: app/src/Main.kt"), listOf("path"))
    override fun describe(args: JsonObject) = "Membaca ${args.str("path")}"
    override suspend fun execute(args: JsonObject): String {
        val text = github.readFile(args.str("path")) ?: return "File tidak ditemukan."
        return if (text.length > MAX_CHARS) text.take(MAX_CHARS) + "\n...(terpotong)" else text
    }
    companion object { const val MAX_CHARS = 12_000 }
}

class WriteFileTool @Inject constructor(private val github: GithubGateway) : AgentTool {
    override val name = "github_write_file"
    override val description = "Buat file baru atau ubah file yang ada, lalu commit ke branch aktif. Isi harus lengkap, bukan potongan."
    override val parameters = objectSchema(
        mapOf(
            "path" to "Path file",
            "content" to "Isi lengkap file",
            "message" to "Pesan commit",
        ),
        listOf("path", "content", "message"),
    )
    override val requiresApproval = true
    override fun describe(args: JsonObject) = "Menulis ${args.str("path")}"
    override suspend fun preview(args: JsonObject): String =
        LineDiff.unified(github.readFileAcrossTargets(args.str("path")), args.str("content"))
    override suspend fun execute(args: JsonObject): String {
        val path = args.str("path")
        val message = args.str("message").ifBlank { "Update $path" }
        return "Tersimpan: " + github.writeFile(path, args.str("content"), message)
    }
}

class DeleteFileTool @Inject constructor(private val github: GithubGateway) : AgentTool {
    override val name = "github_delete_file"
    override val description = "Hapus sebuah file dari branch aktif lalu commit."
    override val parameters = objectSchema(
        mapOf("path" to "Path file yang dihapus", "message" to "Pesan commit"),
        listOf("path", "message"),
    )
    override val requiresApproval = true
    override fun describe(args: JsonObject) = "Menghapus ${args.str("path")}"
    override suspend fun preview(args: JsonObject): String =
        LineDiff.unified(github.readFileAcrossTargets(args.str("path")), "")
    override suspend fun execute(args: JsonObject): String =
        github.deleteFile(args.str("path"), args.str("message").ifBlank { "Hapus ${args.str("path")}" })
}

class CreateBranchTool @Inject constructor(private val github: GithubGateway) : AgentTool {
    override val name = "github_create_branch"
    override val description = "Buat branch baru dari branch lain (default main)."
    override val parameters = objectSchema(
        mapOf("name" to "Nama branch baru", "from" to "Branch sumber, default main"),
        listOf("name"),
    )
    override fun describe(args: JsonObject) = "Membuat branch ${args.str("name")}"
    override suspend fun execute(args: JsonObject): String =
        github.createBranch(args.str("name"), args.str("from").ifBlank { "main" })
}

class CreatePullRequestTool @Inject constructor(private val github: GithubGateway) : AgentTool {
    override val name = "github_create_pull_request"
    override val description = "Buat pull request dari branch head ke branch base."
    override val parameters = objectSchema(
        mapOf(
            "title" to "Judul PR",
            "head" to "Branch sumber (berisi perubahan)",
            "base" to "Branch tujuan, default main",
            "body" to "Deskripsi PR",
        ),
        listOf("title", "head"),
    )
    override val requiresApproval = true
    override fun describe(args: JsonObject) = "Membuat PR: ${args.str("title")}"
    override suspend fun preview(args: JsonObject): String =
        "Dari ${args.str("head")} ke ${args.str("base").ifBlank { "main" }}\n\n${args.str("body")}"
    override suspend fun execute(args: JsonObject): String =
        github.createPullRequest(
            args.str("title"),
            args.str("head"),
            args.str("base").ifBlank { "main" },
            args.str("body"),
        )
}

class ListCommitsTool @Inject constructor(private val github: GithubGateway) : AgentTool {
    override val name = "github_list_commits"
    override val description = "Ambil riwayat commit terbaru di branch aktif."
    override val parameters = objectSchema(mapOf("limit" to "Jumlah commit, default 10"))
    override fun describe(args: JsonObject) = "Membaca riwayat commit"
    override suspend fun execute(args: JsonObject): String =
        github.listCommits(args.str("limit").toIntOrNull() ?: 10).joinToString("\n").ifBlank { "Belum ada commit." }
}

class ListIssuesTool @Inject constructor(private val github: GithubGateway) : AgentTool {
    override val name = "github_list_issues"
    override val description = "Ambil daftar issue dan pull request yang masih terbuka."
    override val parameters = objectSchema(mapOf("limit" to "Jumlah item, default 10"))
    override fun describe(args: JsonObject) = "Membaca issue terbuka"
    override suspend fun execute(args: JsonObject): String =
        github.listIssues(args.str("limit").toIntOrNull() ?: 10).joinToString("\n").ifBlank { "Tidak ada issue terbuka." }
}
