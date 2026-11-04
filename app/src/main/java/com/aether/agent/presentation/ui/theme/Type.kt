package com.aether.agent.presentation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.aether.agent.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private fun googleFont(name: String, weight: FontWeight) =
    Font(googleFont = GoogleFont(name), fontProvider = fontProvider, weight = weight)

/** Heading: Newsreader, kesan hangat seperti buku. */
val Newsreader = FontFamily(
    googleFont("Newsreader", FontWeight.Light),
    googleFont("Newsreader", FontWeight.Normal),
    googleFont("Newsreader", FontWeight.Medium),
    googleFont("Newsreader", FontWeight.SemiBold),
)

/** Body dan UI: Instrument Sans. */
val InstrumentSans = FontFamily(
    googleFont("Instrument Sans", FontWeight.Normal),
    googleFont("Instrument Sans", FontWeight.Medium),
    googleFont("Instrument Sans", FontWeight.SemiBold),
    googleFont("Instrument Sans", FontWeight.Bold),
)

/** Blok kode: JetBrains Mono. */
val AetherMono = FontFamily(
    googleFont("JetBrains Mono", FontWeight.Normal),
    googleFont("JetBrains Mono", FontWeight.Medium),
)

val AetherTypography = androidx.compose.material3.Typography(
    headlineSmall = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = InstrumentSans, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = InstrumentSans, fontSize = 15.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontFamily = InstrumentSans, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.3.sp),
)
