package com.aether.agent.domain.agent_tools

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
        val old = runCatching { ptero.readFile(args.str("server"), args.str("path")) }.getOrDefault("")
        return LineDiff.unified(old, args.str("content"))
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
            "names" to "Nama item dipisah koma, contoh: a.jar,b.jar",
        ),
        listOf("server", "directory", "names"),
    )
    override fun describe(args: JsonObject) = "Menghapus ${args.str("names")}"
    override suspend fun preview(args: JsonObject): String =
        "Hapus dari ${args.str("directory")}:\n" +
            args.str("names").split(",").joinToString("\n") { "- ${it.trim()}" }
    override suspend fun execute(args: JsonObject): String {
        val names = args.str("names").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        ptero.deletePaths(args.str("server"), args.str("directory").ifBlank { "/" }, names)
        return "Dihapus: ${names.joinToString()}"
    }
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
