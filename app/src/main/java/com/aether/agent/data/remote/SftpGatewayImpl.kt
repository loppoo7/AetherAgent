package com.aether.agent.data.remote

import com.aether.agent.domain.model.PteroFile
import com.aether.agent.domain.repository.SettingsRepository
import com.aether.agent.domain.repository.SftpGateway
import java.security.PublicKey
import java.util.EnumSet
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.HostKeyVerifier

/** Folder server di node Pterodactyl. Login SFTP memakai "username.idserver". */
private const val SERVER_ROOT = "/home/container"
private const val CONNECT_TIMEOUT_MS = 15_000
private const val IO_TIMEOUT_MS = 30_000

@Singleton
class SftpGatewayImpl @Inject constructor(
    private val settings: SettingsRepository,
) : SftpGateway {

    /**
     * Buka koneksi SFTP baru untuk setiap operasi (cukup cepat untuk pemakaian manual).
     * Fingerprint host dicatat saat pertama konek; jika berubah, koneksi ditolak.
     */
    private suspend fun <T> withSftp(serverId: String, block: (SFTPClient) -> T): T = withContext(Dispatchers.IO) {
        val s = settings.observeSettings().first()
        require(s.sftpHost.isNotBlank() && s.sftpUser.isNotBlank() && s.sftpPassword.isNotBlank()) {
            "Host, username panel, dan password SFTP belum diisi di Pengaturan."
        }
        val pinned = s.sftpHostKey
        var learned: String? = null
        var mismatch = false
        val ssh = SSHClient()
        try {
            ssh.connectTimeout = CONNECT_TIMEOUT_MS
            ssh.timeout = IO_TIMEOUT_MS
            ssh.addHostKeyVerifier(object : HostKeyVerifier {
                override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
                    val fp = SecurityUtils.getFingerprint(key)
                    return when {
                        pinned.isBlank() -> {
                            learned = fp
                            true
                        }
                        pinned == fp -> true
                        else -> {
                            mismatch = true
                            false
                        }
                    }
                }

                override fun findExistingAlgorithms(hostname: String, port: Int): List<String> = emptyList()
            })
            ssh.connect(s.sftpHost.trim(), s.sftpPort)
            ssh.authPassword("${s.sftpUser.trim()}.$serverId", s.sftpPassword)
            learned?.let { settings.update(s.copy(sftpHostKey = it)) }
            ssh.newSFTPClient().use { client -> block(client) }
        } catch (e: Exception) {
            if (mismatch) {
                throw IllegalStateException(
                    "Fingerprint server berubah. Tekan 'Lupakan' di Pengaturan jika kamu yakin server itu aman.",
                    e,
                )
            }
            throw e
        } finally {
            runCatching { ssh.disconnect() }
        }
    }

    private fun full(path: String): String =
        SERVER_ROOT + if (path == "/" || path.isBlank()) "" else path.trimEnd('/')

    override suspend fun listFiles(serverId: String, dir: String): List<PteroFile> =
        withSftp(serverId) { c ->
            c.ls(full(dir))
                .filter { it.name != "." && it.name != ".." }
                .map { info -> PteroFile(info.name, info.isDirectory, info.attributes.size) }
                .sortedWith(compareByDescending<PteroFile> { it.isDir }.thenBy { it.name.lowercase() })
        }

    override suspend fun readFile(serverId: String, path: String): String =
        withSftp(serverId) { c ->
            c.open(full(path), EnumSet.of(OpenMode.READ)).use { f ->
                f.RemoteFileInputStream().use { it.readBytes().toString(Charsets.UTF_8) }
            }
        }

    override suspend fun writeFile(serverId: String, path: String, content: String) {
        withSftp(serverId) { c ->
            c.open(full(path), EnumSet.of(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)).use { f ->
                f.RemoteFileOutputStream().use { it.write(content.toByteArray(Charsets.UTF_8)) }
            }
        }
    }

    override suspend fun deletePaths(serverId: String, dir: String, names: List<String>) {
        withSftp(serverId) { c ->
            names.forEach { name -> deleteRecursive(c, full(dir) + "/" + name) }
        }
    }

    private fun deleteRecursive(c: SFTPClient, path: String) {
        if (c.stat(path).type == FileMode.Type.DIRECTORY) {
            c.ls(path)
                .filter { it.name != "." && it.name != ".." }
                .forEach { deleteRecursive(c, path + "/" + it.name) }
            c.rmdir(path)
        } else {
            c.rm(path)
        }
    }

    override suspend fun createFolder(serverId: String, dir: String, name: String) {
        withSftp(serverId) { c -> c.mkdir(full(dir) + "/" + name) }
    }

    override suspend fun rename(serverId: String, dir: String, from: String, to: String) {
        withSftp(serverId) { c -> c.rename(full(dir) + "/" + from, full(dir) + "/" + to) }
    }
}
