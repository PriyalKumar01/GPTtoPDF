package com.qwiksave.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qwiksave.app.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "SplashAlpha"
    )

    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.92f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "SplashScale"
    )

    // Sleek skeleton shimmer animation directly below the logo
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE2E8F0),
            Color(0xFF38BDF8).copy(alpha = 0.5f),
            Color(0xFFE2E8F0)
        ),
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1400)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
                .padding(24.dp)
        ) {
            // Centered Transparent App Logo
            Image(
                painter = painterResource(id = R.drawable.app_logo_transparent),
                contentDescription = "Qwik Save Logo",
                modifier = Modifier.size(115.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Brand Title + PRO badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Qwik Save",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFFDC2626), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PRO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "SAVE IN ONE CLICK",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0284C7),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Smooth Skeleton / Shimmer loader bar directly under the logo
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(shimmerBrush)
            )
        }

        // Bottom Brand Attribution Tagline
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .alpha(alphaAnim.value)
        ) {
            Text(
                text = "Qwik Save • Save in One Click",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Engineered by Priyal Kumar",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
