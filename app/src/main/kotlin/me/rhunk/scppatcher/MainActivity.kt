package me.rhunk.scppatcher

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.scppatcher.ui.theme.ScpPatcherTheme
import me.rhunk.scppatcher.ui.theme.fontFamily

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val coroutineScope = rememberCoroutineScope()
            ScpPatcherTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding), contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.offset(y = (-90).dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                modifier = Modifier.size(250.dp),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "ScpPatcher",
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamily
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                fontFamily = fontFamily,
                                modifier = Modifier
                                    .padding(5.dp)
                                    .clickable {
                                        runCatching {
                                            startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://github.com/rhunk/ScpPatcher")
                                                ).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                })
                                        }.onFailure {
                                            Toast
                                                .makeText(
                                                    this@MainActivity,
                                                    "Failed to open link: ${it.message}",
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        }
                                    },
                                text = "https://github.com/rhunk/ScpPatcher",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Light,
                                textDecoration = TextDecoration.Underline
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                "Note: This tool will only work on rooted devices",
                                fontWeight = FontWeight.Light,
                                fontFamily = fontFamily,
                                fontStyle = FontStyle.Italic
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            var isLoading by remember { mutableStateOf(false) }

                            Button(
                                modifier = Modifier
                                    .padding(5.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorResource(id = R.color.ic_launcher_background),
                                    contentColor = Color.White
                                ),
                                onClick = {
                                    if (isLoading) return@Button
                                    isLoading = true
                                    coroutineScope.launch(Dispatchers.IO) {
                                        runCatching {
                                            CorePatcher(this@MainActivity).patchCore()
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Successfully patched Snapchat!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            startActivity(
                                                Intent.makeRestartActivityTask(
                                                    ComponentName(
                                                        "com.snapchat.android",
                                                        "com.snap.mushroom.MainActivity"
                                                    )
                                                ) ?: return@runCatching
                                            )
                                        }.onFailure {
                                            Log.e("ScpPatcher", "Failed to patch", it)

                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Failed to patch: ${it.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        withContext(Dispatchers.Main) {
                                            delay(500)
                                            isLoading = false
                                        }
                                    }
                                }
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Text(text = "Patch Snapchat", fontFamily = fontFamily, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
