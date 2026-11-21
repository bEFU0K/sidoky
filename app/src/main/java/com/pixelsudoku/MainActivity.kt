package com.pixelsudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.pixelsudoku.ui.PixelTheme
import com.pixelsudoku.ui.Root

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val s by vm.settings.collectAsState()
            PixelTheme(s.theme) { Root(vm) }
        }
    }

    override fun onStop() {
        super.onStop()
        vm.persist()
    }
}
