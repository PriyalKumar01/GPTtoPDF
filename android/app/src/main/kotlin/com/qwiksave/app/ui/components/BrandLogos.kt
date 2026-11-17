package com.qwiksave.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Color(0xFF10A37F)), // OpenAI Emerald Green
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "AI Notes",
            tint = Color.White,
            modifier = Modifier.size(size * 0.6f)
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
            .background(Color(0xFF7C3AED)), // Purple
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.QrCode,
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
