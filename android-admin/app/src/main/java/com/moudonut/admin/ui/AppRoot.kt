package com.moudonut.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.moudonut.admin.data.BillsRepository
import com.moudonut.admin.data.SupabaseHolder
import com.moudonut.admin.data.toThaiMessage
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun AppRoot() {
    if (!SupabaseHolder.configured) {
        ConfigErrorScreen()
        return
    }

    val client = SupabaseHolder.client
    var loggedIn by remember { mutableStateOf(client.auth.currentSessionOrNull() != null) }
    var adminChecked by remember { mutableStateOf(false) }
    var adminError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        client.auth.sessionStatus.collect { status ->
            loggedIn = status is SessionStatus.Authenticated
        }
    }

    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            adminChecked = false
            adminError = null
            try {
                val isAdmin = withTimeout(8_000) { BillsRepository().isAdmin() }
                if (isAdmin) {
                    adminChecked = true
                } else {
                    adminError = "บัญชีนี้ไม่มีสิทธิ์แอดมิน"
                    withTimeoutOrNull(5_000) { client.auth.clearSession() }
                }
            } catch (t: Throwable) {
                adminError = if (t is TimeoutCancellationException) {
                    "ตรวจสอบสิทธิ์ไม่สำเร็จ กรุณาเข้าสู่ระบบใหม่"
                } else {
                    t.toThaiMessage()
                }
                withTimeoutOrNull(5_000) { client.auth.clearSession() }
            }
        } else {
            adminChecked = false
        }
    }

    when {
        loggedIn && adminChecked -> MainNav()
        loggedIn -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> LoginScreen(initialError = adminError)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainNav() {
    val nav = rememberNavController()
    val client = SupabaseHolder.client
    val scope = rememberCoroutineScope()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val current = backStackEntry?.destination

    fun go(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("หมู-บ้านโดนัท") },
                actions = {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                client.auth.signOut()
                            } catch (_: Throwable) {
                                // The session flow will return to login on its own.
                            }
                        }
                    }) {
                        Text("ออกจากระบบ")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = current?.hierarchy?.any { it.route == "bills" } == true,
                    onClick = { go("bills") },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("บิล") }
                )
                NavigationBarItem(
                    selected = current?.hierarchy?.any { it.route == "summary" } == true,
                    onClick = { go("summary") },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text("สรุป") }
                )
                NavigationBarItem(
                    selected = current?.hierarchy?.any { it.route == "customers" } == true,
                    onClick = { go("customers") },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("ลูกค้า") }
                )
                NavigationBarItem(
                    selected = current?.hierarchy?.any { it.route == "products" } == true,
                    onClick = { go("products") },
                    icon = { Icon(Icons.Default.Fastfood, contentDescription = null) },
                    label = { Text("สินค้า") }
                )
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = "bills",
            modifier = Modifier.padding(inner)
        ) {
            composable("bills") {
                BillsScreen(onOpen = { id -> nav.navigate("bill/$id") })
            }
            composable("bill/{id}") { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                BillDetailScreen(id = id, onBack = { nav.popBackStack() })
            }
            composable("summary") {
                SummaryScreen(onOpenBill = { id -> nav.navigate("bill/$id") })
            }
            composable("customers") {
                CustomersScreen()
            }
            composable("products") {
                ProductsScreen()
            }
        }
    }
}

@Composable
private fun ConfigErrorScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ยังไม่ได้ตั้งค่าแอพ", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "กรุณาใส่ SUPABASE_URL และ SUPABASE_ANON_KEY ใน android-admin/local.properties",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
