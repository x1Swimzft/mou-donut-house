package com.moudonut.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.moudonut.admin.data.CredentialStore
import com.moudonut.admin.data.SupabaseHolder
import com.moudonut.admin.data.toThaiMessage
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(initialError: String? = null) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember(initialError) { mutableStateOf(initialError) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val client = SupabaseHolder.client

    LaunchedEffect(Unit) {
        CredentialStore.load(context)?.let { (savedEmail, savedPassword) ->
            email = savedEmail
            password = savedPassword
            rememberPassword = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("หมู-บ้านโดนัท", style = MaterialTheme.typography.headlineLarge)
        Text("เข้าสู่ระบบแอดมิน", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                error = null
            },
            label = { Text("อีเมล") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                error = null
            },
            label = { Text("รหัสผ่าน") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = rememberPassword,
                    role = Role.Checkbox,
                    onValueChange = { checked ->
                        rememberPassword = checked
                        if (!checked) CredentialStore.clear(context)
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = rememberPassword, onCheckedChange = null)
            Text("จดจำรหัส")
        }

        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (loading) return@Button
                loading = true
                error = null
                scope.launch {
                    try {
                        client.auth.signInWith(Email) {
                            this.email = email.trim()
                            this.password = password
                        }
                        if (rememberPassword) {
                            runCatching {
                                CredentialStore.save(context, email.trim(), password)
                            }
                        } else {
                            CredentialStore.clear(context)
                        }
                    } catch (t: Throwable) {
                        error = t.toThaiMessage()
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "กำลังเข้าสู่ระบบ..." else "เข้าสู่ระบบ")
        }
    }
}
