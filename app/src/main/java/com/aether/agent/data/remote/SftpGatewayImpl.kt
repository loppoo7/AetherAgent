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
     * Tahap pertama hanya mengambil fingerprint saat key belum dikenal.
     * Verifier sengaja menolak host yang belum dipercaya sehingga kredensial tidak
     * pernah dikirim sebelum pengguna menyetujui fingerprint tersebut.
     */
    override suspend fun inspectHostKey(host: String, port: Int, knownHostKey: String): String =
        withContext(Dispatchers.IO) {
            require(host.isNotBlank()) { "Host SFTP belum diisi." }
            require(port in 1..65535) { "Port SFTP harus antara 1 dan 65535." }

            val pinned = knownHostKey.trim()
            var observed: String? = null
            var mismatch = false
            val ssh = SSHClient()
            try {
                ssh.connectTimeout = CONNECT_TIMEOUT_MS
                ssh.timeout = IO_TIMEOUT_MS
                ssh.addHostKeyVerifier(object : HostKeyVerifier {
                    override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
                        val fingerprint = SecurityUtils.getFingerprint(key)
                        observed = fingerprint
                        if (pinned.isBlank()) return false
                        mismatch = fingerprint != pinned
                        return !mismatch
                    }

                    override fun findExistingAlgorithms(hostname: String, port: Int): List<String> = emptyList()
                })
                ssh.connect(normalizeSftpHost(host), port)
                observed ?: pinned
            } catch (e: Exception) {
                when {
                    pinned.isBlank() && !observed.isNullOrBlank() -> observed!!
                    mismatch -> throw IllegalStateException(
                        "Fingerprint host SFTP berubah. Jangan lanjutkan sebelum memastikan perubahan ini aman.",
                        e,
                    )
                    else -> throw IllegalStateException(
                        "Tidak dapat membaca fingerprint SFTP. Periksa alamat host, port, jaringan, dan firewall.",
                        e,
                    )
                }
            } finally {
                runCatching { ssh.disconnect() }
            }
        }

    /** Verifikasi fingerprint yang sudah disetujui, lalu baru kirim kredensial. */
    override suspend fun testConnection(
        serverId: String,
        host: String,
        port: Int,
        username: String,
        password: String,
        knownHostKey: String,
    ): Unit = withContext(Dispatchers.IO) {
        require(serverId.isNotBlank()) { "ID server tidak ditemukan dari Client API." }
        require(host.isNotBlank()) { "Host SFTP belum diisi." }
        require(port in 1..65535) { "Port SFTP harus antara 1 dan 65535." }
        require(username.isNotBlank() && password.isNotBlank()) {
            "Username dan password SFTP harus diisi."
        }
        require(knownHostKey.isNotBlank()) {
            "Fingerprint belum diverifikasi. Tekan Connect untuk membaca fingerprint terlebih dahulu."
        }

        var mismatch = false
        var connected = false
        var authenticated = false
        val ssh = SSHClient()
        try {
            ssh.connectTimeout = CONNECT_TIMEOUT_MS
            ssh.timeout = IO_TIMEOUT_MS
            ssh.addHostKeyVerifier(object : HostKeyVerifier {
                override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
                    val fingerprint = SecurityUtils.getFingerprint(key)
                    mismatch = fingerprint != knownHostKey
                    return !mismatch
                }

                override fun findExistingAlgorithms(hostname: String, port: Int): List<String> = emptyList()
            })
            ssh.connect(host.trim(), port)
            connected = true
            ssh.authPassword("${username.trim()}.$serverId", password)
            authenticated = true
            ssh.newSFTPClient().use { client -> client.ls(SERVER_ROOT) }
        } catch (e: Exception) {
            when {
                mismatch -> throw IllegalStateException(
                    "Fingerprint host berubah sejak disetujui. Koneksi dibatalkan demi keamanan.",
                    e,
                )
                !connected -> throw IllegalStateException(
                    "Koneksi ke host SFTP gagal. Periksa host, port, jaringan, dan firewall.",
                    e,
                )
                !authenticated -> throw IllegalStateException(
                    "Login SFTP ditolak. Periksa password dan format username.idserver.",
                    e,
                )
                else -> throw IllegalStateException(
                    "Login berhasil, tetapi folder server tidak dapat diakses: ${e.message ?: "izin ditolak"}.",
                    e,
                )
            }
        } finally {
            runCatching { ssh.disconnect() }
        }
    }

    /**
     * Operasi file hanya boleh berjalan memakai fingerprint yang sudah disimpan
     * setelah persetujuan eksplisit dari pengguna.
     */
    private suspend fun <T> withSftp(serverId: String, block: (SFTPClient) -> T): T =
        withContext(Dispatchers.IO) {
            val s = settings.observeSettings().first()
            require(s.sftpHost.isNotBlank() && s.sftpUser.isNotBlank() && s.sftpPassword.isNotBlank()) {
                "Host, username panel, dan password SFTP belum diisi di Pengaturan."
            }
            require(s.sftpHostKey.isNotBlank()) {
                "Fingerprint SFTP belum diverifikasi. Buka Pengaturan > Server lalu tekan Connect."
            }

            var mismatch = false
            val ssh = SSHClient()
            try {
                ssh.connectTimeout = CONNECT_TIMEOUT_MS
                ssh.timeout = IO_TIMEOUT_MS
                ssh.addHostKeyVerifier(object : HostKeyVerifier {
                    override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
                        val fingerprint = SecurityUtils.getFingerprint(key)
                        mismatch = fingerprint != s.sftpHostKey
                        return !mismatch
                    }

                    override fun findExistingAlgorithms(hostname: String, port: Int): List<String> = emptyList()
                })
                ssh.connect(normalizeSftpHost(s.sftpHost), s.sftpPort)
                ssh.authPassword("${s.sftpUser.trim()}.$serverId", s.sftpPassword)
                ssh.newSFTPClient().use { client -> block(client) }
            } catch (e: Exception) {
                if (mismatch) {
                    throw IllegalStateException(
                        "Fingerprint server berubah. Koneksi ditolak; periksa host sebelum memperbarui fingerprint.",
                        e,
                    )
                }
                throw e
            } finally {
                runCatching { ssh.disconnect() }
            }
        }

    /** Pengaturan menerima URL node (https://...), sedangkan SSH membutuhkan hostname saja. */
    private fun normalizeSftpHost(value: String): String {
        val input = value.trim()
        val uri = runCatching {
            java.net.URI(if (input.contains("://")) input else "ssh://$input")
        }.getOrNull()
        return uri?.host?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Host SFTP tidak valid. Masukkan alamat node tanpa path.")
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
