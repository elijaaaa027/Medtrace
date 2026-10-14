package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ActivityLogEntity
import com.example.data.AppointmentEntity
import com.example.data.GuardianEntity
import com.example.data.HealthRecordEntity
import com.example.data.HealthSnapshotEntity
import com.example.data.InsurancePolicyEntity
import com.example.data.MedicineEntity
import com.example.data.ReceiptEntity
import com.example.data.UserProfileEntity
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.i18n.Translations
import com.example.security.AuthAndFileManager

enum class MedTraceRoute {
  DASHBOARD,
  HEALTH_SNAPSHOT,
  HEALTH_RECORDS,
  APPOINTMENTS,
  MEDICINES,
  INSURANCE,
  GUARDIAN,
  GUARDIAN_OVERVIEW,
  NEARBY_HOSPITALS,
  AI_CHAT,
  RECEIPTS,
  PROFILE,
  SETTINGS,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
  strings: AppStrings,
  currentLanguage: AppLanguage,
  profile: UserProfileEntity,
  snapshot: HealthSnapshotEntity?,
  appointments: List<AppointmentEntity>,
  medicines: List<MedicineEntity>,
  records: List<HealthRecordEntity>,
  insurancePolicies: List<InsurancePolicyEntity>,
  guardians: List<GuardianEntity>,
  receipts: List<ReceiptEntity>,
  activityLogs: List<ActivityLogEntity>,
  onNavigate: (MedTraceRoute) -> Unit,
) {
  val greeting =
    Translations.getTimeBasedGreeting(currentLanguage, profile.fullName.ifBlank { "Patient" })

  val upcomingAppointments = appointments.filter { it.status == "Upcoming" }
  val activeMedicines = medicines.filter { it.status == "Active" }
  val activeInsurance = insurancePolicies.firstOrNull { it.isActive } ?: insurancePolicies.firstOrNull()

  val isSnapshotAvailable =
    snapshot != null &&
      (snapshot.bloodGroup.isNotBlank() ||
        snapshot.allergies.isNotBlank() ||
        snapshot.recentDiagnosis.isNotBlank() ||
        snapshot.bloodPressure.isNotBlank() ||
        snapshot.heartRate.isNotBlank())

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Greeting & Patient Verification Header Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors =
        CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = greeting,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
          text =
            "Phone: +91 ${profile.phoneNumber} (${strings.phoneVerifiedBadge}) · " +
              "Local Time: ${Translations.currentLocalDateString()} ${Translations.currentLocalTimeString()}",
          fontFamily = FontFamily.Monospace,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        if (profile.idDocMaskedNumber.isNotBlank()) {
          Text(
            text =
              "${profile.idDocType}: ${profile.idDocMaskedNumber} · ${profile.idDocVerificationStatus}",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }

    // 1. Health Snapshot Card
    DashboardSectionCard(
      title = strings.healthSnapshotCardTitle,
      icon = Icons.Default.MonitorHeart,
      actionLabel = strings.updateHealthSnapshotButton,
      onActionClick = { onNavigate(MedTraceRoute.HEALTH_SNAPSHOT) },
    ) {
      if (!isSnapshotAvailable || snapshot == null) {
        Text(
          text = strings.healthSnapshotUnavailable,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          MetricItem("Blood Group", snapshot.bloodGroup.ifBlank { "N/A" })
          MetricItem("Blood Pressure", snapshot.bloodPressure.ifBlank { "N/A" })
          MetricItem("Heart Rate", snapshot.heartRate.ifBlank { "N/A" })
          MetricItem("SpO2", snapshot.spo2.ifBlank { "N/A" })
        }
        if (snapshot.allergies.isNotBlank() || snapshot.recentDiagnosis.isNotBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text =
              "Allergies: ${snapshot.allergies.ifBlank { "None" }} · Diagnosis: ${snapshot.recentDiagnosis.ifBlank { "N/A" }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // 2. Upcoming Appointment Card
    DashboardSectionCard(
      title = strings.upcomingAppointmentCardTitle,
      icon = Icons.Default.CalendarMonth,
      actionLabel = strings.navAppointments,
      onActionClick = { onNavigate(MedTraceRoute.APPOINTMENTS) },
    ) {
      val nextAppt = upcomingAppointments.firstOrNull()
      if (nextAppt == null) {
        Text(
          text = strings.noUpcomingAppointments,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Text(
          text = "${nextAppt.doctorName} · ${nextAppt.department}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "${nextAppt.hospitalOrClinic} · ${nextAppt.appointmentDate} at ${nextAppt.appointmentTime}",
          fontFamily = FontFamily.Monospace,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "${strings.reasonForVisitLabel}: ${nextAppt.reasonForVisit}",
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }

    // 3. Medicines Card
    DashboardSectionCard(
      title = "${strings.activeMedicinesCardTitle} (${activeMedicines.size})",
      icon = Icons.Default.Medication,
      actionLabel = strings.navMedicines,
      onActionClick = { onNavigate(MedTraceRoute.MEDICINES) },
    ) {
      if (activeMedicines.isEmpty()) {
        Text(
          text = strings.noMedicinesFound,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        activeMedicines.take(3).forEach { med ->
          Text(
            text = "${med.medicineName} (${med.dosage}) · ${med.frequency} · ${med.reminderSchedule}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
          )
        }
      }
    }

    // 4. Health Records & Receipts Card
    DashboardSectionCard(
      title = "${strings.healthRecordsCardTitle} (${records.size})",
      icon = Icons.Default.Description,
      actionLabel = strings.navHealthRecords,
      onActionClick = { onNavigate(MedTraceRoute.HEALTH_RECORDS) },
    ) {
      if (records.isEmpty()) {
        Text(
          text = strings.noRecordsUploaded,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        records.take(3).forEach { rec ->
          Text(
            text = "${rec.fileName} · ${rec.recordType} · ${Translations.formatLocalTimestamp(rec.uploadDate)}",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${strings.navReceipts}: ${receipts.size} recorded",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { onNavigate(MedTraceRoute.RECEIPTS) }) {
          Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(strings.navReceipts)
        }
      }
    }

    // 5. Insurance Card
    DashboardSectionCard(
      title = strings.insuranceCardTitle,
      icon = Icons.Default.HealthAndSafety,
      actionLabel = strings.navInsurance,
      onActionClick = { onNavigate(MedTraceRoute.INSURANCE) },
    ) {
      if (activeInsurance == null) {
        Text(
          text = strings.insuranceNotAdded,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = activeInsurance.providerName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text =
                "Policy: ${activeInsurance.policyNumber} · Coverage: ${activeInsurance.coverageAmount}",
              fontFamily = FontFamily.Monospace,
              style = MaterialTheme.typography.bodySmall,
            )
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              Icons.Default.CheckCircle,
              contentDescription = null,
              tint = if (activeInsurance.isActive) Color(0xFF16A34A) else Color(0xFF64748B),
              modifier = Modifier.size(16.dp),
            )
            Text(
              text =
                if (activeInsurance.isActive) strings.activeCoverBadge
                else strings.inactiveCoverBadge,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = if (activeInsurance.isActive) Color(0xFF16A34A) else Color(0xFF64748B),
            )
          }
        }
      }
    }

    // 6. Guardian Overview Card
    DashboardSectionCard(
      title = strings.guardianOverviewCardTitle,
      icon = Icons.Default.People,
      actionLabel = strings.navGuardianOverview,
      onActionClick = { onNavigate(MedTraceRoute.GUARDIAN_OVERVIEW) },
    ) {
      if (guardians.isEmpty()) {
        Text(
          text = strings.noGuardiansAdded,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { onNavigate(MedTraceRoute.GUARDIAN) }) {
          Text(strings.addGuardianButton)
        }
      } else {
        val primary = guardians.first()
        Text(
          text =
            "${primary.fullName} (${primary.relationship}) · +91 ${primary.phoneNumber} · " +
              if (primary.isPhoneVerified) strings.phoneVerifiedBadge
              else strings.phoneUnverifiedBadge,
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Medium,
        )
      }
    }

    // 7. Nearby Hospitals Card
    DashboardSectionCard(
      title = strings.nearbyHospitalsCardTitle,
      icon = Icons.Default.LocationOn,
      actionLabel = strings.enableLocationButton,
      onActionClick = { onNavigate(MedTraceRoute.NEARBY_HOSPITALS) },
    ) {
      Text(
        text = "Find nearby hospitals, clinics, and pharmacies with real-time GPS distance and Google Maps directions.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    // 8. Recent Activity Card
    DashboardSectionCard(
      title = strings.recentActivityCardTitle,
      icon = Icons.Default.History,
      actionLabel = strings.navSettings,
      onActionClick = { onNavigate(MedTraceRoute.SETTINGS) },
    ) {
      if (activityLogs.isEmpty()) {
        Text(
          text = strings.noRecentActivity,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        activityLogs.take(5).forEach { log ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = log.title,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.weight(1f),
            )
            Text(
              text = Translations.formatLocalTimestamp(log.timestamp),
              fontFamily = FontFamily.Monospace,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DashboardSectionCard(
  title: String,
  icon: ImageVector,
  actionLabel: String,
  onActionClick: () -> Unit,
  content: @Composable () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        TextButton(onClick = onActionClick) {
          Text(text = actionLabel, style = MaterialTheme.typography.labelMedium)
          Spacer(modifier = Modifier.width(4.dp))
          Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
        }
      }
      HorizontalDivider()
      content()
    }
  }
}

@Composable
private fun MetricItem(label: String, value: String) {
  Column {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      fontFamily = FontFamily.Monospace,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
fun SettingsScreen(
  strings: AppStrings,
  currentLanguage: AppLanguage,
  isDarkMode: Boolean,
  isBiometricEnabled: Boolean,
  profile: UserProfileEntity,
  onLanguageChange: (AppLanguage) -> Unit,
  onDarkModeToggle: (Boolean) -> Unit,
  onBiometricToggle: (Boolean) -> Unit,
  onLockAppNow: () -> Unit,
  onNavigateToProfile: () -> Unit,
  onLoadDemoSampleData: () -> Unit,
  onLogout: () -> Unit,
) {
  val context = LocalContext.current
  var showBiometricTestDialog by remember { mutableStateOf(false) }
  var biometricFeedback by remember { mutableStateOf<String?>(null) }
  var pinInput by remember { mutableStateOf(AuthAndFileManager.getSecurityPin(context)) }
  var pinSavedNotice by remember { mutableStateOf<String?>(null) }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = strings.navSettings,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
    )

    // 1. Dark Mode Theme Toggle Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clickable { onDarkModeToggle(!isDarkMode) }
            .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
          )
          Column {
            Text(
              text = "Dark Mode Theme",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text =
                if (isDarkMode) "Dark clinical color scheme active"
                else "Light clinical color scheme active",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Switch(
          checked = isDarkMode,
          onCheckedChange = { onDarkModeToggle(it) },
        )
      }
    }

    // 2. Biometric Authentication (Fingerprint / Face Unlock) & Security PIN Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Default.Fingerprint,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(26.dp),
            )
            Column {
              Text(
                text = strings.biometricTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = strings.biometricSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Switch(
            checked = isBiometricEnabled,
            onCheckedChange = { onBiometricToggle(it) },
          )
        }

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = AuthAndFileManager.getBiometricHardwareStatusLabel(context),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text =
                "Bound Account: +91 ${profile.phoneNumber} · Status: " +
                  if (isBiometricEnabled) "Enabled (Fingerprint & Face Unlock active)"
                  else "Disabled (Phone + OTP required)",
              fontFamily = FontFamily.Monospace,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = { showBiometricTestDialog = true },
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Verify Biometrics")
          }
          Button(
            onClick = onLockAppNow,
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.biometricLockAppButton)
          }
        }

        if (biometricFeedback != null) {
          Text(
            text = biometricFeedback!!,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
          )
        }

        HorizontalDivider()

        Text(
          text = "Backup Security PIN (Alternative to Biometric / Phone OTP)",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = pinInput,
            onValueChange = {
              pinInput = it.filter { ch -> ch.isDigit() }.take(6)
              pinSavedNotice = null
            },
            label = { Text("4–6 Digit Security PIN") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedButton(
            onClick = {
              AuthAndFileManager.saveSecurityPin(context, pinInput)
              pinSavedNotice =
                if (pinInput.length >= 4) "Security PIN saved for quick unlock."
                else "Security PIN cleared."
            },
            enabled = pinInput.isEmpty() || pinInput.length >= 4,
          ) {
            Text(strings.saveButton)
          }
        }
        if (pinSavedNotice != null) {
          Text(
            text = pinSavedNotice!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }

    // 2. Language Preferences Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = strings.preferredLanguageLabel,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Switch the entire application interface between English, தமிழ் (Tamil), and हिन्दी (Hindi).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LanguageSelectorBar(
          currentLanguage = currentLanguage,
          onLanguageChange = onLanguageChange,
        )
      }
    }

    // 3. Profile & Identity Document Verification Summary Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            Icons.Default.VerifiedUser,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Account & Identity Verification",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        Text(
          text = "Name: ${profile.fullName.ifBlank { "Not set" }} · Verified Phone: +91 ${profile.phoneNumber}",
          style = MaterialTheme.typography.bodySmall,
        )
        Text(
          text =
            if (profile.idDocMaskedNumber.isNotBlank()) {
              "${profile.idDocType}: ${profile.idDocMaskedNumber} (${profile.idDocVerificationStatus})"
            } else {
              "Identity Document: ${strings.statusNotSubmitted}"
            },
          fontFamily = FontFamily.Monospace,
          style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(onClick = onNavigateToProfile) {
          Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.editProfileTitle)
        }
      }
    }

    // 4. Optional Demo Sample Data Loader (clearly marked DEMO/TEST DATA)
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            Icons.Default.Science,
            contentDescription = null,
            tint = Color(0xFFD97706),
          )
          Text(
            text = "Demo Sample Clinical Records [DEMO/TEST DATA]",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        Text(
          text = "New accounts start empty by default so no medical records or insurance policies are invented. Tap below if you want to populate sample demo appointments, medicines, insurance, receipts, and health snapshot data for testing.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onLoadDemoSampleData) {
          Text("Populate Sample Demo Data [DEMO/TEST DATA]")
        }
      }
    }

    // 5. Sign Out Button
    Button(
      onClick = onLogout,
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
      modifier = Modifier.fillMaxWidth().height(48.dp),
      shape = RoundedCornerShape(10.dp),
    ) {
      Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(strings.logoutButton, fontWeight = FontWeight.SemiBold)
    }
  }

  if (showBiometricTestDialog) {
    BiometricVerificationDialog(
      strings = strings,
      boundPhone = profile.phoneNumber,
      onDismiss = { showBiometricTestDialog = false },
      onSuccess = { method ->
        showBiometricTestDialog = false
        onBiometricToggle(true)
        biometricFeedback = "Biometric verification succeeded ($method)."
      },
    )
  }
}
