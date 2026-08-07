package com.ragnala.pos.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ragnala.pos.service.PinRole
import com.ragnala.pos.service.PinService
import com.ragnala.pos.service.SettingsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Management screen (PRD §9): owner-gated settings.
 * Owner PIN (dev default 9999) unlocks PIN changes and store settings.
 */
class ManagementViewModel(
    private val settingsService: SettingsService,
    private val pinService: PinService,
) : ViewModel() {

    private val _ownerVerified = MutableStateFlow(false)
    val ownerVerified: StateFlow<Boolean> = _ownerVerified

    // retained owner PIN after successful verification (used for owner-override PIN changes)
    private var ownerPinValue: String = ""

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    // store settings mirrors
    private val _storeName = MutableStateFlow("")
    val storeName: StateFlow<String> = _storeName
    private val _scPercent = MutableStateFlow(5.0)
    val scPercent: StateFlow<Double> = _scPercent
    private val _taxPercent = MutableStateFlow(11.0)
    val taxPercent: StateFlow<Double> = _taxPercent

    // QRIS payment image (absolute file path, empty = none)
    private val _qrisImagePath = MutableStateFlow<String?>(null)
    val qrisImagePath: StateFlow<String?> = _qrisImagePath

    fun setQrisImagePath(path: String?) {
        _qrisImagePath.value = path
        viewModelScope.launch { settingsService.setQrisImagePath(path ?: "") }
    }

    fun verifyOwner(pin: String) {
        viewModelScope.launch {
            _error.value = null
            if (pinService.verify(PinRole.OWNER, pin)) {
                _ownerVerified.value = true
                ownerPinValue = pin
                loadSettings()
            } else {
                _error.value = "Incorrect owner PIN"
            }
        }
    }

    private suspend fun loadSettings() {
        _storeName.value = settingsService.storeName()
        _scPercent.value = settingsService.serviceChargePercent()
        _taxPercent.value = settingsService.taxPercent()
        _qrisImagePath.value = settingsService.qrisImagePath().ifBlank { null }
    }

    fun setStoreName(value: String) { _storeName.value = value }
    fun setScPercent(value: Double) { _scPercent.value = value }
    fun setTaxPercent(value: Double) { _taxPercent.value = value }

    fun saveStoreSettings() {
        viewModelScope.launch {
            _error.value = null
            try {
                settingsService.setStoreName(_storeName.value)
                settingsService.setServiceChargePercent(_scPercent.value)
                settingsService.setTaxPercent(_taxPercent.value)
                _message.value = "Settings saved"
            } catch (e: Exception) {
                _error.value = e.message ?: "Save failed"
            }
        }
    }

    fun changeBaristaPin(newPin: String) {
        changePin(PinRole.BARISTA, newPin)
    }

    fun changeOwnerPin(newPin: String) {
        changePin(PinRole.OWNER, newPin)
    }

    private fun changePin(role: PinRole, newPin: String) {
        if (!_ownerVerified.value) {
            _error.value = "Owner PIN required"
            return
        }
        viewModelScope.launch {
            _error.value = null
            try {
                pinService.setPinAsOwner(role, newPin, ownerPin = ownerPinValue, userLabel = "owner")
                _message.value = "${role.name} PIN updated"
            } catch (e: Exception) {
                _error.value = e.message ?: "PIN change failed"
            }
        }
    }

    fun clearMessage() { _message.value = null }
    fun clearError() { _error.value = null }
}

class ManagementViewModelFactory(
    private val settingsService: SettingsService,
    private val pinService: PinService,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ManagementViewModel(settingsService, pinService) as T
}
