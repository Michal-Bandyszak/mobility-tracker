package com.mobilitytracker.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilitytracker.data.repository.BackupRepository
import com.mobilitytracker.data.repository.WorkoutRepository
import com.mobilitytracker.domain.usecase.ResetProgramFlowUseCase
import com.mobilitytracker.notification.AlarmScheduler
import com.mobilitytracker.notification.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val canScheduleExactAlarms: Boolean = true,
    val hasNotificationPermission: Boolean = true,
    val strengthDays: Set<DayOfWeek> = emptySet(),
    val strengthDayPlanAssignments: Map<DayOfWeek, Int> = emptyMap(),
    val themeMode: String = "system"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val backupRepository: BackupRepository,
    private val resetProgramFlow: ResetProgramFlowUseCase,
    private val alarmScheduler: AlarmScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val (enabled, hour, minute) = workoutRepository.getReminderSettings()
            val strengthDayPlanAssignments = workoutRepository.getStrengthDayPlanAssignments()
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
                hasNotificationPermission = hasNotifPermission,
                strengthDays = strengthDayPlanAssignments.keys,
                strengthDayPlanAssignments = strengthDayPlanAssignments,
                themeMode = workoutRepository.getThemeMode()
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
            workoutTitle = "Jeśli to widzisz — powiadomienia działają!"
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

    fun toggleStrengthDay(day: DayOfWeek) {
        viewModelScope.launch {
            val current = _state.value.strengthDayPlanAssignments
            val currentPlan = current[day]
            val updated = when (currentPlan) {
                null -> current + (day to 1)
                1 -> current + (day to 2)
                else -> current - day
            }
            workoutRepository.saveStrengthDayPlanAssignments(updated)
            _state.value = _state.value.copy(
                strengthDays = updated.keys,
                strengthDayPlanAssignments = updated
            )
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            workoutRepository.saveThemeMode(mode)
            _state.value = _state.value.copy(themeMode = mode)
        }
    }

    fun resetFlow() {
        viewModelScope.launch {
            resetProgramFlow()
        }
    }

    fun exportData(onDataReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = backupRepository.exportDataToJson()
            onDataReady(json)
        }
    }

    fun importData(json: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = backupRepository.importDataFromJson(json)
            onComplete(result.isSuccess)
        }
    }
}
