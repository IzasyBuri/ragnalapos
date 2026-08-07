package com.ragnala.pos.ui.barista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ragnala.pos.service.PinRole
import com.ragnala.pos.service.PinService
import com.ragnala.pos.service.SettingsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Drives the Barista Mode unlock gate (PRD §9 Role Enforcement).
 *
 * Two ways to enter Barista Mode:
 *  1. Enter the correct BARISTA PIN (session unlock — stays open until app restart).
 *  2. Owner has disabled the PIN for today (barista_pin_disabled_date == today):
 *     anyone can enter without a PIN.
 *
 * The daily disable toggle itself requires the OWNER PIN to flip (so a customer
 * cannot simply disable it).
 */
class BaristaUnlockViewModel(
    private val pinService: PinService,
    private val settingsService: SettingsService,
) : ViewModel() {

    private val _pin = MutableStateFlow("")
    val pin: StateFlow<String> = _pin

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** Whether the PIN is disabled for today (owner decision). */
    private val _pinDisabledToday = MutableStateFlow(false)
    val pinDisabledToday: StateFlow<Boolean> = _pinDisabledToday

    /** True while verifying an owner PIN for the disable/enable toggle. */
    private val _ownerBusy = MutableStateFlow(false)
    val ownerBusy: StateFlow<Boolean> = _ownerBusy

    /** Whether the unlock succeeded (drives navigation into Barista queue). */
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked

    init {
        viewModelScope.launch {
            val today = todayString()
            _pinDisabledToday.value = settingsService.baristaPinDisabledDate() == today
        }
    }

    fun onPinChange(value: String) {
        _pin.value = value.filter { it.isDigit() }.take(6)
        _error.value = null
    }

    /** Attempts to unlock the Barista session. Result surfaced via [unlocked]/[error]. */
    fun tryUnlock() {
        if (_unlocked.value) return
        viewModelScope.launch {
            val today = todayString()
            // Path A: owner disabled PIN for today
            if (settingsService.baristaPinDisabledDate() == today) {
                _pinDisabledToday.value = true
                _error.value = null
                _unlocked.value = true
                return@launch
            }
            // Path B: correct barista PIN
            if (pinService.verify(PinRole.BARISTA, _pin.value)) {
                _error.value = null
                _unlocked.value = true
            } else {
                _error.value = "Incorrect PIN"
            }
        }
    }

    /** Owner-gated toggle of the daily disable flag. */
    fun togglePinDisabledForToday(ownerPin: String) {
        viewModelScope.launch {
            if (!pinService.verify(PinRole.OWNER, ownerPin)) {
                _error.value = "Incorrect Owner PIN"
                return@launch
            }
            _ownerBusy.value = true
            try {
                val today = todayString()
                if (_pinDisabledToday.value) {
                    settingsService.clearBaristaPinDisabled()
                    _pinDisabledToday.value = false
                } else {
                    settingsService.setBaristaPinDisabledDate(today)
                    _pinDisabledToday.value = true
                }
            } finally {
                _ownerBusy.value = false
            }
        }
    }

    fun clearError() { _error.value = null }

    private fun todayString(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Calendar.getInstance().time)
    }

    class Factory(
        private val pinService: PinService,
        private val settingsService: SettingsService,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BaristaUnlockViewModel(pinService, settingsService) as T
    }
}
