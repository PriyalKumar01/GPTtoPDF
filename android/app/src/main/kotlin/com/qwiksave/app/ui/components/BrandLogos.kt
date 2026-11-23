package com.qwiksave.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qwiksave.app.R

/**
 * Authentic brand icons for social media and utility services.
 * Uses official vector drawables for Instagram, Facebook, YouTube, LinkedIn.
 */
@Composable
fun InstagramLogo(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Image(
        painter = painterResource(id = R.drawable.ic_instagram_brand),
        contentDescription = "Instagram",
        modifier = modifier.size(size)
    )
}

@Composable
fun FacebookLogo(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Image(
        painter = painterResource(id = R.drawable.ic_facebook_brand),
        contentDescription = "Facebook",
        modifier = modifier.size(size)
    )
}

@Composable
fun YouTubeLogo(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Image(
        painter = painterResource(id = R.drawable.ic_youtube_brand),
        contentDescription = "YouTube",
        modifier = modifier.size(size)
    )
}

@Composable
fun LinkedInLogo(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Image(
        painter = painterResource(id = R.drawable.ic_linkedin_brand),
        contentDescription = "LinkedIn",
        modifier = modifier.size(size)
    )
}

@Composable
fun AiNotesLogo(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    if (size >= 40.dp) {
        // Multi-Brand AI Cluster Logo with exact original ChatGPT, Gemini & Claude emblems
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.28f))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E293B)
                        )
                    )
                )
                .padding(size * 0.10f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Row: Authentic ChatGPT (OpenAI Spiral) + Authentic Claude (Anthropic Starburst)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Exact Official ChatGPT Emblem
                    Image(
                        painter = painterResource(id = R.drawable.ic_chatgpt_brand),
                        contentDescription = "ChatGPT",
                        modifier = Modifier
                            .size(size * 0.38f)
                            .clip(RoundedCornerShape(size * 0.10f))
                    )

                    // Exact Official Claude Emblem
                    Image(
                        painter = painterResource(id = R.drawable.ic_claude_brand),
                        contentDescription = "Claude",
                        modifier = Modifier
                            .size(size * 0.38f)
                            .clip(RoundedCornerShape(size * 0.10f))
                    )
                }

                // Bottom Row: Authentic Google Gemini (Multi-Color Sparkle) + Official Red PDF Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Exact Official Google Gemini Sparkle Star
                    Image(
                        painter = painterResource(id = R.drawable.ic_gemini_brand),
                        contentDescription = "Gemini",
                        modifier = Modifier
                            .size(size * 0.38f)
                            .clip(RoundedCornerShape(size * 0.10f))
                    )

                    // Official Crimson Red PDF Badge
                    Box(
                        modifier = Modifier
                            .size(size * 0.38f)
                            .clip(RoundedCornerShape(size * 0.10f))
                            .background(Color(0xFFDC2626)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PDF",
                            fontSize = (size.value * 0.16f).sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
    } else {
        // Compact single-icon representation: official ChatGPT emblem
        Image(
            painter = painterResource(id = R.drawable.ic_chatgpt_brand),
            contentDescription = "AI Notes",
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.28f))
        )
    }
}

@Composable
fun PdfSuiteLogo(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Color(0xFFDC2626)), // PDF Crimson Red
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PictureAsPdf,
            contentDescription = "PDF Tools",
            tint = Color.White,
            modifier = Modifier.size(size * 0.65f)
        )
    }
}

@Composable
fun QrCodeLogo(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Color(0xFF0F172A)), // Sleek Black & White QR Style
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.QrCode2,
            contentDescription = "QR Code",
            tint = Color.White,
            modifier = Modifier.size(size * 0.65f)
        )
    }
}

@Composable
fun AudioExtractorLogo(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Color(0xFF1E1B4B)), // Rich Dark Indigo
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Audiotrack,
            contentDescription = "Audio Extractor",
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(size * 0.65f)
        )
    }
}

@Composable
fun GoogleLogo(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Image(
        painter = painterResource(id = R.drawable.ic_google_brand),
        contentDescription = "Google",
        modifier = modifier.size(size)
    )
}

@Composable
fun GitHubLogo(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Image(
        painter = painterResource(id = R.drawable.ic_github_brand),
        contentDescription = "GitHub",
        modifier = modifier.size(size)
    )
}

@Composable
fun XLogo(modifier: Modifier = Modifier, size: Dp = 22.dp, tint: Color = Color.White) {
    Icon(
        painter = painterResource(id = R.drawable.ic_x_brand),
        contentDescription = "X",
        tint = tint,
        modifier = modifier.size(size)
    )
}
