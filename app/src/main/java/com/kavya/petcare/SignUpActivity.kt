package com.kavya.petcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (com.kavya.petcare.data.session.SessionManager.getInstance(this).isLoggedIn()) {
            val intent = Intent(this, HomeDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }
        setContent {
            PetCareTheme {
                val viewModel = PetViewModel.getShared(this)
                SignUpScreen(
                    viewModel = viewModel,
                    onBack = {
                        startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                        finish()
                    },
                    onCreateAccount = {
                        startActivity(Intent(this@SignUpActivity, SelectPetTypeActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun SignUpScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onBack: () -> Unit,
    onCreateAccount: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var signUpError by remember { mutableStateOf<String?>(null) }
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable { onBack() },
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = EmeraldDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Hero double-ring logo
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MintSoft)
            )
            Surface(
                modifier = Modifier
                    .size(76.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape, ambientColor = MintSage.copy(alpha = 0.3f)),
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(2.dp, MintSage.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = "Logo",
                        tint = EmeraldDark,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Create Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = EmeraldDark
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Join thousands of caring pet parents today",
            fontSize = 14.sp,
            color = TextSecondary
        )

        if (signUpError != null) {
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AlertRoseSoft,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AlertRose.copy(alpha = 0.3f))
            ) {
                Text(
                    text = signUpError ?: "",
                    modifier = Modifier.padding(14.dp),
                    color = AlertRose,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Card Container for Inputs
        PetModernCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PetTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        signUpError = null
                    },
                    label = "Full Name",
                    placeholder = "e.g. Alex Johnson",
                    leadingIcon = Icons.Default.Person
                )

                PetTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        signUpError = null
                    },
                    label = "Email Address",
                    placeholder = "e.g. alex@example.com",
                    leadingIcon = Icons.Default.Email
                )

                PetTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        signUpError = null
                    },
                    label = "Password (min 6 characters)",
                    placeholder = "Create a strong password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        val isValid = name.isNotBlank() && email.isNotBlank() && password.length >= 6

        PetGradientButton(
            text = "Create Account",
            enabled = isValid && !isLoading,
            isLoading = isLoading,
            onClick = {
                val trimmedName = name.trim()
                val trimmedEmail = email.trim()
                if (trimmedName.isBlank()) {
                    signUpError = "Please enter your full name."
                    return@PetGradientButton
                }
                if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                    signUpError = "Please enter a valid email address."
                    return@PetGradientButton
                }
                if (password.length < 6) {
                    signUpError = "Password must be at least 6 characters."
                    return@PetGradientButton
                }

                viewModel.register(
                    name = trimmedName,
                    email = trimmedEmail,
                    pass = password,
                    onSuccess = {
                        signUpError = null
                        onCreateAccount()
                    },
                    onError = { err ->
                        signUpError = err
                    }
                )
            }
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "By creating an account, you agree to our Terms of Service & Privacy Policy.",
            fontSize = 12.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Already have an account?",
                color = TextSecondary,
                fontSize = 14.sp
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Log In",
                color = MintSage,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onBack() }
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}
