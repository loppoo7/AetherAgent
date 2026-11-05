package com.aether.agent

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

@HiltAndroidApp
class AetherApp : Application() {
    init {
        // sshj butuh BouncyCastle versi lengkap; provider BC bawaan Android tidak punya semua algoritma SSH.
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
}
