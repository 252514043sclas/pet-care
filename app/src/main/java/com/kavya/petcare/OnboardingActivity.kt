package com.kavya.petcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.theme.PetCareTheme

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetCareTheme {
                OnboardingScreen(
                    onContinue = {
                        startActivity(Intent(this@OnboardingActivity, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }

    val titles = listOf(
        "Care for your best friend",
        "Keep your pet healthy",
        "Everyday care made easy"
    )

    val descriptions = listOf(
        "Everything your pet needs, all in one place.",
        "Track symptoms and keep important health information handy.",
        "Manage meals, weight and grooming reminders with ease."
    )

    val icons = listOf(Icons.Default.Pets, Icons.Default.Favorite, Icons.Default.Notifications)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8F3))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onContinue) {
                Text("Skip", color = Color(0xFF267D6D))
            }
        }

        Spacer(Modifier.weight(1f))

        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = Color(0xFFE8F5F2)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icons[page],
                    contentDescription = "Illustration",
                    tint = Color(0xFF267D6D),
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(Modifier.height(36.dp))

        Text(
            titles[page],
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF184D43),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            descriptions[page],
            fontSize = 16.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == page) 12.dp else 8.dp)
                        .background(
                            if (index == page)
                                Color(0xFF267D6D)
                            else
                                Color.LightGray,
                            CircleShape
                        )
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                if (page < 2) {
                    page++
                } else {
                    onContinue()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF267D6D)
            )
        ) {
            Text(if (page == 2) "Get Started" else "Next")
        }
    }
}
