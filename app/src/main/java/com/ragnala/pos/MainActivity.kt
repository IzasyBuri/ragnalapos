package com.ragnala.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ragnala.pos.BuildConfig
import com.ragnala.pos.service.PinRole
import com.ragnala.pos.service.PinService
import com.ragnala.pos.ui.AppGraph
import com.ragnala.pos.ui.RagnalaApp
import com.ragnala.pos.ui.theme.RagnalaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dev convenience: on debug builds, seed a default Barista PIN so the PIN gate
        // isn't a dead end during development. Release builds stay secure (PRD §9) —
        // a real PIN must be set via Settings. Never overwrites an existing PIN.
        if (BuildConfig.DEBUG) {
            CoroutineScope(Dispatchers.IO).launch {
                val pinService = AppGraph.pinService(applicationContext)
                if (!pinService.isSet(PinRole.BARISTA)) {
                    pinService.setPin(PinRole.BARISTA, "1234", currentPin = null, userLabel = "dev default")
                }
                // Dev default Owner PIN (PRD §9 role enforcement). Release builds stay secure.
                if (!pinService.isSet(PinRole.OWNER)) {
                    pinService.setPin(PinRole.OWNER, "9999", currentPin = null, userLabel = "dev default")
                }
            }
        }
        setContent {
            RagnalaTheme {
                RagnalaApp()
            }
        }
    }
}
