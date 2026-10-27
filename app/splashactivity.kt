package com.example.petcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private val TealLight = Color(0xFF279D8B)
private val TealMiddle = Color(0xFF19776A)
private val TealDark = Color(0xFF064D45)
private val LogoCream = Color(0xFFF8F6F0)
private val MintText = Color(0xFFC4DEDA)
private val PawTeal = Color(0xFF188777)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw edge-to-edge and hide Android's system bars.
        // We draw the reference status bar inside the screen ourselves.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TealDark
                ) {
                    PetCareSplashScreen()
                }
            }
        }
    }
}

@Composable
fun PetCareSplashScreen() {

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        TealLight,
                        TealMiddle,
                        TealDark
                    )
                )
            )
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Fake iPhone status bar from the reference image.
        ReferenceStatusBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = screenWidth * 0.063f,
                    end = screenWidth * 0.065f
                )
                .offset(y = screenHeight * 0.022f)
        )

        // Top tagline
        Text(
            text = "A LITTLE CARE. A LOT OF LOVE.",
            color = MintText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.35.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp)
                .offset(y = screenHeight * 0.060f)
        )

        // White divider above the logo
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = screenHeight * 0.115f)
                .width(screenWidth * 0.308f)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
        )

        // Paw-and-heart logo
        PawHeartLogo(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = screenHeight * 0.133f)
                .size(screenWidth * 0.281f)
        )

        // Main app title
        Text(
            text = "Pet Care",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.7).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = screenHeight * 0.293f)
                .fillMaxWidth()
        )

        // Subtitle
        Text(
            text = "Happy pets, happy you",
            color = MintText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.1.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = screenHeight * 0.364f)
                .fillMaxWidth()
        )

        // Three onboarding-style indicator dots
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = screenHeight * 0.440f),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IndicatorDot(color = Color.White)
            IndicatorDot(color = Color(0xFFB7D6D0))
            IndicatorDot(color = Color(0xFF639F96))
        }
    }
}

@Composable
private fun ReferenceStatusBar(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "9:41",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Cellular signal bars
            Canvas(
                modifier = Modifier.size(
                    width = 19.dp,
                    height = 13.dp
                )
            ) {
                val barWidth = size.width * 0.17f
                val gap = size.width * 0.09f
                val heights = listOf(0.32f, 0.52f, 0.75f, 1f)

                heights.forEachIndexed { index, fraction ->
                    val barHeight = size.height * fraction
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(
                            x = index * (barWidth + gap),
                            y = size.height - barHeight
                        ),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            2f,
                            2f
                        )
                    )
                }
            }

            // Wi-Fi symbol
            Canvas(
                modifier = Modifier.size(
                    width = 17.dp,
                    height = 13.dp
                )
            ) {
                val stroke = size.width * 0.13f

                val arc1 = Path().apply {
                    moveTo(size.width * 0.05f, size.height * 0.30f)
                    quadraticTo(
                        size.width * 0.50f,
                        -size.height * 0.10f,
                        size.width * 0.95f,
                        size.height * 0.30f
                    )
                }

                val arc2 = Path().apply {
                    moveTo(size.width * 0.23f, size.height * 0.49f)
                    quadraticTo(
                        size.width * 0.50f,
                        size.height * 0.23f,
                        size.width * 0.77f,
                        size.height * 0.49f
                    )
                }

                val arc3 = Path().apply {
                    moveTo(size.width * 0.40f, size.height * 0.69f)
                    quadraticTo(
                        size.width * 0.50f,
                        size.height * 0.57f,
                        size.width * 0.60f,
                        size.height * 0.69f
                    )
                }

                drawPath(
                    path = arc1,
                    color = Color.White,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke
                    )
                )
                drawPath(
                    path = arc2,
                    color = Color.White,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke
                    )
                )
                drawPath(
                    path = arc3,
                    color = Color.White,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke
                    )
                )

                drawCircle(
                    color = Color.White,
                    radius = stroke * 0.65f,
                    center = Offset(
                        size.width * 0.50f,
                        size.height * 0.88f
                    )
                )
            }

            // Battery outline and terminal
            Canvas(
                modifier = Modifier.size(
                    width = 25.dp,
                    height = 13.dp
                )
            ) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(0f, size.height * 0.08f),
                    size = Size(
                        size.width * 0.88f,
                        size.height * 0.84f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        size.height * 0.20f
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.2.dp.toPx()
                    )
                )

                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(
                        size.width * 0.09f,
                        size.height * 0.20f
                    ),
                    size = Size(
                        size.width * 0.68f,
                        size.height * 0.60f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        2.dp.toPx()
                    )
                )

                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(
                        size.width * 0.91f,
                        size.height * 0.34f
                    ),
                    size = Size(
                        size.width * 0.09f,
                        size.height * 0.32f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        1.dp.toPx()
                    )
                )
            }
        }
    }
}

@Composable
private fun PawHeartLogo(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {

        val w = size.width
        val h = size.height

        // Ivory rounded-square background
        drawRoundRect(
            color = LogoCream,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                x = w * 0.31f,
                y = h * 0.31f
            )
        )

        // Paw pads: four toes
        // Top-left toe
        rotate(
            degrees = -24f,
            pivot = Offset(w * 0.30f, h * 0.36f)
        ) {
            drawOval(
                color = PawTeal,
                topLeft = Offset(w * 0.215f, h * 0.265f),
                size = Size(w * 0.145f, h * 0.205f)
            )
        }

        // Upper-middle toe
        drawOval(
            color = PawTeal,
            topLeft = Offset(w * 0.365f, h * 0.185f),
            size = Size(w * 0.155f, h * 0.225f)
        )

        // Upper-right toe
        rotate(
            degrees = 12f,
            pivot = Offset(w * 0.605f, h * 0.30f)
        ) {
            drawOval(
                color = PawTeal,
                topLeft = Offset(w * 0.535f, h * 0.20f),
                size = Size(w * 0.155f, h * 0.215f)
            )
        }

        // Far-right toe
        rotate(
            degrees = 27f,
            pivot = Offset(w * 0.735f, h * 0.425f)
        ) {
            drawOval(
                color = PawTeal,
                topLeft = Offset(w * 0.67f, h * 0.34f),
                size = Size(w * 0.145f, h * 0.20f)
            )
        }

        // Large lower paw pad
        drawOval(
            color = PawTeal,
            topLeft = Offset(w * 0.275f, h * 0.445f),
            size = Size(w * 0.45f, h * 0.355f)
        )

        // Heart inside the main paw pad
        val heart = Path().apply {
            moveTo(w * 0.50f, h * 0.705f)

            cubicTo(
                w * 0.455f, h * 0.655f,
                w * 0.375f, h * 0.600f,
                w * 0.375f, h * 0.565f
            )

            cubicTo(
                w * 0.375f, h * 0.505f,
                w * 0.445f, h * 0.505f,
                w * 0.50f, h * 0.555f
            )

            cubicTo(
                w * 0.555f, h * 0.505f,
                w * 0.625f, h * 0.505f,
                w * 0.625f, h * 0.565f
            )

            cubicTo(
                w * 0.625f, h * 0.600f,
                w * 0.545f, h * 0.655f,
                w * 0.50f, h * 0.705f
            )

            close()
        }

        drawPath(
            path = heart,
            color = LogoCream
        )
    }
}

@Composable
private fun IndicatorDot(
    color: Color
) {
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}