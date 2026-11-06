package com.aether.agent

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.security.Security
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import net.schmizz.sshj.common.SecurityUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider

@HiltAndroidApp
class AetherApp : Application() {

    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        startFreezeWatchdog()

        // SSHJ needs the newer BC algorithms, but BC must not outrank Android's
        // Conscrypt/AndroidOpenSSL provider. Putting BC at position 1 can break
        // the system's default TLS context and therefore every HTTPS request.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())

        // Keep SSHJ cryptographic primitives on the app-bundled BC provider;
        // leave HTTPS/TLS provider selection to Android.
        SecurityUtils.setSecurityProvider(BouncyCastleProvider.PROVIDER_NAME)
    }

    /** Catat setiap crash ke Download/AetherAgent-CRASH-*.txt sebelum app tertutup. */
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                writeLog("CRASH", "Thread: ${thread.name}\n\n${error.stackTraceToString()}")
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /**
     * Catat saat thread utama tidak merespons lebih dari 5 detik (dialog "isn't responding").
     * Stack trace thread utama menunjukkan kode apa yang memblokir.
     */
    private fun startFreezeWatchdog() {
        val mainThread = Thread.currentThread()
        val mainHandler = Handler(Looper.getMainLooper())
        val lastBeat = AtomicLong(System.currentTimeMillis())
        Thread {
            var reported = false
            while (true) {
                try {
                    Thread.sleep(1_000)
                } catch (_: InterruptedException) {
                    return@Thread
                }
                mainHandler.post { lastBeat.set(System.currentTimeMillis()) }
                val lag = System.currentTimeMillis() - lastBeat.get()
                if (lag > 5_000 && !reported) {
                    reported = true
                    val stack = mainThread.stackTrace.joinToString("\n") { "    at $it" }
                    runCatching {
                        writeLog("FREEZE", "Thread utama tidak merespons ${lag} ms.\n\nStack thread utama:\n$stack")
                    }
                } else if (lag < 1_000) {
                    reported = false
                }
            }
        }.apply {
            name = "freeze-watchdog"
            isDaemon = true
            start()
        }
    }

    private fun writeLog(kind: String, body: String) {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val name = "AetherAgent-$kind-$stamp.txt"
        val text = buildString {
            appendLine("Waktu: ${Date()}")
            appendLine("Perangkat: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine()
            appendLine(body)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
            resolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        } else {
            val dir = getExternalFilesDir(null) ?: filesDir
            File(dir, name).writeText(text)
        }
    }
}
