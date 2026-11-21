package com.pixelsudoku.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PText(
    text: String, size: Int = 16, color: Color = LocalPixel.current.ink,
    align: TextAlign? = null, modifier: Modifier = Modifier
) {
    Text(
        text, modifier, color = color, fontSize = size.sp, fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold, textAlign = align
    )
}

@Composable
fun PixelButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, selected: Boolean = false,
    color: Color = LocalPixel.current.accent, size: Int = 16, hpad: Int = 14
) {
    val c = LocalPixel.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val bg = when {
        !enabled -> c.inkSoft.copy(alpha = 0.35f)
        selected -> c.accent2
        else -> color
    }
    Box(modifier.padding(end = 4.dp, bottom = 4.dp)) {
        Box(
            Modifier
                .offset(y = if (pressed) 3.dp else 0.dp)
                .drawBehind {
                    if (!pressed) drawRect(c.border, topLeft = Offset(4.dp.toPx(), 4.dp.toPx()), size = this.size)
                }
                .background(bg)
                .border(3.dp, c.border)
                .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
                .padding(horizontal = hpad.dp, vertical = 10.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            PText(text, size, color = Color.White, align = TextAlign.Center)
        }
    }
}

@Composable
fun PixelPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalPixel.current
    Column(modifier.background(c.panel).border(3.dp, c.border).padding(12.dp), content = content)
}

@Composable
fun ScreenFrame(
    title: String, onBack: (() -> Unit)?, scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = LocalPixel.current
    Column(
        Modifier.fillMaxSize().background(c.bg).statusBarsPadding().navigationBarsPadding().padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                PixelButton("<", onBack, Modifier.width(64.dp), hpad = 0)
                Spacer(Modifier.width(8.dp))
            }
            PText(title, 22)
        }
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.weight(1f).fillMaxWidth()
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

fun fmtTime(sec: Int): String = "%02d:%02d".format(sec / 60, sec % 60)
