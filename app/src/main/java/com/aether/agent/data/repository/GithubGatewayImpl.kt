package com.aether.agent.data.repository

import com.aether.agent.domain.model.AgentSettings

import kotlinx.coroutines.CancellationException
import com.aether.agent.domain.agent_tools.LineDiff

import android.util.Base64
import com.aether.agent.data.api.github.GithubClient
import com.aether.agent.data.api.github.GithubConfig
import com.aether.agent.data.api.readableErrors
import com.aether.agent.domain.model.GithubRepo
import com.aether.agent.domain.repository.GithubGateway
import com.aether.agent.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Singleton
class GithubGatewayImpl @Inject constructor(
    private val client: GithubClient,
    private val settings: SettingsRepository,
) : GithubGateway {
    private suspend fun currentSettings() = settings.observeSettings().first()

    private suspend fun config(): GithubConfig {
        val s = currentSettings()
        require(s.githubOwner.isNotBlank() && s.githubRepo.isNotBlank()) {
            "Pilih setidaknya satu repository GitHub terlebih dahulu."
        }
        return GithubConfig(s.githubToken, s.githubOwner, s.githubRepo, s.githubBranch)
    }

    private suspend fun configForTarget(target: String): GithubConfig {
        val s = currentSettings()
        val fullName = target.substringBefore('|').trim()
        val owner = fullName.substringBefore('/')
        val repo = fullName.substringAfter('/', "")
        require(owner.isNotBlank() && repo.isNotBlank()) { "Target repository tidak valid: $target" }
        return GithubConfig(s.githubToken, owner, repo, target.substringAfter('|', "").ifBlank { s.githubBranch })
    }

    /** Target yang dipakai. Fallback ke owner/repo tunggal hanya kalau keduanya sudah terisi. */
    private fun targetsOf(s: AgentSettings): Set<String> {
        val explicit = s.githubTargets.filter { isValidTarget(it) }.toSet()
        if (explicit.isNotEmpty()) return explicit
        return if (s.githubOwner.isNotBlank() && s.githubRepo.isNotBlank())
            setOf("${s.githubOwner}/${s.githubRepo}|${s.githubBranch}") else emptySet()
    }

    private fun isValidTarget(t: String): Boolean {
        val full = t.substringBefore('|').trim()
        return full.substringBefore('/').isNotBlank() && full.substringAfter('/', "").isNotBlank()
    }

    private fun decodeFile(file: JsonObject?): String? {
        if (file == null) return null
        val encoded = file["content"]?.jsonPrimitive?.contentOrNull.orEmpty().replace("\n", "")
        return if (encoded.isBlank()) "" else Base64.decode(encoded, Base64.DEFAULT).toString(Charsets.UTF_8)
    }

    override suspend fun listRepos(): List<GithubRepo> = readableErrors {
        val token = currentSettings().githubToken
        require(token.isNotBlank()) { "Akun GitHub belum terhubung. Hubungkan GitHub melalui Device Flow terlebih dahulu." }
        val out = mutableListOf<GithubRepo>()
        for (page in 1..10) {
            val batch = client.userRepos(token, page)
            for (e in batch) {
                val o = e.jsonObject
                if (o["archived"]?.jsonPrimitive?.booleanOrNull == true) continue
                out += GithubRepo(
                    owner = o["owner"]?.jsonObject?.get("login")?.jsonPrimitive?.contentOrNull.orEmpty(),
                    name = o["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    isPrivate = o["private"]?.jsonPrimitive?.booleanOrNull == true,
                    description = o["description"]?.jsonPrimitive?.contentOrNull,
                    defaultBranch = o["default_branch"]?.jsonPrimitive?.contentOrNull ?: "main",
                    canPush = o["permissions"]?.jsonObject?.get("push")?.jsonPrimitive?.booleanOrNull != false,
                )
            }
            if (batch.size < 100) break
        }
        out
    }

    private suspend fun treeFiles(c: GithubConfig): List<String> {
        val tree = client.tree(c)
        val files = tree["tree"]?.jsonArray.orEmpty().mapNotNull { e ->
            val o = e.jsonObject
            if (o["type"]?.jsonPrimitive?.content == "blob") o["path"]?.jsonPrimitive?.content else null
        }
        return if (tree["truncated"]?.jsonPrimitive?.booleanOrNull == true)
            files + "(daftar tidak lengkap: repository terlalu besar, GitHub memotong hasilnya)"
        else files
    }

    override suspend fun listFiles(): List<String> = readableErrors {
        val s = currentSettings()
        val targets = targetsOf(s)
        val allFiles = mutableListOf<String>()
        for (target in targets.sorted()) {
            val label = target.substringBefore('|')
            allFiles += "## $label"
            allFiles += treeFiles(configForTarget(target))
        }
        allFiles
    }
    override suspend fun listFiles(repoTarget: String): List<String> = readableErrors { treeFiles(configForTarget(repoTarget)) }
    override suspend fun readFile(path: String): String? = readableErrors {
        val s = currentSettings()
        if (s.githubTargets.size > 1) readFileAcrossTargets(path) else decodeFile(client.file(config(), path))
    }
    override suspend fun readFile(repoTarget: String, path: String): String? =
        readableErrors { decodeFile(client.file(configForTarget(repoTarget), path)) }

    override suspend fun readFileAcrossTargets(path: String): String = readableErrors {
        val s = currentSettings()
        val targets = targetsOf(s)
        val sections = mutableListOf<String>()
        for (target in targets.sorted()) {
            val label = target.substringBefore('|')
            val content = readFile(target, path) ?: "(file tidak ditemukan)"
            sections += "===== $label · $path =====\n$content"
        }
        sections.joinToString("\n\n")
    }

    override suspend fun writeFile(path: String, content: String, message: String): String = readableErrors {
        val s = currentSettings()
        val targets = targetsOf(s)
        require(targets.isNotEmpty()) { "Pilih repository GitHub terlebih dahulu." }
        val done = mutableListOf<String>()
        val failed = mutableListOf<String>()
        for (target in targets.sorted()) {
            val c = configForTarget(target)
            val label = "${c.owner}/${c.repo}"
            try {
                val sha = client.file(c, path)?.get("sha")?.jsonPrimitive?.content
                val result = client.putFile(c, path, content, message, sha)
                val url = result["content"]?.jsonObject?.get("html_url")?.jsonPrimitive?.content.orEmpty()
                done += "$label: $url"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed += "$label: ${e.message ?: e::class.simpleName}"
            }
        }
        outcome("Perubahan berhasil dikirim ke", done, failed)
    }

    override suspend fun deleteFile(path: String, message: String): String = readableErrors {
        val s = currentSettings()
        val targets = targetsOf(s)
        require(targets.isNotEmpty()) { "Pilih repository GitHub terlebih dahulu." }
        val done = mutableListOf<String>()
        val failed = mutableListOf<String>()
        for (target in targets.sorted()) {
            val c = configForTarget(target)
            val label = "${c.owner}/${c.repo}"
            try {
                val sha = client.file(c, path)?.get("sha")?.jsonPrimitive?.content
                    ?: error("File $path tidak ditemukan.")
                client.deleteFile(c, path, message, sha)
                done += label
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed += "$label: ${e.message ?: e::class.simpleName}"
            }
        }
        outcome("Penghapusan berhasil di", done, failed)
    }

    override suspend fun previewChange(path: String, newContent: String): String = readableErrors {
        val s = currentSettings()
        val targets = targetsOf(s)
        val sections = mutableListOf<String>()
        for (target in targets.sorted()) {
            val c = configForTarget(target)
            val label = "${c.owner}/${c.repo}"
            var old = ""
            var warning: String? = null
            try {
                old = decodeFile(client.file(c, path)).orEmpty()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                warning = e.message ?: "error"
            }
            sections += buildString {
                append("=== $label · $path ===\n")
                if (warning != null) {
                    append("(peringatan: gagal membaca isi lama: $warning. Diff di bawah bisa menampilkan seluruh isi sebagai baru.)\n")
                }
                append(LineDiff.unified(old, newContent))
            }
        }
        sections.joinToString("\n\n")
    }

    /** Ringkasan per repo. Kalau ada yang gagal, error menyebut mana yang sudah berubah dan mana yang gagal. */
    private fun outcome(successPrefix: String, done: List<String>, failed: List<String>): String {
        if (failed.isEmpty()) return "$successPrefix ${done.size} repository:\n" + done.joinToString("\n")
        error(
            "Operasi hanya sebagian. Sudah berubah: " +
                (if (done.isEmpty()) "tidak ada" else done.joinToString("; ")) +
                ". Gagal: " + failed.joinToString("; ")
        )
    }

    override suspend fun createBranch(name: String, from: String): String = readableErrors {
        val c = config()
        client.createBranch(c, name, client.branchSha(c, from))
        "Branch $name dibuat dari $from"
    }

    override suspend fun createPullRequest(title: String, head: String, base: String, body: String): String =
        readableErrors {
            val c = config()
            client.createPull(c, title, head, base, body)["html_url"]?.jsonPrimitive?.content.orEmpty()
        }

    override suspend fun listCommits(limit: Int): List<String> = readableErrors {
        val c = config()
        client.commits(c, limit).map { e ->
            val o = e.jsonObject
            val sha = o["sha"]?.jsonPrimitive?.content.orEmpty().take(7)
            val msg = o["commit"]?.jsonObject?.get("message")?.jsonPrimitive?.content.orEmpty().lineSequence().firstOrNull().orEmpty()
            "$sha $msg"
        }
    }

    override suspend fun listIssues(limit: Int): List<String> = readableErrors {
        val c = config()
        client.issues(c, limit).map { e ->
            val o = e.jsonObject
            "#${o["number"]?.jsonPrimitive?.content} ${o["title"]?.jsonPrimitive?.content.orEmpty()}"
        }
    }
}
