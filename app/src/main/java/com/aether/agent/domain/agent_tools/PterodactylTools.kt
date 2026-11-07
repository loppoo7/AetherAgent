package com.aether.agent.domain.agent_tools

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

import com.aether.agent.domain.repository.PterodactylGateway
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject

private const val SERVER_HINT = "ID server Pterodactyl (identifier pendek)."

class PteroListFilesTool @Inject constructor(private val ptero: PterodactylGateway) : AgentTool {
    override val name = "server_list_files"
    override val description = "Daftar file dan folder di sebuah server Pterodactyl."
    override val isServerTool = true
    override val parameters = objectSchema(
        mapOf("server" to SERVER_HINT, "directory" to "Folder, contoh: /plugins"),
        listOf("server"),
    )
    override fun describe(args: JsonObject) = "Membaca folder ${args.str("directory").ifBlank { "/" }}"
    override suspend fun execute(args: JsonObject): String =
        ptero.listFiles(args.str("server"), args.str("directory").ifBlank { "/" })
            .joinToString("\n") { (if (it.isDir) "[dir] " else "") + it.name }
            .ifBlank { "Folder kosong." }
}

class PteroReadFileTool @Inject constructor(private val ptero: PterodactylGateway) : AgentTool {
    override val name = "server_read_file"
    override val description = "Baca isi file di server Pterodactyl."
    override val isServerTool = true
    override val parameters = objectSchema(
        mapOf("server" to SERVER_HINT, "path" to "Path file, contoh: /config.yml"),
        listOf("server", "path"),
    )
    override fun describe(args: JsonObject) = "Membaca ${args.str("path")}"
    override suspend fun execute(args: JsonObject): String {
        val text = ptero.readFile(args.str("server"), args.str("path"))
        return if (text.length > 12_000) text.take(12_000) + "\n...(terpotong)" else text
    }
}

class PteroWriteFileTool @Inject constructor(private val ptero: PterodactylGateway) : AgentTool {
    override val name = "server_write_file"
    override val description = "Tulis isi lengkap sebuah file di server Pterodactyl. Harus disetujui pengguna."
    override val isServerTool = true
    override val requiresApproval = true
    override val parameters = objectSchema(
        mapOf("server" to SERVER_HINT, "path" to "Path file", "content" to "Isi lengkap file"),
        listOf("server", "path", "content"),
    )
    override fun describe(args: JsonObject) = "Menulis ${args.str("path")}"
    override suspend fun preview(args: JsonObject): String {
        val content = args.str("content")
        val old = try {
            ptero.readFile(args.str("server"), args.str("path"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return "Peringatan: isi lama gagal dibaca (${e.message ?: "error"}). Jika file sudah ada, diff di bawah keliru.\n\n" +
                LineDiff.unified("", content)
        }
        return LineDiff.unified(old, content)
    }
    override suspend fun execute(args: JsonObject): String {
        ptero.writeFile(args.str("server"), args.str("path"), args.str("content"))
        return "Tersimpan: ${args.str("path")}"
    }
}

class PteroDeleteTool @Inject constructor(private val ptero: PterodactylGateway) : AgentTool {
    override val name = "server_delete"
    override val description = "Hapus file atau folder di server Pterodactyl. Harus disetujui pengguna."
    override val isServerTool = true
    override val requiresApproval = true
    override val parameters = objectSchema(
        mapOf(
            "server" to SERVER_HINT,
            "directory" to "Folder tempat item berada, contoh: /plugins",
            "names" to "Nama item dipisah koma, contoh: a.jar,b.jar. Atau JSON array, contoh: [\"a,b.txt\",\"c.jar\"]",
        ),
        listOf("server", "directory", "names"),
    )
    override fun describe(args: JsonObject) = "Menghapus ${args.str("names")}"
    override suspend fun preview(args: JsonObject): String {
        val dir = args.str("directory").ifBlank { "/" }
        val names = parseNames(args.str("names"))
        if (names.isEmpty()) return "Tidak ada nama item yang diberikan. Tidak ada yang dihapus."
        return "Hapus dari $dir:\n" + names.joinToString("\n") { "- $it" }
    }
    override suspend fun execute(args: JsonObject): String {
        val dir = args.str("directory").ifBlank { "/" }
        val names = parseNames(args.str("names"))
        if (names.isEmpty()) return "Tidak ada nama item yang diberikan. Tidak ada yang dihapus."
        ptero.deletePaths(args.str("server"), dir, names)
        return "Dihapus dari $dir: ${names.joinToString()}"
    }
}

/** Nama item dari teks biasa (dipisah koma) atau JSON array. Dipakai oleh preview dan eksekusi agar sama. */
private fun parseNames(raw: String): List<String> {
    val text = raw.trim()
    val items: List<String> = if (text.startsWith("[")) {
        runCatching { Json.parseToJsonElement(text).jsonArray.map { it.jsonPrimitive.content } }
            .getOrElse { text.split(",") }
    } else {
        text.split(",")
    }
    return items.map { it.trim() }.filter { it.isNotEmpty() }
}

class PteroPowerTool @Inject constructor(private val ptero: PterodactylGateway) : AgentTool {
    override val name = "server_power"
    override val description = "Kirim sinyal power ke server: start, stop, restart, atau kill. Harus disetujui pengguna."
    override val isServerTool = true
    override val requiresApproval = true
    override val parameters = objectSchema(
        mapOf("server" to SERVER_HINT, "signal" to "start, stop, restart, atau kill"),
        listOf("server", "signal"),
    )
    override fun describe(args: JsonObject) = "Power ${args.str("signal")}"
    override suspend fun preview(args: JsonObject): String =
        "Kirim sinyal '${args.str("signal")}' ke server ${args.str("server")}."
    override suspend fun execute(args: JsonObject): String {
        ptero.power(args.str("server"), args.str("signal"))
        return "Sinyal ${args.str("signal")} dikirim."
    }
}
