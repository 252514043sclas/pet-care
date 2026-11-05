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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.data.network.NetworkConfig
import com.kavya.petcare.ui.*
import com.kavya.petcare.ui.theme.PetCareTheme

class LoginActivity : ComponentActivity() {
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
                LoginScreen(
                    viewModel = viewModel,
                    onLogin = {
                        val intent = Intent(this@LoginActivity, HomeDashboardActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    },
                    onSignUp = {
                        startActivity(Intent(this@LoginActivity, SignUpActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: PetViewModel = PetViewModel.getShared(LocalContext.current),
    onLogin: () -> Unit,
    onSignUp: () -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showServerConfigDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var serverUrlInput by remember { mutableStateOf(NetworkConfig.getCurrentBaseUrl(context)) }
    var resetEmail by remember { mutableStateOf("") }
    var resetSuccessMessage by remember { mutableStateOf("") }
    var resetErrorMessage by remember { mutableStateOf<String?>(null) }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }
    val isLoading by viewModel.isLoading.collectAsState()

    // ------------------------------------------------------------------------
    // Server Configuration Dialog
    // ------------------------------------------------------------------------
    if (showServerConfigDialog) {
        AlertDialog(
            onDismissRequest = { showServerConfigDialog = false },
            title = {
                Text(
                    text = "Server Configuration",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (NetworkConfig.isEmulator()) "Environment: Android Emulator" else "Environment: Physical Device",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = { serverUrlInput = it },
                        label = { Text("API Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { serverUrlInput = NetworkConfig.EMULATOR_BASE_URL },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("10.0.2.2", fontSize = 11.sp, color = EmeraldDark)
                        }
                        OutlinedButton(
                            onClick = { serverUrlInput = NetworkConfig.LAN_BASE_URL },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("LAN Host", fontSize = 11.sp, color = EmeraldDark)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        NetworkConfig.setCustomBaseUrl(context, serverUrlInput)
                        showServerConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintSage),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerConfigDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Forgot Password Dialog
    // ------------------------------------------------------------------------
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotPasswordDialog = false
                resetErrorMessage = null
            },
            title = { Text("Reset Password", fontWeight = FontWeight.Bold, color = EmeraldDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter your email address to receive a secure password reset link.", color = TextSecondary, fontSize = 14.sp)
                    if (resetErrorMessage != null) {
                        Text(
                            text = resetErrorMessage ?: "",
                            color = AlertRose,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it
                            resetErrorMessage = null
                        },
                        label = { Text("Registered Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = resetEmail.trim()
                        if (trimmed.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
                            resetErrorMessage = "Please enter a valid email address."
                            return@Button
                        }
                        viewModel.forgotPassword(trimmed) { success, msg ->
                            if (success) {
                                resetSuccessMessage = msg
                                loginErrorMessage = null
                                showForgotPasswordDialog = false
                                resetEmail = ""
                            } else {
                                resetErrorMessage = msg
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintSage),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showForgotPasswordDialog = false
                    resetErrorMessage = null
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Main Login Layout
    // ------------------------------------------------------------------------
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row with server settings button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        serverUrlInput = NetworkConfig.getCurrentBaseUrl(context)
                        showServerConfigDialog = true
                    },
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Server Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Hero Logo with double ring effect
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .background(MintSoft)
            )
            Surface(
                modifier = Modifier
                    .size(84.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = MintSage.copy(alpha = 0.3f)),
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(2.dp, MintSage.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = "Logo",
                        tint = EmeraldDark,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Welcome Back",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = EmeraldDark
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Sign in to continue caring for your pet",
            fontSize = 14.sp,
            color = TextSecondary
        )

        // Reset success notification
        if (resetSuccessMessage.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MintSoft,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MintSage.copy(alpha = 0.3f))
            ) {
                Text(
                    text = resetSuccessMessage,
                    modifier = Modifier.padding(14.dp),
                    color = EmeraldDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        // Error message banner
        if (loginErrorMessage != null) {
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AlertRoseSoft,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AlertRose.copy(alpha = 0.3f))
            ) {
                Text(
                    text = loginErrorMessage ?: "",
                    modifier = Modifier.padding(14.dp),
                    color = AlertRose,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Input Card
        PetModernCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PetTextField(
                    value = identifier,
                    onValueChange = {
                        identifier = it
                        loginErrorMessage = null
                    },
                    label = "Email address or User ID",
                    placeholder = "e.g. user@example.com or User ID",
                    leadingIcon = Icons.Default.Email
                )

                PetTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        loginErrorMessage = null
                    },
                    label = "Password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showForgotPasswordDialog = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Forgot Password?",
                            color = MintSage,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        PetGradientButton(
            text = "Log In",
            isLoading = isLoading,
            onClick = {
                val trimmedId = identifier.trim()
                if (trimmedId.isBlank()) {
                    loginErrorMessage = "Please enter your email or user ID."
                    return@PetGradientButton
                }
                if (password.isBlank()) {
                    loginErrorMessage = "Please enter your password."
                    return@PetGradientButton
                }
                viewModel.login(
                    email = trimmedId,
                    pass = password,
                    onSuccess = {
                        loginErrorMessage = null
                        onLogin()
                    },
                    onError = { err ->
                        loginErrorMessage = err
                    }
                )
            }
        )

        Spacer(Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Don't have an account?",
                color = TextSecondary,
                fontSize = 14.sp
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Sign Up",
                color = MintSage,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onSignUp() }
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}
