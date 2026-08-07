package com.ragnala.pos.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ragnala.pos.service.BackupService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Backup/Restore (PRD §9 Backup): create archives, list them, restore (destructive, confirmed). */
class BackupViewModel(
    private val backupService: BackupService,
) : ViewModel() {

    private val _backups = MutableStateFlow<List<File>>(emptyList())
    val backups: StateFlow<List<File>> = _backups.asStateFlow()

    private val _lastBackup = MutableStateFlow<File?>(null)
    val lastBackup: StateFlow<File?> = _lastBackup.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refreshList()
    }

    fun refreshList() {
        viewModelScope.launch {
            _backups.value = backupService.listBackups()
            _lastBackup.value = backupService.lastBackup()
        }
    }

    fun createBackup() {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                val file = backupService.createBackup()
                _message.value = "Backup created: ${file.name}"
                refreshList()
            } catch (e: Exception) {
                _error.value = e.message ?: "Backup failed"
            } finally {
                _busy.value = false
            }
        }
    }

    /** Restores from an archive. Destructive — the UI confirms first, then calls here. */
    fun restore(file: File) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                backupService.restore(file)
                _message.value = "Restored: ${file.name}"
            } catch (e: Exception) {
                _error.value = e.message ?: "Restore failed"
            } finally {
                _busy.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
    fun clearMessage() { _message.value = null }

    class Factory(
        private val backupService: BackupService,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BackupViewModel(backupService) as T
    }
}