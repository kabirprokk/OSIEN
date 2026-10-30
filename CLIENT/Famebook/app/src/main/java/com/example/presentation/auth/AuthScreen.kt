package com.example.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.util.UUID
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.User
import com.example.domain.repository.UserRepository
import com.example.presentation.components.PrimaryGoldButton
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RecRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

enum class AuthMode {
  SIGN_IN,
  SIGN_UP
}

@Composable
fun AuthScreen(
  userRepository: UserRepository,
  initialMode: AuthMode = AuthMode.SIGN_IN,
  onLoginSuccess: (User) -> Unit
) {
  val scope = rememberCoroutineScope()
  var authMode by remember(initialMode) { mutableStateOf(initialMode) }

  // Sign In fields
  var signInEmail by remember { mutableStateOf("") }
  var signInPassword by remember { mutableStateOf("") }

  // Sign Up fields
  var signUpFullName by remember { mutableStateOf("") }
  var signUpEmail by remember { mutableStateOf("") }
  var signUpPassword by remember { mutableStateOf("") }
  var signUpConfirmPassword by remember { mutableStateOf("") }

  // Status & dialogs
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showForgotPasswordDialog by remember { mutableStateOf(false) }
  var resetEmail by remember { mutableStateOf("") }
  var resetSuccessMessage by remember { mutableStateOf<String?>(null) }
  var resetErrorMessage by remember { mutableStateOf<String?>(null) }
  var resetSending by remember { mutableStateOf(false) }

  val scrollState = rememberScrollState()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianBlack)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 24.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(24.dp))

      // Brand Identity
      Image(
        painter = painterResource(id = R.drawable.famebook_logo),
        contentDescription = "FameBook Logo",
        modifier = Modifier
          .height(84.dp)
          .clip(RoundedCornerShape(20.dp))
      )

      Spacer(modifier = Modifier.height(36.dp))

      // Mode Switcher Header
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(modifier = Modifier.padding(4.dp)) {
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(if (authMode == AuthMode.SIGN_IN) AmberGold else DarkElevated)
              .clickable {
                authMode = AuthMode.SIGN_IN
                errorMessage = null
              }
              .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "SIGN IN",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = if (authMode == AuthMode.SIGN_IN) ObsidianBlack else TextMuted
            )
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(if (authMode == AuthMode.SIGN_UP) AmberGold else DarkElevated)
              .clickable {
                authMode = AuthMode.SIGN_UP
                errorMessage = null
              }
              .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "CREATE ACCOUNT",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = if (authMode == AuthMode.SIGN_UP) ObsidianBlack else TextMuted
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Error banner
      if (errorMessage != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = RecRed.copy(alpha = 0.15f),
          border = androidx.compose.foundation.BorderStroke(1.dp, RecRed.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = errorMessage!!,
            style = MaterialTheme.typography.bodySmall,
            color = PureWhite,
            modifier = Modifier.padding(14.dp),
            textAlign = TextAlign.Center
          )
        }
        Spacer(modifier = Modifier.height(20.dp))
      }

      // Main Form Card
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(22.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          if (authMode == AuthMode.SIGN_IN) {
            // SIGN IN FORM
            Text(
              text = "Welcome Back",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = PureWhite
            )

            OutlinedTextField(
              value = signInEmail,
              onValueChange = {
                signInEmail = it
                errorMessage = null
              },
              label = { Text("Email", color = TextMuted) },
              placeholder = { Text("name@example.com", color = TextMuted) },
              leadingIcon = {
                Icon(Icons.Default.Mail, contentDescription = null, tint = TextMuted)
              },
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PureWhite,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = AmberGold,
                unfocusedBorderColor = DarkBorder,
                cursorColor = AmberGold
              ),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_email_input")
            )

            OutlinedTextField(
              value = signInPassword,
              onValueChange = {
                signInPassword = it
                errorMessage = null
              },
              label = { Text("Password", color = TextMuted) },
              leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
              },
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PureWhite,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = AmberGold,
                unfocusedBorderColor = DarkBorder,
                cursorColor = AmberGold
              ),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_password_input")
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              TextButton(
                onClick = {
                  resetEmail = signInEmail
                  showForgotPasswordDialog = true
                }
              ) {
                Text(
                  text = "Forgot password?",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                  color = AmberGold
                )
              }
            }

            PrimaryGoldButton(
              text = if (isLoading) "SIGNING IN..." else "SIGN IN",
              onClick = {
                if (signInEmail.isBlank() || signInPassword.isBlank()) {
                  errorMessage = "Please enter your email and password."
                  return@PrimaryGoldButton
                }
                isLoading = true
                errorMessage = null
                scope.launch {
                  val result = userRepository.login(signInEmail, signInPassword)
                  isLoading = false
                  if (result.isSuccess) {
                    onLoginSuccess(result.getOrThrow())
                  } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Sign in failed."
                  }
                }
              },
              enabled = !isLoading,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_submit_button")
            )

            GoogleSignInButton(
              userRepository = userRepository,
              onSuccess = onLoginSuccess,
              onError = { errorMessage = it }
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Don't have an account?",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
              )
              TextButton(
                onClick = {
                  authMode = AuthMode.SIGN_UP
                  errorMessage = null
                }
              ) {
                Text(
                  text = "Create account",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                  color = AmberGold
                )
              }
            }
          } else {
            // SIGN UP FORM
            Text(
              text = "Create Account",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = PureWhite
            )

            OutlinedTextField(
              value = signUpFullName,
              onValueChange = {
                signUpFullName = it
                errorMessage = null
              },
              label = { Text("Full Name", color = TextMuted) },
              placeholder = { Text("Alex Vance", color = TextMuted) },
              leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted)
              },
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PureWhite,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = AmberGold,
                unfocusedBorderColor = DarkBorder,
                cursorColor = AmberGold
              ),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_fullname_input")
            )

            OutlinedTextField(
              value = signUpEmail,
              onValueChange = {
                signUpEmail = it
                errorMessage = null
              },
              label = { Text("Email", color = TextMuted) },
              placeholder = { Text("name@example.com", color = TextMuted) },
              leadingIcon = {
                Icon(Icons.Default.Mail, contentDescription = null, tint = TextMuted)
              },
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PureWhite,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = AmberGold,
                unfocusedBorderColor = DarkBorder,
                cursorColor = AmberGold
              ),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_email_input")
            )

            OutlinedTextField(
              value = signUpPassword,
              onValueChange = {
                signUpPassword = it
                errorMessage = null
              },
              label = { Text("Password", color = TextMuted) },
              leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
              },
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PureWhite,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = AmberGold,
                unfocusedBorderColor = DarkBorder,
                cursorColor = AmberGold
              ),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_password_input")
            )

            OutlinedTextField(
              value = signUpConfirmPassword,
              onValueChange = {
                signUpConfirmPassword = it
                errorMessage = null
              },
              label = { Text("Confirm Password", color = TextMuted) },
              leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
              },
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PureWhite,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = AmberGold,
                unfocusedBorderColor = DarkBorder,
                cursorColor = AmberGold
              ),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_confirm_password_input")
            )

            PrimaryGoldButton(
              text = if (isLoading) "CREATING ACCOUNT..." else "CREATE ACCOUNT",
              onClick = {
                if (signUpFullName.isBlank() || signUpEmail.isBlank() || signUpPassword.isBlank()) {
                  errorMessage = "Please complete all fields."
                  return@PrimaryGoldButton
                }
                if (signUpPassword != signUpConfirmPassword) {
                  errorMessage = "Passwords do not match."
                  return@PrimaryGoldButton
                }
                if (signUpPassword.length < 6) {
                  errorMessage = "Password must be at least 6 characters."
                  return@PrimaryGoldButton
                }
                isLoading = true
                errorMessage = null
                scope.launch {
                   val result = userRepository.register(
                     name = signUpFullName,
                     email = signUpEmail,
                     password = signUpPassword
                   )
                  isLoading = false
                  if (result.isSuccess) {
                    onLoginSuccess(result.getOrThrow())
                  } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Account creation failed."
                  }
                }
              },
              enabled = !isLoading,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_submit_button")
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Already have an account?",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
              )
              TextButton(
                onClick = {
                  authMode = AuthMode.SIGN_IN
                  errorMessage = null
                }
              ) {
                Text(
                  text = "Sign in",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                  color = AmberGold
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

    }
  }

  // Forgot Password Dialog
  if (showForgotPasswordDialog) {
    AlertDialog(
      onDismissRequest = {
        showForgotPasswordDialog = false
        resetSuccessMessage = null
        resetErrorMessage = null
      },
      containerColor = DarkSurface,
      title = {
        Text("Reset Password", color = PureWhite, fontWeight = FontWeight.Bold)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            "Enter your email to receive password reset instructions.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
          )
          OutlinedTextField(
            value = resetEmail,
            onValueChange = { resetEmail = it },
            label = { Text("Email", color = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = TextPrimary,
              focusedBorderColor = AmberGold,
              unfocusedBorderColor = DarkBorder
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          if (resetSuccessMessage != null) {
            Text(
              resetSuccessMessage!!,
              color = EmeraldSuccess,
              style = MaterialTheme.typography.bodySmall
            )
          }
          if (resetErrorMessage != null) {
            Text(
              resetErrorMessage!!,
              color = RecRed,
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            resetErrorMessage = null
            resetSending = true
            scope.launch {
              val result = userRepository.sendPasswordReset(resetEmail)
              resetSending = false
              if (result.isSuccess) {
                resetSuccessMessage = "Check $resetEmail for the reset link."
              } else {
                resetErrorMessage = result.exceptionOrNull()?.message ?: "Could not send reset email."
              }
            }
          },
          enabled = !resetSending,
          colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianBlack)
        ) {
          Text(if (resetSending) "Sending..." else "Send Reset Link", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = {
          showForgotPasswordDialog = false
          resetSuccessMessage = null
          resetErrorMessage = null
        }) {
          Text("Close", color = TextSecondary)
        }
      }
    )
  }
}
@Composable
private fun GoogleSignInButton(
  userRepository: UserRepository,
  onSuccess: (User) -> Unit,
  onError: (String) -> Unit
) {
  // Hidden until a Google web client id is configured (see local.properties).
  if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) return
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var busy by remember { mutableStateOf(false) }

  OutlinedButton(
    onClick = {
      if (busy) return@OutlinedButton
      busy = true
      scope.launch {
        try {
          val nonce = UUID.randomUUID().toString()
          val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setNonce(nonce)
            .build()
          val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
          val result = CredentialManager.create(context).getCredential(context, request)
          val credential = result.credential
          if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
          ) {
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val signIn = userRepository.signInWithGoogle(idToken, nonce)
            if (signIn.isSuccess) onSuccess(signIn.getOrThrow())
            else onError(signIn.exceptionOrNull()?.message ?: "Google sign-in didn't work.")
          } else {
            onError("Google sign-in didn't work. Try email instead.")
          }
        } catch (e: GetCredentialException) {
          onError("No Google account found on this device.")
        } catch (e: Exception) {
          onError(e.message ?: "Google sign-in didn't work.")
        } finally {
          busy = false
        }
      }
    },
    enabled = !busy,
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("google_signin_button")
  ) {
    Icon(
      imageVector = Icons.Default.AccountCircle,
      contentDescription = null,
      tint = PureWhite,
      modifier = Modifier.size(20.dp)
    )
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      text = if (busy) "Connecting..." else "Continue with Google",
      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    )
  }
}
