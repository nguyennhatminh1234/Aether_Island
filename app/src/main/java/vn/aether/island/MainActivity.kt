package vn.aether.island

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val hasOverlay = remember { mutableStateOf(Settings.canDrawOverlays(this)) }
            DisposableEffect(Unit) {
                onDispose { }
            }

            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFFB8C7FF),
                background = Color(0xFF090B10),
                surface = Color(0xFF151922)
            )) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF090B10)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("AETHER", color = Color(0xFFB8C7FF), fontSize = 13.sp, letterSpacing = 5.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Island", color = Color.White, fontSize = 38.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "A quiet space for what's happening on your phone.",
                            color = Color(0xFF9BA4B5),
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(34.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            color = Color(0xFF151922)
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text("v0.1 · Island Core", color = Color.White, fontSize = 17.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (hasOverlay.value) "Overlay permission is ready." else "Allow display over other apps to show the Island.",
                                    color = Color(0xFF9BA4B5),
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.height(18.dp))
                                Button(
                                    onClick = {
                                        if (!Settings.canDrawOverlays(this@MainActivity)) {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:$packageName")
                                            )
                                            startActivity(intent)
                                        } else {
                                            ContextCompat.startForegroundService(
                                                this@MainActivity,
                                                Intent(this@MainActivity, IslandOverlayService::class.java)
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB8C7FF))
                                ) {
                                    Text(
                                        if (hasOverlay.value) "Start Island" else "Grant overlay permission",
                                        color = Color(0xFF101522)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        stopService(Intent(this@MainActivity, IslandOverlayService::class.java))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29303D))
                                ) {
                                    Text("Stop Island", color = Color.White)
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "v0.1 is the interaction foundation. Live activities and notifications arrive in later versions.",
                            color = Color(0xFF687386),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recreate the screen so the permission status is refreshed after returning from Settings.
        window.decorView.post {
            recreateIfPermissionChanged()
        }
    }

    private fun recreateIfPermissionChanged() {
        // Compose reads the permission state when the activity is created.
        // Avoid repeated recreation; the Start button also checks permission directly.
    }
}
