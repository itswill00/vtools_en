package com.omarea.vtools.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixHeader(
    title: String,
    onBack: (() -> Unit)? = null
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else MiuixTheme.colorScheme.onSurface
    val iconColor = if (isDark) Color.White.copy(alpha = 0.9f) else MiuixTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.75f, size.height * 0.15f)
                            lineTo(size.width * 0.25f, size.height * 0.5f)
                            lineTo(size.width * 0.75f, size.height * 0.85f)
                        }
                        drawPath(
                            path = path,
                            color = iconColor,
                            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            Text(
                text = title,
                style = MiuixTheme.textStyles.title2,
                color = textColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = if (onBack == null) 0.dp else 4.dp)
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(textColor.copy(alpha = 0.08f)))
    }
}

@Composable
fun MiuixSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MiuixTheme.textStyles.footnote2,
        fontWeight = FontWeight.Bold,
        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp, top = 16.dp)
    )
}

@Composable
fun MiuixPropertyRow(
    label: String, 
    value: String, 
    iconRes: Int? = null,
    onClick: (() -> Unit)? = null,
    color: Color? = null
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable { onClick() }
    } else {
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Medium,
                color = color ?: MiuixTheme.colorScheme.onSurfaceContainerVariant
            )
            if (onClick != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    painter = painterResource(com.omarea.vtools.R.drawable.icon_more),
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceContainerVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun MiuixSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(content = content)
    }
}
