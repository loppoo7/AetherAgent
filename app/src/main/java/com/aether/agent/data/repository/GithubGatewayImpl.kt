package com.aether.agent.data.repository

import android.util.Base64
import com.aether.agent.data.api.github.GithubClient
import com.aether.agent.data.api.github.GithubConfig
import com.aether.agent.data.api.readableErrors
import com.aether.agent.domain.repository.GithubGateway
import com.aether.agent.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Singleton
class GithubGatewayImpl @Inject constructor(
    private val client: GithubClient,
    private val settings: SettingsRepository,
) : GithubGateway {

    private suspend fun config(): GithubConfig {
        val s = settings.observeSettings().first()
        require(s.githubOwner.isNotBlank() && s.githubRepo.isNotBlank()) {
            "Owner dan nama repo GitHub belum diatur di Pengaturan."
        }
        return GithubConfig(s.githubToken, s.githubOwner, s.githubRepo, s.githubBranch)
    }

    override suspend fun listFiles(): List<String> = readableErrors {
        val c = config()
        client.tree(c)["tree"]?.jsonArray.orEmpty().mapNotNull { e ->
            val o = e.jsonObject
            if (o["type"]?.jsonPrimitive?.content == "blob") o["path"]?.jsonPrimitive?.content else null
        }
    }

    override suspend fun readFile(path: String): String? = readableErrors {
        val c = config()
        val f = client.file(c, path) ?: return@readableErrors null
        val b64 = f["content"]?.jsonPrimitive?.content.orEmpty().replace("\n", "")
        Base64.decode(b64, Base64.DEFAULT).toString(Charsets.UTF_8)
    }

    override suspend fun writeFile(path: String, content: String, message: String): String = readableErrors {
        val c = config()
        val sha = client.file(c, path)?.get("sha")?.jsonPrimitive?.content
        val res = client.putFile(c, path, content, message, sha)
        res["content"]?.jsonObject?.get("html_url")?.jsonPrimitive?.content.orEmpty()
    }

    override suspend fun deleteFile(path: String, message: String): String = readableErrors {
        val c = config()
        val sha = client.file(c, path)?.get("sha")?.jsonPrimitive?.content
            ?: error("File $path tidak ditemukan.")
        client.deleteFile(c, path, message, sha)
        "Dihapus: $path"
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
            val msg = o["commit"]?.jsonObject?.get("message")?.jsonPrimitive?.content.orEmpty()
                .lineSequence().firstOrNull().orEmpty()
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
