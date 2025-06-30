package com.example.android.circuitboardwarehouse

import android.app.backup.BackupAgentHelper
import android.app.backup.FileBackupHelper
import android.app.backup.SharedPreferencesBackupHelper

class WarehouseBackupAgent : BackupAgentHelper() {
    override fun onCreate() {
        addHelper("files", FileBackupHelper(this, "config.json"))
        addHelper("prefs", SharedPreferencesBackupHelper(this, "app_prefs"))
    }
}