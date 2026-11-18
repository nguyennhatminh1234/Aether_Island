package vn.aether.island

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity:ComponentActivity(){
    private val hasOverlay=mutableStateOf(false)
    private val hasMediaAccess=mutableStateOf(false)
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);refreshPermissions()
        setContent{
            MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFFB8C7FF),background=Color(0xFF090B10),surface=Color(0xFF151922))){
                Surface(Modifier.fillMaxSize(),color=Color(0xFF090B10)){
                    Column(Modifier.fillMaxSize().padding(22.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
                        Text("AETHER",color=Color(0xFFB8C7FF),fontSize=13.sp,letterSpacing=5.sp)
                        Text("Island",color=Color.White,fontSize=38.sp)
                        Text("Live activities · v0.2",color=Color(0xFF9BA4B5),fontSize=14.sp)
                        Spacer(Modifier.height(22.dp))
                        Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF151922))){
                            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                                Text(if(hasOverlay.value)"Overlay permission ready" else "Overlay permission required",color=Color.White)
                                Button(onClick={if(!Settings.canDrawOverlays(this@MainActivity))startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))else ContextCompat.startForegroundService(this@MainActivity,Intent(this@MainActivity,IslandOverlayService::class.java))},Modifier.fillMaxWidth()){
                                    Text(if(hasOverlay.value)"Start Island" else "Grant overlay permission")
                                }
                                OutlinedButton(onClick={startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))},Modifier.fillMaxWidth()){
                                    Text(if(hasMediaAccess.value)"Media access settings" else "Enable music detection")
                                }
                                Text("Music detection needs Notification Access. Android asks you to grant it manually.",color=Color(0xFF9BA4B5),fontSize=12.sp)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF151922))){
                            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                                Text("Test activities",color=Color.White,fontSize=17.sp)
                                Button(onClick={LiveActivityStore.startTimer(this@MainActivity,60)},Modifier.fillMaxWidth()){Text("Start 1-minute timer")}
                                OutlinedButton(onClick={LiveActivityStore.startStopwatch(this@MainActivity)},Modifier.fillMaxWidth()){Text("Start stopwatch")}
                                OutlinedButton(onClick={LiveActivityStore.stopTimer(this@MainActivity);LiveActivityStore.stopStopwatch(this@MainActivity);LiveActivityStore.remove(this@MainActivity,"music")},Modifier.fillMaxWidth()){Text("Clear test activities")}
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("v0.2 includes media-session detection, timer/stopwatch test activities and activity switching. Calls, audio-device and system-volume integrations are not included in this build yet.",color=Color(0xFF687386),fontSize=11.sp)
                        TextButton(onClick={stopService(Intent(this@MainActivity,IslandOverlayService::class.java))}){Text("Stop Island")}
                    }
                }
            }
        }
    }
    private fun refreshPermissions(){
        hasOverlay.value=Settings.canDrawOverlays(this)
        hasMediaAccess.value=android.provider.Settings.Secure.getString(contentResolver,"enabled_notification_listeners")?.contains(packageName)==true
    }
    override fun onResume(){super.onResume();refreshPermissions()}
}
