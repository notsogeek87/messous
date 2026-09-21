package com.budgetflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetflow.engine.recognition.RecognizableService

/**
 * A small colored "logo" for a recognized service (spec section 7/9): a monogram avatar in a
 * curated brand color, e.g. a red circle "N" for Netflix - this is exactly the "🔴 Netflix"
 * visual language from the spec's own mockup, not a placeholder. BudgetFlow stays 100% offline
 * (section 1) and never redistributes third-party brand artwork without confirming redistribution
 * rights per brand (section 7's explicit warning), so this monogram avatar *is* the level-1 icon
 * (section 8) - swap a given [ServiceIcons] entry for a bundled vector logo later, once rights are
 * confirmed for that brand, without touching any call site or the recognition engine.
 */
@Composable
fun ServiceLogo(service: RecognizableService, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val color = ServiceIcons.colorFor(service.icon, service.id)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ServiceIcons.initialFor(service.name),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp
        )
    }
}

object ServiceIcons {
    /** The letter/digit shown inside a [ServiceLogo] when there's no dedicated glyph. */
    fun initialFor(name: String): String =
        name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"

    /** A curated brand color for [iconKey], or a color deterministically derived from [fallbackSeed]. */
    fun colorFor(iconKey: String, fallbackSeed: String): Color = BRAND_COLORS[iconKey] ?: hashColor(fallbackSeed)

    /** A small fixed palette so unrecognized brands still get a stable, visually distinct color. */
    private fun hashColor(seed: String): Color {
        val hash = seed.fold(0) { acc, char -> acc * 31 + char.code }
        val index = ((hash % FALLBACK_PALETTE.size) + FALLBACK_PALETTE.size) % FALLBACK_PALETTE.size
        return FALLBACK_PALETTE[index]
    }

    private val FALLBACK_PALETTE: List<Color> = listOf(
        Color(0xFF2E7D5B), Color(0xFF1565C0), Color(0xFF6A1B9A), Color(0xFFAD1457),
        Color(0xFFEF6C00), Color(0xFF00838F), Color(0xFF558B2F), Color(0xFF4527A0),
        Color(0xFFC62828), Color(0xFF00695C), Color(0xFF37474F), Color(0xFF9E5B00)
    )

    /** Curated colors for the most recognizable brands in the catalog; anything else falls back to [hashColor]. */
    private val BRAND_COLORS: Map<String, Color> = mapOf(
        // Streaming vidéo
        "netflix" to Color(0xFFE50914),
        "disneyplus" to Color(0xFF113CCF),
        "primevideo" to Color(0xFF00A8E1),
        "hbomax" to Color(0xFF6B2FBF),
        "paramountplus" to Color(0xFF0064FF),
        "appletvplus" to Color(0xFF000000),
        "canalplus" to Color(0xFF000000),
        "ocs" to Color(0xFFFF6B00),
        "mycanal" to Color(0xFF000000),
        "francetv" to Color(0xFF00A0DC),
        "crunchyroll" to Color(0xFFF47521),
        "youtubepremium" to Color(0xFFFF0000),
        "dazn" to Color(0xFF171717),
        "beinsports" to Color(0xFF001B4F),
        "eurosport" to Color(0xFF00A8E7),
        "ligue1plus" to Color(0xFF1C1E5A),
        // Musique / audio
        "spotify" to Color(0xFF1DB954),
        "deezer" to Color(0xFFA238FF),
        "applemusic" to Color(0xFFFA243C),
        "youtubemusic" to Color(0xFFFF0000),
        "amazonmusic" to Color(0xFF25D1DA),
        "tidal" to Color(0xFF000000),
        "soundcloud" to Color(0xFFFF5500),
        "audible" to Color(0xFF232F3E),
        "calm" to Color(0xFF3E7CB1),
        "headspace" to Color(0xFFFF6D2E),
        // IA
        "chatgpt" to Color(0xFF10A37F),
        "claude" to Color(0xFFCC785C),
        "gemini" to Color(0xFF4285F4),
        "perplexity" to Color(0xFF20808D),
        "copilot" to Color(0xFF0078D4),
        "grok" to Color(0xFF000000),
        "midjourney" to Color(0xFF000000),
        "canva" to Color(0xFF00C4CC),
        "mistral" to Color(0xFFFA5210),
        // Cloud / stockage
        "googleone" to Color(0xFF4285F4),
        "icloud" to Color(0xFF3693F3),
        "onedrive" to Color(0xFF0078D4),
        "dropbox" to Color(0xFF0061FF),
        "protondrive" to Color(0xFF6D4AFF),
        "mega" to Color(0xFFD9272E),
        "box" to Color(0xFF0061D5),
        // Logiciels
        "microsoft365" to Color(0xFFD83B01),
        "adobecc" to Color(0xFFDA1F26),
        "figma" to Color(0xFFF24E1E),
        "notion" to Color(0xFF000000),
        "evernote" to Color(0xFF00A82D),
        "todoist" to Color(0xFFE44332),
        "1password" to Color(0xFF1A285F),
        "bitwarden" to Color(0xFF175DDC),
        "dashlane" to Color(0xFF0E353D),
        "nordvpn" to Color(0xFF4687C6),
        "protonvpn" to Color(0xFF6D4AFF),
        "expressvpn" to Color(0xFFDA3940),
        "surfshark" to Color(0xFF1EBFBF),
        "grammarly" to Color(0xFF15C39A),
        "githubcopilot" to Color(0xFF181717),
        "jetbrains" to Color(0xFF000000),
        // Jeux
        "xboxgamepass" to Color(0xFF107C10),
        "psplus" to Color(0xFF003791),
        "nintendoswitchonline" to Color(0xFFE60012),
        "eaplay" to Color(0xFFFF4747),
        "geforcenow" to Color(0xFF76B900),
        "applearcade" to Color(0xFF000000),
        "roblox" to Color(0xFF000000),
        "minecraft" to Color(0xFF62B47A),
        // Télécom / internet
        "orange" to Color(0xFFFF7900),
        "sosh" to Color(0xFF6D8CF0),
        "sfr" to Color(0xFFE2001A),
        "bouygues" to Color(0xFF0092CF),
        "free" to Color(0xFFCE0032),
        "starlink" to Color(0xFF000000),
        // Énergie / eau
        "edf" to Color(0xFFEE7203),
        "engie" to Color(0xFF00AAA0),
        "totalenergies" to Color(0xFFD1051F),
        "veolia" to Color(0xFF0077C8),
        "suez" to Color(0xFF00A9E0),
        // Assurances / banques
        "axa" to Color(0xFF00008F),
        "allianz" to Color(0xFF003781),
        "maif" to Color(0xFFE2001A),
        "macif" to Color(0xFF0069B4),
        "groupama" to Color(0xFFE2001A),
        "creditagricole" to Color(0xFF00964B),
        "creditmutuel" to Color(0xFFC8102E),
        "cic" to Color(0xFF004A98),
        "bnpparibas" to Color(0xFF00915A),
        "societegenerale" to Color(0xFFE60028),
        "caissedepargne" to Color(0xFFE2001A),
        "lcl" to Color(0xFF00549F),
        "boursobank" to Color(0xFFEF4123),
        "fortuneo" to Color(0xFF6AB023),
        "revolut" to Color(0xFF0666EB),
        "n26" to Color(0xFF000000),
        "lydia" to Color(0xFF0ECA97),
        // Automobile / transport
        "tesla" to Color(0xFFE31937),
        "sncf" to Color(0xFF8B2E8B),
        "ouigo" to Color(0xFFA1D033),
        "ratp" to Color(0xFF0064B0),
        "uber" to Color(0xFF000000),
        "bolt" to Color(0xFF34D186),
        "blablacar" to Color(0xFF00AFF5),
        // Sport
        "basicfit" to Color(0xFF1A1A1A),
        "fitnesspark" to Color(0xFFE2001A),
        "strava" to Color(0xFFFC4C02),
        "fitbit" to Color(0xFF00B0B9),
        "garminconnect" to Color(0xFF007CC3),
        // Shopping / livraison
        "amazon" to Color(0xFFFF9900),
        "carrefour" to Color(0xFF004E9F),
        "auchan" to Color(0xFFE2001A),
        "leclerc" to Color(0xFF0055A4),
        "lidl" to Color(0xFF0050AA),
        "ubereats" to Color(0xFF06C167),
        "deliveroo" to Color(0xFF00CCBC),
        "justeat" to Color(0xFFFF8000),
        "toogoodtogo" to Color(0xFF4E1E42),
        "cdiscount" to Color(0xFF283891),
        "fnac" to Color(0xFFEFA700),
        "darty" to Color(0xFFE2001A),
        "rakuten" to Color(0xFFBF0000),
        "zalando" to Color(0xFFFF6900),
        // Presse
        "lemonde" to Color(0xFF000000),
        "lefigaro" to Color(0xFF0053A0),
        "lequipe" to Color(0xFF002B54),
        "mediapart" to Color(0xFF000000),
        // Éducation
        "duolingo" to Color(0xFF58CC02),
        "coursera" to Color(0xFF0056D2),
        "udemy" to Color(0xFFA435F0),
        "linkedinlearning" to Color(0xFF0A66C2),
        "masterclass" to Color(0xFFE50914),
        // Revenus
        "salaire" to Color(0xFF2E7D5B),
        "caf" to Color(0xFF0075BC),
        "francetravail" to Color(0xFF001A72),
        "retraite" to Color(0xFF2E7D5B)
    )
}
