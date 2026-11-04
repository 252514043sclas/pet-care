package com.kavya.petcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kavya.petcare.data.network.NetworkConfig
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
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showServerConfigDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var serverUrlInput by remember { mutableStateOf(NetworkConfig.getCurrentBaseUrl(context)) }
    var resetEmail by remember { mutableStateOf("") }
    var resetSuccessMessage by remember { mutableStateOf("") }
    var resetErrorMessage by remember { mutableStateOf<String?>(null) }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }
    val isLoading by viewModel.isLoading.collectAsState()

    if (showServerConfigDialog) {
        AlertDialog(
            onDismissRequest = { showServerConfigDialog = false },
            title = {
                Text(
                    text = "Server Configuration",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF184D43)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (NetworkConfig.isEmulator()) "Device: Android Emulator" else "Device: Physical Phone",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = { serverUrlInput = it },
                        label = { Text("API Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { serverUrlInput = NetworkConfig.EMULATOR_BASE_URL },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("10.0.2.2", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { serverUrlInput = NetworkConfig.LAN_BASE_URL },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("LAN Host", fontSize = 11.sp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF267D6D))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerConfigDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotPasswordDialog = false
                resetErrorMessage = null
            },
            title = { Text("Reset Password", fontWeight = FontWeight.Bold, color = Color(0xFF184D43)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter your email address to receive a password reset link.", color = Color(0xFF2D3748))
                    if (resetErrorMessage != null) {
                        Text(
                            text = resetErrorMessage ?: "",
                            color = Color(0xFFC62828),
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
                        label = { Text("Email address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(color = Color(0xFF111816), fontSize = 15.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111816),
                            unfocusedTextColor = Color(0xFF111816),
                            focusedBorderColor = Color(0xFF267D6D),
                            unfocusedBorderColor = Color(0xFFB5C4BE),
                            focusedLabelColor = Color(0xFF184D43),
                            unfocusedLabelColor = Color(0xFF556B64)
                        ),
                        shape = RoundedCornerShape(12.dp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF267D6D))
                ) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showForgotPasswordDialog = false
                    resetErrorMessage = null
                }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8F3))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Surface(
            modifier = Modifier
                .size(80.dp)
                .clickable {
                    serverUrlInput = NetworkConfig.getCurrentBaseUrl(context)
                    showServerConfigDialog = true
                },
            shape = CircleShape,
            color = Color(0xFFE8F5F2)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = "Logo",
                    tint = Color(0xFF184D43),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Welcome Back!",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF184D43)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Log in to continue caring for your pet",
            color = Color.Gray
        )

        if (resetSuccessMessage.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFE8F5F2),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = resetSuccessMessage,
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFF184D43),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        if (loginErrorMessage != null) {
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFEBEE),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = loginErrorMessage ?: "",
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFFC62828),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = identifier,
            onValueChange = {
                identifier = it
                loginErrorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email address or User ID") },
            placeholder = { Text("e.g. user@example.com or User ID", color = Color.Gray) },
            singleLine = true,
            textStyle = TextStyle(color = Color(0xFF111816), fontSize = 16.sp, fontWeight = FontWeight.Normal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF111816),
                unfocusedTextColor = Color(0xFF111816),
                focusedBorderColor = Color(0xFF267D6D),
                unfocusedBorderColor = Color(0xFFB5C4BE),
                focusedLabelColor = Color(0xFF184D43),
                unfocusedLabelColor = Color(0xFF556B64)
            ),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                loginErrorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            textStyle = TextStyle(color = Color(0xFF111816), fontSize = 16.sp, fontWeight = FontWeight.Normal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF111816),
                unfocusedTextColor = Color(0xFF111816),
                focusedBorderColor = Color(0xFF267D6D),
                unfocusedBorderColor = Color(0xFFB5C4BE),
                focusedLabelColor = Color(0xFF184D43),
                unfocusedLabelColor = Color(0xFF556B64)
            ),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = Color(0xFF267D6D)
                    )
                }
            },
            shape = RoundedCornerShape(14.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { showForgotPasswordDialog = true }) {
                Text("Forgot Password?", color = Color(0xFF267D6D), fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                val trimmedId = identifier.trim()
                if (trimmedId.isBlank()) {
                    loginErrorMessage = "Please enter your email or user ID."
                    return@Button
                }
                if (password.isBlank()) {
                    loginErrorMessage = "Please enter your password."
                    return@Button
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
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF267D6D)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Log In")
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have an account? ")

            TextButton(onClick = onSignUp) {
                Text(
                    "Sign Up",
                    color = Color(0xFF267D6D),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
