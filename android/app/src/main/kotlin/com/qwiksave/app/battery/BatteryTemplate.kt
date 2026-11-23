package com.qwiksave.app.battery

data class BatteryTemplateItem(
    val id: String,
    val name: String,
    val category: String, // "Anime", "Emoji", "Cyber", "Minimal"
    val tag: String,
    val primaryColor: Long,
    val accentColor: Long,
    val glowColor: Long,
    val description: String,
    val emojiIcon: String = "⚡"
)

object BatteryTemplates {
    val ALL = listOf(
        BatteryTemplateItem(
            id = "gear5_luffy",
            name = "One Piece Gear 5",
            category = "Anime",
            tag = "Sun God Nika",
            primaryColor = 0xFF7E22CE, // Purple-700
            accentColor = 0xFFF3E8FF,
            glowColor = 0xFFA855F7, // Purple-500
            description = "Gear 5 Luffy laughter aura with purple crackling lightning & Straw Hat skull",
            emojiIcon = "👒"
        ),
        BatteryTemplateItem(
            id = "goku_saiyan",
            name = "Super Saiyan Aura",
            category = "Anime",
            tag = "Golden Ki",
            primaryColor = 0xFFCA8A04, // Yellow-600
            accentColor = 0xFFFEF08A,
            glowColor = 0xFFFACC15,
            description = "Golden electric sparks & Super Saiyan surge surrounding battery percentage",
            emojiIcon = "🔥"
        ),
        BatteryTemplateItem(
            id = "tanjiro_sun",
            name = "Sun Breathing Flame",
            category = "Anime",
            tag = "Hinokami Kagura",
            primaryColor = 0xFFDC2626, // Red-600
            accentColor = 0xFFFEE2E2,
            glowColor = 0xFFEF4444,
            description = "Dynamic solar flame vortex with glowing crimson embers",
            emojiIcon = "⚔️"
        ),
        BatteryTemplateItem(
            id = "naruto_sage",
            name = "Kurama Chakra Cloak",
            category = "Anime",
            tag = "Nine Tails",
            primaryColor = 0xFFEA580C, // Orange-600
            accentColor = 0xFFFFEDD5,
            glowColor = 0xFFF97316,
            description = "Swirling Nine-Tails chakra spiral with fiery orange boost",
            emojiIcon = "🦊"
        ),
        BatteryTemplateItem(
            id = "dynamic_emoji",
            name = "Expressive Shimeji",
            category = "Emoji",
            tag = "Reactive Face",
            primaryColor = 0xFFD97706, // Amber-600
            accentColor = 0xFFFEF3C7,
            glowColor = 0xFFFBBF24,
            description = "Expressive face that changes from cool sunglasses to panic sweat as battery drops",
            emojiIcon = "😎"
        ),
        BatteryTemplateItem(
            id = "kawaii_neko",
            name = "Cyber Kitty Shimeji",
            category = "Emoji",
            tag = "Purr Power",
            primaryColor = 0xFFDB2777, // Pink-600
            accentColor = 0xFFFCE7F3,
            glowColor = 0xFFF472B6,
            description = "Cute anime kitten sleeping when full and playfully purring lightning when charging",
            emojiIcon = "🐱"
        ),
        BatteryTemplateItem(
            id = "sparkle_heart",
            name = "Cyber Neon Heart",
            category = "Emoji",
            tag = "Heartbeat Pulse",
            primaryColor = 0xFFBE123C, // Rose-700
            accentColor = 0xFFFFE4E6,
            glowColor = 0xFFFB7185,
            description = "Pulsing neon heart whose heartbeat frequency syncs with current charging rate",
            emojiIcon = "💖"
        ),
        BatteryTemplateItem(
            id = "neon_lightning",
            name = "Cyberpunk Dual Bolt",
            category = "Cyber",
            tag = "Neon Surge",
            primaryColor = 0xFF0891B2, // Cyan-600
            accentColor = 0xFFCFFAFE,
            glowColor = 0xFF22D3EE,
            description = "Futuristic neon cyan & magenta dual lightning rods with acrylic glass backing",
            emojiIcon = "⚡"
        ),
        BatteryTemplateItem(
            id = "dynamic_pill",
            name = "Dynamic Island Fluid",
            category = "Minimal",
            tag = "iOS Sleek",
            primaryColor = 0xFF059669, // Emerald-600
            accentColor = 0xFFD1FAE5,
            glowColor = 0xFF10B981,
            description = "Ultra-clean matte obsidian pill with fluid emerald battery fill & minimal bolt",
            emojiIcon = "🔋"
        ),
        BatteryTemplateItem(
            id = "speed_turbo",
            name = "Turbo Boost Meter",
            category = "Cyber",
            tag = "Tachometer",
            primaryColor = 0xFF2563EB, // Blue-600
            accentColor = 0xFFDBEAFE,
            glowColor = 0xFF38BDF8,
            description = "Automotive tachometer dial with RPM boost needle and nitro battery readout",
            emojiIcon = "🏎️"
        ),
        BatteryTemplateItem(
            id = "reactor_core",
            name = "Sci-Fi Arc Reactor",
            category = "Cyber",
            tag = "Zero Point",
            primaryColor = 0xFF7C3AED, // Violet-600
            accentColor = 0xFFEDE9FE,
            glowColor = 0xFFA78BFA,
            description = "Iron Man inspired circular reactor core with rotating energy particle ring",
            emojiIcon = "⚛️"
        ),
        BatteryTemplateItem(
            id = "minimal_plug",
            name = "Minimal Power Circuit",
            category = "Minimal",
            tag = "Executive Clean",
            primaryColor = 0xFF475569, // Slate-600
            accentColor = 0xFFF1F5F9,
            glowColor = 0xFF94A3B8,
            description = "Understated high-end hardware plug with sleek status bar integration",
            emojiIcon = "🔌"
        )
    )

    fun getById(id: String): BatteryTemplateItem {
        return ALL.find { it.id.equals(id, ignoreCase = true) } ?: ALL.first()
    }
}
