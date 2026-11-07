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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.userauth.UserAuthException

/**
 * Root sesi SFTP Wings (Pterodactyl) adalah folder server itu sendiri.
 * Path "/home/container" hanya ada di dalam container Docker, jadi tidak bisa dipakai di sini.
 */
private const val SERVER_ROOT = "/"
private const val DEFAULT_PORT = 2022
private const val CONNECT_TIMEOUT_MS = 15_000
private const val IO_TIMEOUT_MS = 30_000

/** Tahap koneksi, dipakai untuk menyusun pesan error yang jelas. */
private enum class Stage(val label: String) {
    CONNECT("Koneksi ke host"),
    AUTH("Login"),
    SUBSYSTEM("Membuka layanan SFTP"),
    FILES("Membaca folder server"),
}

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
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException(failure(Stage.CONNECT, e), e)
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
        var stage = Stage.CONNECT
        try {
            configure(ssh)
            ssh.addHostKeyVerifier(pinnedVerifier(pinnedFingerprint) { mismatch = true })
            ssh.connect(normalizeHost(host), portOf(port))
            stage = Stage.AUTH
            try {
                ssh.authPassword(loginUsername(username, serverId), password)
            } catch (e: UserAuthException) {
                throw IllegalStateException("Login SFTP ditolak. Periksa username panel (tanpa .idserver), password, dan ID server.", e)
            }
            stage = Stage.SUBSYSTEM
            ssh.newSFTPClient().use { client ->
                stage = Stage.FILES
                val count = client.ls(SERVER_ROOT).count { it.name != "." && it.name != ".." }
                "Login berhasil. $count item di folder server."
            }
        } catch (e: Exception) {
            when {
                mismatch -> throw IllegalStateException("Fingerprint server berubah. Koneksi dibatalkan.", e)
                e is IllegalStateException -> throw e
                else -> throw IllegalStateException(failure(stage, e), e)
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
            names.forEach { name -> deleteRecursive(c, child(dir, name)) }
        }
    }

    override suspend fun createFolder(serverId: String, dir: String, name: String) {
        withSftp(serverId) { c -> c.mkdir(child(dir, name)) }
    }

    override suspend fun rename(serverId: String, dir: String, from: String, to: String) {
        withSftp(serverId) { c -> c.rename(child(dir, from), child(dir, to)) }
    }

    /** Semua operasi file wajib memakai fingerprint yang sudah disetujui di Pengaturan. */
    /** Satu sesi SFTP dipakai ulang. [key] berisi host, port, user, server, fingerprint, dan hash password. */
    private class Session(val key: String, val ssh: SSHClient, val sftp: SFTPClient)

    private val sessionLock = Mutex()
    private var session: Session? = null

    /**
     * Semua operasi file lewat satu sesi bersama, dijalankan satu per satu.
     * Jika koneksi putus (biasanya karena idle), sesi dibuka ulang sekali lalu operasi dicoba lagi.
     */
    private suspend fun <T> withSftp(serverId: String, block: (SFTPClient) -> T): T = withContext(Dispatchers.IO) {
        val s = settings.observeSettings().first()
        require(s.sftpHost.isNotBlank() && s.sftpUser.isNotBlank() && s.sftpPassword.isNotBlank()) {
            "Host, username panel, dan password SFTP belum diisi di Pengaturan."
        }
        require(s.sftpHostKey.isNotBlank()) {
            "Fingerprint SFTP belum disetujui. Buka Pengaturan, bagian SFTP, lalu tekan Baca fingerprint."
        }
        val key = listOf(
            s.sftpHost.trim(), portOf(s.sftpPort), s.sftpUser.trim(), serverId, s.sftpHostKey, s.sftpPassword.hashCode(),
        ).joinToString("|")

        sessionLock.withLock {
            val current = session
            val active = if (current != null && current.key == key && current.ssh.isConnected && current.ssh.isAuthenticated) {
                current
            } else {
                closeSession()
                openSession(s.sftpHost, s.sftpPort, s.sftpUser, s.sftpPassword, s.sftpHostKey, serverId, key)
            }
            try {
                block(active.sftp)
            } catch (e: Exception) {
                if (e is IllegalStateException || e is IllegalArgumentException) throw e
                if (active.ssh.isConnected) throw IllegalStateException(failure(Stage.FILES, e), e)
                // Koneksi sudah putus: buka ulang sekali dan coba lagi.
                closeSession()
                val fresh = openSession(s.sftpHost, s.sftpPort, s.sftpUser, s.sftpPassword, s.sftpHostKey, serverId, key)
                try {
                    block(fresh.sftp)
                } catch (retryError: Exception) {
                    closeSession()
                    if (retryError is IllegalStateException || retryError is IllegalArgumentException) throw retryError
                    throw IllegalStateException(failure(Stage.FILES, retryError), retryError)
                }
            }
        }
    }

    private fun openSession(
        host: String,
        port: Int,
        user: String,
        password: String,
        pinned: String,
        serverId: String,
        key: String,
    ): Session {
        var mismatch = false
        var stage = Stage.CONNECT
        val ssh = SSHClient()
        try {
            configure(ssh)
            ssh.addHostKeyVerifier(pinnedVerifier(pinned) { mismatch = true })
            ssh.connect(normalizeHost(host), portOf(port))
            stage = Stage.AUTH
            try {
                ssh.authPassword(loginUsername(user, serverId), password)
            } catch (e: UserAuthException) {
                throw IllegalStateException("Login SFTP ditolak. Periksa username panel, password, dan ID server.", e)
            }
            stage = Stage.SUBSYSTEM
            val opened = Session(key, ssh, ssh.newSFTPClient())
            session = opened
            return opened
        } catch (e: Exception) {
            runCatching { ssh.disconnect() }
            when {
                mismatch -> throw IllegalStateException("Fingerprint server berubah. Koneksi dibatalkan demi keamanan.", e)
                e is IllegalStateException || e is IllegalArgumentException -> throw e
                else -> throw IllegalStateException(failure(stage, e), e)
            }
        }
    }

    private fun closeSession() {
        session?.let { old ->
            runCatching { old.sftp.close() }
            runCatching { old.ssh.disconnect() }
        }
        session = null
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

    /** Path absolut di server, dimulai dari SERVER_ROOT. Contoh: "/" + "logs" -> "/logs". */
    private fun full(path: String): String =
        if (path.isBlank() || path == "/") SERVER_ROOT else path.trimEnd('/')

    /** Path anak di dalam folder [dir], tanpa "//" berlebih. */
    private fun child(dir: String, name: String): String =
        full(dir).trimEnd('/') + "/" + name

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

    /** Pesan error yang menyebut tahap yang gagal. Pesan generik "failure" dari server diterjemahkan. */
    private fun failure(stage: Stage, e: Throwable): String {
        val raw = e.message?.trim().orEmpty()
        val detail = when {
            raw.isBlank() -> e::class.simpleName.orEmpty()
            raw.equals("failure", ignoreCase = true) ->
                "server menolak permintaan. Pastikan host adalah alamat node, port SFTP benar (biasanya 2022), dan ID server sesuai"
            else -> raw
        }
        return "${stage.label} gagal: $detail. Periksa host, port, jaringan, dan firewall."
    }
}
