package org.example.umineko

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch


@Composable
fun LoginScreenDemo() {

    val vm = remember { AuthViewModel() }
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("123456") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            "LoginDemo",
            style = MaterialTheme.typography.headlineMedium
        )

        //  用户名输入框
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        //  密码输入框
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        //  按钮区
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    vm.login(username, password)
                }
            }) {
                Text("Login")
            }

            Button(onClick = {
                scope.launch {
                    vm.logout()
                }
            }) {
                Text("Logout")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    vm.profile()
                }
            }) {
                Text("Profile")
            }

            Button(onClick = {
                scope.launch {
                    vm.admin()
                }
            }) {
                Text("Admin")
            }
        }

        HorizontalDivider()

        Text("Result", style = MaterialTheme.typography.titleMedium)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp
        ) {
            val result by vm.result.collectAsState()
            Text(result)
        }
    }
}

