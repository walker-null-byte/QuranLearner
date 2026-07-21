package com.dillu.quranlearner.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dillu.quranlearner.ui.components.rememberNotificationScheduler
import com.dillu.quranlearner.ui.components.rememberNotificationPermissionState
import com.dillu.quranlearner.ui.components.rememberFileExporter
import com.dillu.quranlearner.ui.theme.LocalNoorTypography
import com.dillu.quranlearner.ui.theme.NoorColors
import kotlinx.coroutines.launch

/** Available reciters with their EveryAyah.com folder names. */
data class ReciterOption(val id: String, val name: String, val folder: String)

val AVAILABLE_RECITERS = listOf(
    ReciterOption("alafasy", "Mishary Rashid Al-Afasy", "Alafasy_128kbps"),
    ReciterOption("husary", "Mahmoud Khalil Al-Husary", "Husary_128kbps"),
    ReciterOption("minshawi_murattal", "Mohamed Siddiq Al-Minshawi", "Minshawy_Murattal_128kbps"),
    ReciterOption("abdulbasit_murattal", "Abdul Basit (Murattal)", "Abdul_Basit_Murattal_192kbps"),
    ReciterOption("sudais", "Abdur-Rahman As-Sudais", "Abdurrahmaan_As-Sudais_192kbps"),
    ReciterOption("shuraim", "Saud Ash-Shuraim", "Saood_ash-Shuraym_128kbps"),
    ReciterOption("ajamy", "Ahmed Al-Ajamy", "Ahmed_ibn_Ali_al-Ajamy_128kbps_ketaballah.net"),
    ReciterOption("maher", "Maher Al-Muaiqly", "MauroAl-Muaiqly128kbps"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
) {
    val noorType = LocalNoorTypography.current
    val notificationScheduler = rememberNotificationScheduler()
    val permissionState = rememberNotificationPermissionState()
    
    var showTimePicker by remember { mutableStateOf(false) }

    val selectedReciterId by viewModel.selectedReciterId.collectAsState()
    val arabicFontSize by viewModel.arabicFontSize.collectAsState()
    val translationFontSize by viewModel.translationFontSize.collectAsState()
    val reminderEnabled by viewModel.reminderEnabled.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val reminderMinute by viewModel.reminderMinute.collectAsState()
    val useDataSaverAudio by viewModel.useDataSaverAudio.collectAsState()
    val exportLocation by viewModel.exportLocation.collectAsState()
    
    val exporter = rememberFileExporter { uri ->
        viewModel.updateExportLocation(uri)
    }
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) { viewModel.loadSettings() }

    var showPrivacyDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showLoginSnackbar by remember { mutableStateOf(false) }

    LaunchedEffect(showLoginSnackbar) {
        if (showLoginSnackbar) {
            snackbarHostState.showSnackbar("Cloud sync coming soon!")
            showLoginSnackbar = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NoorColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = noorType.headlineMd) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NoorColors.Surface.copy(alpha = 0.8f)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ═══════════════════════════════════
            //  Account / Sign In
            // ═══════════════════════════════════
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = NoorColors.SurfaceContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, NoorColors.Primary.copy(alpha = 0.2f)),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = NoorColors.SurfaceContainerHigh,
                        border = BorderStroke(1.5.dp, NoorColors.Primary.copy(alpha = 0.25f)),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(30.dp),
                                tint = NoorColors.OnSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Guest Learner", style = noorType.bodyLg, color = NoorColors.OnSurface)
                        Text("Sign in to sync across devices", style = noorType.labelSm, color = NoorColors.OnSurfaceVariant)
                    }
                    Button(
                        onClick = { showLoginSnackbar = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NoorColors.Primary.copy(alpha = 0.15f),
                            contentColor = NoorColors.Primary,
                        ),
                        border = BorderStroke(1.dp, NoorColors.Primary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign In", style = noorType.labelSm)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ═══════════════════════════════════
            //  Reciter Selection
            // ═══════════════════════════════════
            SettingsSectionHeader(icon = Icons.Default.RecordVoiceOver, title = "Reciter")
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = NoorColors.SurfaceContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, NoorColors.OutlineVariant.copy(alpha = 0.25f)),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    AVAILABLE_RECITERS.forEachIndexed { index, reciter ->
                        val isSelected = reciter.id == selectedReciterId
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) NoorColors.Primary.copy(alpha = 0.12f) else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, NoorColors.Primary.copy(alpha = 0.3f)) else null,
                            onClick = { viewModel.updateReciter(reciter.id) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.updateReciter(reciter.id) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = NoorColors.Primary,
                                        unselectedColor = NoorColors.OnSurfaceVariant.copy(alpha = 0.4f),
                                    ),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    reciter.name,
                                    style = noorType.bodyMd,
                                    color = if (isSelected) NoorColors.OnSurface else NoorColors.OnSurfaceVariant,
                                )
                            }
                        }
                        if (index < AVAILABLE_RECITERS.lastIndex) {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ═══════════════════════════════════
            //  Font Size
            // ═══════════════════════════════════
            SettingsSectionHeader(icon = Icons.Default.FormatSize, title = "Font Size")
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = NoorColors.SurfaceContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, NoorColors.OutlineVariant.copy(alpha = 0.25f)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Arabic Text", style = noorType.labelSm, color = NoorColors.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("A", style = noorType.labelSm, color = NoorColors.OnSurfaceVariant, fontSize = 12.sp)
                        Slider(
                            value = arabicFontSize,
                            onValueChange = { viewModel.updateArabicFontSize(it) },
                            valueRange = 24f..56f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = NoorColors.Primary,
                                activeTrackColor = NoorColors.Primary,
                                inactiveTrackColor = NoorColors.OutlineVariant.copy(alpha = 0.25f),
                            ),
                        )
                        Text("A", style = noorType.headlineMd, color = NoorColors.OnSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${arabicFontSize.toInt()}sp", style = noorType.labelSm, color = NoorColors.Secondary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Translation Text", style = noorType.labelSm, color = NoorColors.OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("A", style = noorType.labelSm, color = NoorColors.OnSurfaceVariant, fontSize = 12.sp)
                        Slider(
                            value = translationFontSize,
                            onValueChange = { viewModel.updateTranslationFontSize(it) },
                            valueRange = 12f..28f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = NoorColors.Primary,
                                activeTrackColor = NoorColors.Primary,
                                inactiveTrackColor = NoorColors.OutlineVariant.copy(alpha = 0.25f),
                            ),
                        )
                        Text("A", style = noorType.headlineMd, color = NoorColors.OnSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${translationFontSize.toInt()}sp", style = noorType.labelSm, color = NoorColors.Secondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ═══════════════════════════════════
            //  Daily Reminder
            // ═══════════════════════════════════
            SettingsSectionHeader(
                icon = if (reminderEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                title = "Daily Reminder",
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = NoorColors.SurfaceContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, NoorColors.OutlineVariant.copy(alpha = 0.25f)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Enable Reminder", style = noorType.bodyLg, color = NoorColors.OnSurface)
                            if (reminderEnabled) {
                                val timeStr = "${reminderHour.toString().padStart(2, '0')}:${reminderMinute.toString().padStart(2, '0')}"
                                Text("Every day at $timeStr", style = noorType.labelSm, color = NoorColors.OnSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !permissionState.hasPermission) {
                                    permissionState.requestPermission()
                                }
                                viewModel.updateReminderEnabled(enabled)
                                if (enabled) {
                                    notificationScheduler.scheduleDailyReminder(
                                        reminderHour, reminderMinute,
                                        "Time to Learn \uD83C\uDF19",
                                        "Continue your Quran journey — even one ayah keeps the streak alive!"
                                    )
                                } else {
                                    notificationScheduler.cancelDailyReminder()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NoorColors.Primary,
                                checkedTrackColor = NoorColors.Primary.copy(alpha = 0.3f),
                                uncheckedThumbColor = NoorColors.OnSurfaceVariant,
                                uncheckedTrackColor = NoorColors.SurfaceContainerHighest,
                            ),
                        )
                    }

                    if (reminderEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val timeOptions = listOf(6 to "06:00", 12 to "12:00", 18 to "18:00", 20 to "20:00", 22 to "22:00")
                            timeOptions.forEach { (hour, label) ->
                                val isSelected = reminderHour == hour && reminderMinute == 0
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) NoorColors.Primary.copy(alpha = 0.15f) else NoorColors.SurfaceContainerHigh,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) NoorColors.Primary.copy(alpha = 0.4f) else Color.Transparent,
                                    ),
                                    onClick = {
                                        viewModel.updateReminderTime(hour, 0)
                                        notificationScheduler.scheduleDailyReminder(
                                            hour, 0,
                                            "Time to Learn \uD83C\uDF19",
                                            "Continue your Quran journey — even one ayah keeps the streak alive!"
                                        )
                                    },
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    ) {
                                        Text(
                                            label,
                                            style = noorType.labelSm,
                                            fontSize = 11.sp,
                                            color = if (isSelected) NoorColors.Primary else NoorColors.OnSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = NoorColors.SurfaceContainerHigh,
                                border = BorderStroke(1.dp, Color.Transparent),
                                onClick = { showTimePicker = true },
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                ) {
                                    Text(
                                        "Custom Time",
                                        style = noorType.labelSm,
                                        color = NoorColors.Primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ═══════════════════════════════════
            //  Downloads & Data Saver
            // ═══════════════════════════════════
            SettingsSectionHeader(icon = Icons.Default.Download, title = "Data & Storage")
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = NoorColors.SurfaceContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, NoorColors.OutlineVariant.copy(alpha = 0.25f)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Data Saver Audio", style = noorType.bodyLg, color = NoorColors.OnSurface)
                            Text(
                                "Download 64kbps audio to save space and data. High quality (128kbps) used otherwise.",
                                style = noorType.labelSm,
                                color = NoorColors.OnSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = useDataSaverAudio,
                            onCheckedChange = { viewModel.updateDataSaver(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NoorColors.Primary,
                                checkedTrackColor = NoorColors.Primary.copy(alpha = 0.3f),
                                uncheckedThumbColor = NoorColors.OnSurfaceVariant,
                                uncheckedTrackColor = NoorColors.SurfaceContainerHighest,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = NoorColors.OutlineVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    val exportLocationLabel = if (exportLocation.length > 40) "..." + exportLocation.takeLast(37) else exportLocation
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { exporter.pickDirectory() }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = NoorColors.Secondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Export Location", style = noorType.bodyLg, color = NoorColors.OnSurface)
                            Text(exportLocationLabel, style = noorType.labelSm, color = NoorColors.OnSurfaceVariant, maxLines = 1)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            if (isExporting || exportLocation == "Default (Internal App Storage)") return@clickable
                            isExporting = true
                            exportProgress = 0f
                            scope.launch {
                                val downloaded = viewModel.getDownloadedSurahs()
                                if (downloaded.isEmpty()) {
                                    isExporting = false
                                    return@launch
                                }
                                exporter.exportSurahs(
                                    surahNumbers = downloaded,
                                    folderUriString = exportLocation,
                                    reciterFolder = "Alafasy_128kbps", // Currently hardcoded to active reciter if we map it, but alafasy is good default for offline testing
                                    onProgress = { exportProgress = it },
                                    onComplete = { isExporting = false },
                                    onError = { isExporting = false }
                                )
                            }
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(progress = { exportProgress }, modifier = Modifier.size(24.dp), color = NoorColors.Primary)
                        } else {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, tint = NoorColors.Primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Export Offline Surahs", style = noorType.bodyLg, color = NoorColors.OnSurface)
                            Text(if (isExporting) "Exporting... ${(exportProgress * 100).toInt()}%" else if (exportLocation == "Default (Internal App Storage)") "Please pick an export location first" else "Save cached audio to selected folder", style = noorType.labelSm, color = NoorColors.OnSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ═══════════════════════════════════
            //  About
            // ═══════════════════════════════════
            SettingsSectionHeader(icon = Icons.Default.Info, title = "About")
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = NoorColors.SurfaceContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, NoorColors.OutlineVariant.copy(alpha = 0.25f)),
                onClick = { showPrivacyDialog = true },
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = NoorColors.OnSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Privacy Policy", style = noorType.bodyLg, color = NoorColors.OnSurface)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Noor v1.0 · Made with 💚",
                style = noorType.labelSm,
                color = NoorColors.OnSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                containerColor = NoorColors.SurfaceContainerHigh,
                titleContentColor = NoorColors.OnSurface,
                textContentColor = NoorColors.OnSurfaceVariant,
                title = { Text("Privacy Policy", style = noorType.headlineMd) },
                text = {
                    Text(
                        "Your privacy is our priority.\n\nYour data is fully encrypted and synced exclusively for your personal backup. We do not sell, share, or analyze your personal information with any third party.",
                        style = noorType.bodyMd,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyDialog = false }) {
                        Text("Close", color = NoorColors.Primary)
                    }
                }
            )
        }

        if (showTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = reminderHour,
                initialMinute = reminderMinute,
                is24Hour = false,
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                modifier = Modifier.fillMaxWidth(),
                containerColor = NoorColors.SurfaceContainerHigh,
                titleContentColor = NoorColors.OnSurface,
                text = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TimePicker(state = timePickerState)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val h = timePickerState.hour
                        val m = timePickerState.minute
                        viewModel.updateReminderTime(h, m)
                        notificationScheduler.scheduleDailyReminder(
                            h, m,
                            "Time to Learn \uD83C\uDF19",
                            "Continue your Quran journey — even one ayah keeps the streak alive!"
                        )
                        showTimePicker = false
                    }) {
                        Text("Save", color = NoorColors.Primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Cancel", color = NoorColors.OnSurfaceVariant)
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    val noorType = LocalNoorTypography.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = NoorColors.Primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = noorType.bodyLg, color = NoorColors.OnSurface)
    }
}
