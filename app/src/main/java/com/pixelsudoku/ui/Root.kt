package com.pixelsudoku.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pixelsudoku.AppViewModel
import com.pixelsudoku.Screen
import kotlinx.coroutines.delay

@Composable
fun Root(vm: AppViewModel) {
    val c = LocalPixel.current
    BackHandler(enabled = vm.screen != Screen.HOME) { vm.back() }
    LaunchedEffect(vm.toast) { if (vm.toast != null) { delay(2200); vm.toast = null } }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Crossfade(vm.screen, label = "nav") { s ->
            when (s) {
                Screen.HOME -> HomeScreen(vm)
                Screen.NEW -> NewGameScreen(vm)
                Screen.LEVELS -> LevelsScreen(vm)
                Screen.GAME -> GameScreen(vm)
                Screen.WIN -> WinScreen(vm)
                Screen.RESULTS -> ResultsScreen(vm)
                Screen.STATS -> StatsScreen(vm)
                Screen.SETTINGS -> SettingsScreen(vm)
            }
        }
        if (vm.loading) {
            Box(
                Modifier.fillMaxSize().background(Color(0xAA000000)).clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) { PixelPanel { PText("Генерация головоломки...", 16) } }
        }
        vm.toast?.let {
            Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 24.dp), contentAlignment = Alignment.BottomCenter) {
                Box(Modifier.background(c.ink).border(3.dp, c.border).padding(12.dp)) { PText(it, 14, color = c.bg) }
            }
        }
        vm.confirmStart?.let { go ->
            ConfirmDialog(
                "Начать новую партию?", "Текущая незавершённая партия будет потеряна.",
                "Начать", { go() }, { vm.confirmStart = null }
            )
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, ok: String, onOk: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onOk) { Text(ok) } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Отмена") } }
    )
}
