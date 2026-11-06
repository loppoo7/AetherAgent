package com.aether.agent

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.security.Security
import net.schmizz.sshj.common.SecurityUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider

@HiltAndroidApp
class AetherApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // SSHJ needs the newer BC algorithms, but BC must not outrank Android's
        // Conscrypt/AndroidOpenSSL provider. Putting BC at position 1 can break
        // the system's default TLS context and therefore every HTTPS request.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())

        // Keep SSHJ cryptographic primitives on the app-bundled BC provider;
        // leave HTTPS/TLS provider selection to Android.
        SecurityUtils.setSecurityProvider(BouncyCastleProvider.PROVIDER_NAME)
    }
}
