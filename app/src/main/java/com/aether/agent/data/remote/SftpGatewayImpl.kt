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
import net.schmizz.sshj.userauth.UserAuthException

/** Folder server di node Pterodactyl. Login SFTP memakai "username.idserver". */
private const val SERVER_ROOT = "/home/container"
private const val DEFAULT_PORT = 2022
private const val CONNECT_TIMEOUT_MS = 15_000
private const val IO_TIMEOUT_MS = 30_000

@Singleton
class SftpGatewayImpl @Inject constructor(
    private val settings: SettingsRepository,
) : SftpGateway {

    /** Hanya membaca fingerprint. Verifier menerima host ini karena tidak ada kredensial yang dikirim. */
    override suspend fun fetchFingerprint(host: String, port: Int): String = withContext(Dispatchers.IO) {
        var observed: String? = null
        val ssh = SSHClient()
        try {
            configure(ssh)
            ssh.addHostKeyVerifier(object : HostKeyVerifier {
                override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
                    observed = SecurityUtils.getFingerprint(key)
                    return true
                }

                override fun findExistingAlgorithms(hostname: String, port: Int): List<String> = emptyList()
            })
            ssh.connect(normalizeHost(host), portOf(port))
            observed ?: throw IllegalStateException("Fingerprint host tidak terbaca.")
        } catch (e: Exception) {
            throw IllegalStateException(connectFailure(e), e)
        } finally {
            runCatching { ssh.disconnect() }
        }
    }

    override suspend fun checkLogin(
        host: String,
        port: Int,
        username: String,
        password: String,
        serverId: String,
        pinnedFingerprint: String,
    ): String = withContext(Dispatchers.IO) {
        require(pinnedFingerprint.isNotBlank()) { "Fingerprint belum disetujui." }
        var mismatch = false
        val ssh = SSHClient()
        try {
            configure(ssh)
            ssh.addHostKeyVerifier(pinnedVerifier(pinnedFingerprint) { mismatch = true })
            ssh.connect(normalizeHost(host), portOf(port))
            try {
                ssh.authPassword(loginUsername(username, serverId), password)
            } catch (e: UserAuthException) {
                throw IllegalStateException("Login SFTP ditolak. Periksa username panel (tanpa .idserver) dan password.", e)
            }
            ssh.newSFTPClient().use { client ->
                val count = client.ls(SERVER_ROOT).count { it.name != "." && it.name != ".." }
                "Login berhasil. $count item di folder server."
            }
        } catch (e: Exception) {
            when {
                mismatch -> throw IllegalStateException("Fingerprint server berubah. Koneksi dibatalkan.", e)
                e is IllegalStateException -> throw e
                else -> throw IllegalStateException(connectFailure(e), e)
            }
        } finally {
            runCatching { ssh.disconnect() }
        }
    }

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

    override suspend fun createFolder(serverId: String, dir: String, name: String) {
        withSftp(serverId) { c -> c.mkdir(full(dir) + "/" + name) }
    }

    override suspend fun rename(serverId: String, dir: String, from: String, to: String) {
        withSftp(serverId) { c -> c.rename(full(dir) + "/" + from, full(dir) + "/" + to) }
    }

    /** Semua operasi file wajib memakai fingerprint yang sudah disetujui di Pengaturan. */
    private suspend fun <T> withSftp(serverId: String, block: (SFTPClient) -> T): T = withContext(Dispatchers.IO) {
        val s = settings.observeSettings().first()
        require(s.sftpHost.isNotBlank() && s.sftpUser.isNotBlank() && s.sftpPassword.isNotBlank()) {
            "Host, username panel, dan password SFTP belum diisi di Pengaturan."
        }
        require(s.sftpHostKey.isNotBlank()) {
            "Fingerprint SFTP belum disetujui. Buka Pengaturan, bagian Server, lalu tekan Connect SFTP."
        }
        var mismatch = false
        val ssh = SSHClient()
        try {
            configure(ssh)
            ssh.addHostKeyVerifier(pinnedVerifier(s.sftpHostKey) { mismatch = true })
            ssh.connect(normalizeHost(s.sftpHost), portOf(s.sftpPort))
            try {
                ssh.authPassword(loginUsername(s.sftpUser, serverId), s.sftpPassword)
            } catch (e: UserAuthException) {
                throw IllegalStateException("Login SFTP ditolak. Periksa username panel dan password.", e)
            }
            ssh.newSFTPClient().use { client -> block(client) }
        } catch (e: Exception) {
            when {
                mismatch -> throw IllegalStateException("Fingerprint server berubah. Koneksi dibatalkan demi keamanan.", e)
                e is IllegalStateException -> throw e
                else -> throw IllegalStateException(connectFailure(e), e)
            }
        } finally {
            runCatching { ssh.disconnect() }
        }
    }

    private fun configure(ssh: SSHClient) {
        ssh.connectTimeout = CONNECT_TIMEOUT_MS
        ssh.timeout = IO_TIMEOUT_MS
    }

    private fun pinnedVerifier(pinned: String, onMismatch: () -> Unit): HostKeyVerifier =
        object : HostKeyVerifier {
            override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
                val ok = SecurityUtils.getFingerprint(key) == pinned
                if (!ok) onMismatch()
                return ok
            }

            override fun findExistingAlgorithms(hostname: String, port: Int): List<String> = emptyList()
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

    private fun full(path: String): String =
        SERVER_ROOT + if (path == "/" || path.isBlank()) "" else path.trimEnd('/')

    private fun portOf(port: Int): Int = if (port in 1..65535) port else DEFAULT_PORT

    /** Terima URL node (https://...) atau hostname saja. */
    private fun normalizeHost(value: String): String {
        val input = value.trim()
        val uri = runCatching {
            java.net.URI(if (input.contains("://")) input else "ssh://$input")
        }.getOrNull()
        return uri?.host?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Host SFTP tidak valid. Masukkan alamat node.")
    }

    /** Terima "user" maupun "user.idserver", hasilnya selalu "user.idserver". */
    private fun loginUsername(username: String, serverId: String): String {
        val base = username.trim().removeSuffix(".$serverId")
        return "$base.$serverId"
    }

    private fun connectFailure(e: Throwable): String =
        "Koneksi ke host SFTP gagal: ${e.message ?: e::class.simpleName}. Periksa host, port, jaringan, dan firewall."
}
