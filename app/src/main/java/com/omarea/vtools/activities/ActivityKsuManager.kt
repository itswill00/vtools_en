package com.omarea.vtools.activities

import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omarea.common.ui.DialogHelper
import com.omarea.vtools.R
import com.omarea.vtools.utils.ModuleInfo
import com.omarea.vtools.utils.ModuleUtils
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class ActivityKsuManager : ActivityBase() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val composeView = ComposeView(this)
        setContentView(composeView)

        composeView.setContent {
            val isDark = isSystemInDarkTheme()
            val controller = remember {
                ThemeController(if (isDark) ColorSchemeMode.Dark else ColorSchemeMode.Light)
            }
            
            MiuixTheme(controller = controller) {
                KsuManagerScreen(isDark)
            }
        }
    }

    @Composable
    fun KsuManagerScreen(isDark: Boolean) {
        var modules by remember { mutableStateOf(emptyList<ModuleInfo>()) }
        var isLoading by remember { mutableStateOf(true) }
        val textColor = if (isDark) Color.White else MiuixTheme.colorScheme.onSurface

        LaunchedEffect(Unit) {
            modules = ModuleUtils.getInstalledModules()
            isLoading = false
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
        ) {
            // Unified Glass Header with existing close icon
            HeaderSection(isDark, onBack = { finish() }, onRefresh = {
                isLoading = true
                modules = ModuleUtils.getInstalledModules()
                isLoading = false
            })

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MiuixTheme.colorScheme.primary, strokeWidth = 3.dp)
                }
            } else if (modules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No local modules found", color = textColor.copy(alpha = 0.5f), style = MiuixTheme.textStyles.body2)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(modules) { module ->
                        ModuleCard(module, isDark, onUpdate = {
                            modules = ModuleUtils.getInstalledModules()
                            Toast.makeText(this@ActivityKsuManager, "Reboot required to apply changes", Toast.LENGTH_SHORT).show()
                        })
                    }
                }
            }
        }
    }

    @Composable
    fun HeaderSection(isDark: Boolean, onBack: () -> Unit, onRefresh: () -> Unit) {
        val textColor = if (isDark) Color.White else MiuixTheme.colorScheme.onSurface
        val iconColor = if (isDark) Color.White.copy(alpha = 0.9f) else MiuixTheme.colorScheme.onSurface

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(Color.Transparent)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.close), 
                            contentDescription = "Back", 
                            tint = iconColor, 
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "KernelSU Modules",
                        style = MiuixTheme.textStyles.title3,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                    Icon(painterResource(R.drawable.app_options_restore), contentDescription = "Refresh", tint = MiuixTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.4.dp)
                    .background(textColor.copy(alpha = 0.12f))
            )
        }
    }

    @Composable
    fun ModuleCard(module: ModuleInfo, isDark: Boolean, onUpdate: () -> Unit) {
        val textColor = if (isDark) Color.White else MiuixTheme.colorScheme.onSurface
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            cornerRadius = 14.dp,
            colors = CardDefaults.defaultColors()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = module.name,
                            style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            color = if (module.isEnabled) textColor else textColor.copy(alpha = 0.5f),
                            maxLines = 1
                        )
                        Text(
                            text = "${module.version} • ${module.author}",
                            style = MiuixTheme.textStyles.footnote2,
                            color = textColor.copy(alpha = 0.45f),
                            maxLines = 1
                        )
                    }
                    
                    Switch(
                        checked = module.isEnabled,
                        onCheckedChange = { 
                            ModuleUtils.toggleModule(module.path, it)
                            onUpdate()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MiuixTheme.colorScheme.primary,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.scale(0.75f)
                    )
                }
                
                if (module.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = module.description,
                        style = MiuixTheme.textStyles.footnote2,
                        color = textColor.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (module.isRemoved) {
                        Text("TO BE REMOVED", color = Color.Red.copy(alpha = 0.8f), style = MiuixTheme.textStyles.footnote2, fontWeight = FontWeight.Bold)
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    DialogHelper.confirm(this@ActivityKsuManager, "Remove Module", "Are you sure you want to remove ${module.name}?") {
                                        ModuleUtils.removeModule(module.path)
                                        onUpdate()
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "DELETE", 
                                color = Color.Red.copy(alpha = 0.5f), 
                                style = MiuixTheme.textStyles.footnote2, 
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    
                    Text(
                        text = module.id,
                        style = MiuixTheme.textStyles.footnote2,
                        color = textColor.copy(alpha = 0.25f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
