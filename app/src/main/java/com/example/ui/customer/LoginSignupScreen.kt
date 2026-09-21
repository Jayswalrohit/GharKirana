package com.example.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GharKiranaBrandLogo
import com.example.ui.theme.*
import com.example.ui.viewmodel.GroceryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginSignupScreen(
    viewModel: GroceryViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSignUpMode by remember { mutableStateOf(false) }

    // Customer Login Fields
    var loginIdentifier by remember { mutableStateOf("9841001122") }
    var loginPassword by remember { mutableStateOf("kirana123") }
    var showLoginPassword by remember { mutableStateOf(false) }

    // Customer Registration Fields
    var regFullName by remember { mutableStateOf("") }
    var regMobile by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regAddress by remember { mutableStateOf("") }
    var showRegPassword by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPhoneOrEmail by remember { mutableStateOf("") }
    var forgotSentSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isSignUpMode) "ग्राहक दर्ता / Create Account" else "ग्राहक लगइन / Customer Login",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // GharKirana Authentic Brand Logo
            GharKiranaBrandLogo(
                showTagline = true,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Auth Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (!isSignUpMode) {
                        // ==================== LOGIN FORM ====================
                        Text(
                            text = "स्वागत छ / Welcome to GharKirana",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KiranaGreenDark
                        )
                        Text(
                            text = "Sign in to order fresh groceries with doorstep delivery.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Phone or Email
                        OutlinedTextField(
                            value = loginIdentifier,
                            onValueChange = {
                                loginIdentifier = it
                                errorMessage = null
                            },
                            label = { Text("Phone Number or Email") },
                            placeholder = { Text("e.g. 9841XXXXXX or user@nepal.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = KiranaGreenPrimary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_login_identifier")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password
                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = {
                                loginPassword = it
                                errorMessage = null
                            },
                            label = { Text("Password") },
                            placeholder = { Text("Enter your password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = KiranaGreenPrimary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                    Icon(
                                        imageVector = if (showLoginPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_login_password")
                        )

                        // Forgot Password Link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    forgotPhoneOrEmail = loginIdentifier
                                    forgotSentSuccess = false
                                    showForgotPasswordDialog = true
                                }
                            ) {
                                Text(
                                    text = "Forgot password?",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = KiranaGreenDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (errorMessage != null) {
                            Surface(
                                color = ErrorRedLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = ErrorRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        // Login Button
                        Button(
                            onClick = {
                                if (loginIdentifier.isBlank() || loginPassword.isBlank()) {
                                    errorMessage = "Please provide both your phone/email and password."
                                    return@Button
                                }
                                val ok = viewModel.loginCustomerWithCredentials(loginIdentifier, loginPassword)
                                if (ok) {
                                    onSuccess()
                                } else {
                                    errorMessage = "Login failed! Please check your credentials."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("customer_login_submit_btn")
                        ) {
                            Text("लगइन गर्नुहोस् • Log In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Switch to Registration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Don't have an account?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = {
                                isSignUpMode = true
                                errorMessage = null
                            }) {
                                Text(
                                    text = "Create Account",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = KiranaGreenDark
                                )
                            }
                        }
                    } else {
                        // ==================== REGISTRATION FORM ====================
                        Text(
                            text = "नयाँ खाता खोल्नुहोस् / Register New Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KiranaGreenDark
                        )
                        Text(
                            text = "Join GharKirana to order everyday kitchen and grocery staples.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Full Name
                        OutlinedTextField(
                            value = regFullName,
                            onValueChange = { regFullName = it; errorMessage = null },
                            label = { Text("Full Name *") },
                            placeholder = { Text("e.g. Sita Shrestha") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = KiranaGreenPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("reg_fullname")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Mobile Number
                        OutlinedTextField(
                            value = regMobile,
                            onValueChange = { regMobile = it; errorMessage = null },
                            label = { Text("Mobile Number *") },
                            placeholder = { Text("e.g. 9841234567") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = KiranaGreenPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("reg_mobile")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Email
                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it; errorMessage = null },
                            label = { Text("Email Address") },
                            placeholder = { Text("e.g. sita@nepal.com") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = KiranaGreenPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("reg_email")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Password
                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it; errorMessage = null },
                            label = { Text("Password *") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = KiranaGreenPrimary) },
                            trailingIcon = {
                                IconButton(onClick = { showRegPassword = !showRegPassword }) {
                                    Icon(
                                        imageVector = if (showRegPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("reg_password")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Confirm Password
                        OutlinedTextField(
                            value = regConfirmPassword,
                            onValueChange = { regConfirmPassword = it; errorMessage = null },
                            label = { Text("Confirm Password *") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = KiranaGreenPrimary) },
                            visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("reg_confirm_password")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Delivery Address
                        OutlinedTextField(
                            value = regAddress,
                            onValueChange = { regAddress = it; errorMessage = null },
                            label = { Text("Delivery Address (Chowk, City) *") },
                            placeholder = { Text("e.g. Bhanu Chowk, Janakpur Dham") },
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = KiranaGreenPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("reg_address")
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = ErrorRedLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = ErrorRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Register Button
                        Button(
                            onClick = {
                                if (regFullName.isBlank()) {
                                    errorMessage = "Please enter your full name."
                                    return@Button
                                }
                                if (regMobile.isBlank()) {
                                    errorMessage = "Please enter your mobile phone number."
                                    return@Button
                                }
                                if (regPassword.length < 4) {
                                    errorMessage = "Password must be at least 4 characters long."
                                    return@Button
                                }
                                if (regPassword != regConfirmPassword) {
                                    errorMessage = "Passwords do not match!"
                                    return@Button
                                }
                                if (regAddress.isBlank()) {
                                    errorMessage = "Please provide your delivery address or chowk."
                                    return@Button
                                }

                                val success = viewModel.registerCustomerAccount(
                                    name = regFullName,
                                    phone = regMobile,
                                    email = regEmail.ifBlank { "${regMobile}@customer.gharkirana.np" },
                                    password = regPassword,
                                    address = regAddress
                                )
                                if (success) {
                                    onSuccess()
                                } else {
                                    errorMessage = "Registration could not be completed. Please try again."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("customer_register_submit_btn")
                        ) {
                            Text("खाता खोल्नुहोस् • Register Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Switch to Login
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Already have an account?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = {
                                isSignUpMode = false
                                errorMessage = null
                            }) {
                                Text(
                                    text = "Log In",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = KiranaGreenDark
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Text("पासवर्ड रिसेट / Reset Password", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    if (forgotSentSuccess) {
                        Text(
                            text = "✅ Verification SMS code sent to $forgotPhoneOrEmail! Check your message inbox to reset your password.",
                            color = SuccessGreen,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "Enter your registered Nepali mobile number or email address. We will send an SMS reset code.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = forgotPhoneOrEmail,
                            onValueChange = { forgotPhoneOrEmail = it },
                            label = { Text("Phone or Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (forgotSentSuccess) {
                    Button(onClick = { showForgotPasswordDialog = false }) {
                        Text("Close")
                    }
                } else {
                    Button(
                        onClick = {
                            if (forgotPhoneOrEmail.isNotBlank()) {
                                forgotSentSuccess = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary)
                    ) {
                        Text("Send Reset Code")
                    }
                }
            },
            dismissButton = {
                if (!forgotSentSuccess) {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
