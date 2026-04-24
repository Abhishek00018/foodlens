package com.caltrack.app.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caltrack.app.BuildConfig
import com.caltrack.app.ui.theme.DarkSurfaceVariant
import com.caltrack.app.ui.theme.NeonLime
import com.caltrack.app.ui.theme.TextSecondary

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var confirmCode by remember { mutableStateOf("") }

    // Navigate when signed in
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.SignedIn) {
            onLoginSuccess()
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NeonLime,
        unfocusedBorderColor = DarkSurfaceVariant,
        focusedLabelColor = NeonLime,
        unfocusedLabelColor = TextSecondary,
        cursorColor = NeonLime,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(NeonLime),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CT",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Caltrack",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Track calories with AI",
                fontSize = 14.sp,
                color = TextSecondary
            )

            // Loading indicator under header
            if (uiState is AuthUiState.Loading) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonLime,
                    trackColor = DarkSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(40.dp))
            }

            // Confirmation code flow
            if (uiState is AuthUiState.NeedsConfirmation) {
                val confirmEmail = (uiState as AuthUiState.NeedsConfirmation).email

                Text(
                    text = "We sent a code to $confirmEmail",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = confirmCode,
                    onValueChange = { confirmCode = it },
                    label = { Text("Verification Code") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.confirmSignUp(confirmEmail, confirmCode) },
                    enabled = uiState !is AuthUiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
                ) {
                    Text(
                        text = "Verify Email",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
            } else {
                // Normal sign in / sign up flow

                // Name field (sign up only)
                if (isSignUp) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Login / Sign Up button
                Button(
                    onClick = {
                        if (isSignUp) viewModel.signUp(name, email, password)
                        else viewModel.signIn(email, password)
                    },
                    enabled = uiState !is AuthUiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
                ) {
                    Text(
                        text = if (isSignUp) "Create Account" else "Log In",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Google sign in via Cognito Hosted UI
                OutlinedButton(
                    onClick = {
                        if (viewModel.isDevMode) {
                            viewModel.skipLogin()
                        } else {
                            val url = "https://${BuildConfig.COGNITO_HOSTED_DOMAIN}/oauth2/authorize" +
                                "?client_id=${BuildConfig.COGNITO_CLIENT_ID}" +
                                "&response_type=code" +
                                "&scope=email+openid+profile" +
                                "&redirect_uri=${BuildConfig.COGNITO_REDIRECT_URI}" +
                                "&identity_provider=Google"
                            val customTabsIntent = CustomTabsIntent.Builder().build()
                            customTabsIntent.launchUrl(context, Uri.parse(url))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Continue with Google",
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Toggle sign up / login
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isSignUp) "Already have an account?" else "Don't have an account?",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    TextButton(onClick = {
                        isSignUp = !isSignUp
                        viewModel.clearError()
                    }) {
                        Text(
                            text = if (isSignUp) "Log In" else "Sign Up",
                            color = NeonLime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Error message
            if (uiState is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = Color(0xFFFF5252),
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Skip for now — dev mode only
        if (viewModel.isDevMode) {
            TextButton(
                onClick = { viewModel.skipLogin() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Skip for now (Dev Mode)",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}
