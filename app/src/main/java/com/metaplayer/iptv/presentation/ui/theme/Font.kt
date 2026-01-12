package com.metaplayer.iptv.presentation.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.metaplayer.iptv.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val LoraFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Lora"),
        fontProvider = provider
    )
)
