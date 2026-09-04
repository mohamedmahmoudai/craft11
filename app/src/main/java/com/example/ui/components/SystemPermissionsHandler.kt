package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.alarm.FocusAlarmManager
import com.example.util.BatteryOptimizationHelper

@Composable
fun SystemPermissionsHandler(
    onPermissionsVerified: () -> Unit = {}
) {
    val context = LocalContext.current
    var pendingPermissionDialog by remember { mutableStateOf<PermissionType?>(null) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }

    // Check if notification permission is needed (Android 13+)
    val hasNotificationPermission = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingPermissionDialog = null
            // Check exact alarms if needed
            checkExactAlarmPermission(context) { needed ->
                if (needed) pendingPermissionDialog = PermissionType.EXACT_ALARM
                else onPermissionsVerified()
            }
        } else {
            isPermanentlyDenied = true
        }
    }

    LaunchedEffect(Unit) {
        // First check Notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            pendingPermissionDialog = PermissionType.NOTIFICATION
        } else {
            // Check exact alarms
            checkExactAlarmPermission(context) { needed ->
                if (needed) pendingPermissionDialog = PermissionType.EXACT_ALARM
                else onPermissionsVerified()
            }
        }
    }

    pendingPermissionDialog?.let { type ->
        PermissionRationaleDialog(
            permissionType = type,
            isPermanentlyDenied = isPermanentlyDenied,
            onConfirm = {
                when (type) {
                    PermissionType.NOTIFICATION -> {
                        if (isPermanentlyDenied) {
                            BatteryOptimizationHelper.openAppSettings(context)
                            pendingPermissionDialog = null
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            pendingPermissionDialog = null
                        }
                    }
                    PermissionType.EXACT_ALARM -> {
                        BatteryOptimizationHelper.openExactAlarmSettings(context)
                        pendingPermissionDialog = null
                    }
                    PermissionType.BATTERY_OPTIMIZATION -> {
                        BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                        pendingPermissionDialog = null
                    }
                }
            },
            onDismiss = {
                pendingPermissionDialog = null
            }
        )
    }
}

private fun checkExactAlarmPermission(context: Context, onResult: (Boolean) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
        val canSchedule = alarmManager?.canScheduleExactAlarms() ?: true
        onResult(!canSchedule)
    } else {
        onResult(false)
    }
}
