package com.mobilitytracker.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilitytracker.data.repository.WorkoutRepository
import com.mobilitytracker.notification.AlarmScheduler
import com.mobilitytracker.notification.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val canScheduleExactAlarms: Boolean = true,
    val hasNotificationPermission: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val alarmScheduler: AlarmScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val (enabled, hour, minute) = workoutRepository.getReminderSettings()
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val canSchedule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else true
            val hasNotifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            } else true
            _state.value = SettingsUiState(
                isLoading = false,
                reminderEnabled = enabled,
                reminderHour = hour,
                reminderMinute = minute,
                canScheduleExactAlarms = canSchedule,
                hasNotificationPermission = hasNotifPermission
            )
        }
    }

    fun refreshPermissions() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canSchedule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true
        val hasNotifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true
        _state.value = _state.value.copy(
            canScheduleExactAlarms = canSchedule,
            hasNotificationPermission = hasNotifPermission
        )
    }

    fun sendTestNotification() {
        NotificationHelper.createChannel(context)
        NotificationHelper.showReminder(
            context = context,
            title = "Test powiadomienia",
            workoutTitle = "Jeśli to widzisz — powiadomienia działają!",
            cycleNumber = 0,
            videoOrder = 0
        )
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val state = _state.value
            workoutRepository.saveReminderSettings(enabled, state.reminderHour, state.reminderMinute)
            _state.value = state.copy(reminderEnabled = enabled)
            if (enabled) {
                alarmScheduler.scheduleDaily(state.reminderHour, state.reminderMinute)
            } else {
                alarmScheduler.cancel()
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val state = _state.value
            workoutRepository.saveReminderSettings(state.reminderEnabled, hour, minute)
            _state.value = state.copy(reminderHour = hour, reminderMinute = minute)
            if (state.reminderEnabled) {
                alarmScheduler.scheduleDaily(hour, minute)
            }
        }
    }
}
