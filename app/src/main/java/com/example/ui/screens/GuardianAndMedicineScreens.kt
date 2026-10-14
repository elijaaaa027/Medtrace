package com.example.ui.screens

import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.UserProfileEntity
import com.example.i18n.AppStrings
import com.example.i18n.Translations
import com.example.security.AuthAndFileManager
import com.example.security.OtpSessionChallenge
import com.example.security.OtpVerificationResult

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuardianManagementScreen(
  strings: AppStrings,
  userId: String,
  guardians: List<GuardianEntity>,
  onSaveGuardian: (GuardianEntity) -> Unit,
  onDeleteGuardian: (GuardianEntity) -> Unit,
  onNavigateToOverview: () -> Unit,
) {
  var editingGuardian by remember { mutableStateOf<GuardianEntity?>(null) }
  var showEditorDialog by remember { mutableStateOf(false) }
  var guardianToDelete by remember { mutableStateOf<GuardianEntity?>(null) }

  // OTP Verification state for Guardian phone number
  var verifyingGuardian by remember { mutableStateOf<GuardianEntity?>(null) }
  var guardianOtpChallenge by remember { mutableStateOf<OtpSessionChallenge?>(null) }
  var guardianOtpInput by remember { mutableStateOf("") }
  var guardianOtpError by remember { mutableStateOf<String?>(null) }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = strings.guardianManagementTitle,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Manage caregivers, phone verification, and role-based permissions.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Button(
        onClick = {
          editingGuardian = null
          showEditorDialog = true
        }
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(strings.addGuardianButton)
      }
    }

    OutlinedButton(
      onClick = onNavigateToOverview,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(strings.navGuardianOverview)
    }

    if (guardians.isEmpty()) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            Icons.Default.People,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
          )
          Text(
            text = strings.noGuardiansAdded,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      guardians.forEach { guardian ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = guardian.fullName,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text =
                    buildString {
                      append(guardian.relationship)
                      if (guardian.isPrimaryEmergencyContact) {
                        append(" · Primary Emergency Contact")
                      }
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              Row {
                IconButton(
                  onClick = {
                    editingGuardian = guardian
                    showEditorDialog = true
                  }
                ) {
                  Icon(Icons.Default.Edit, contentDescription = strings.editGuardianButton)
                }
                IconButton(onClick = { guardianToDelete = guardian }) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = strings.removeGuardianButton,
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "${strings.guardianPhoneLabel}: +91 ${guardian.phoneNumber}",
                  fontFamily = FontFamily.Monospace,
                  style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                  text =
                    if (guardian.isPhoneVerified) strings.phoneVerifiedBadge
                    else strings.phoneUnverifiedBadge,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (guardian.isPhoneVerified) Color(0xFF16A34A) else Color(0xFFD97706),
                  fontWeight = FontWeight.SemiBold,
                )
              }

              if (!guardian.isPhoneVerified) {
                OutlinedButton(
                  onClick = {
                    val ch = AuthAndFileManager.issueOtpForPhone(guardian.phoneNumber)
                    if (ch != null) {
                      verifyingGuardian = guardian
                      guardianOtpChallenge = ch
                      guardianOtpInput = ""
                      guardianOtpError = null
                    }
                  }
                ) {
                  Text(strings.verifyGuardianPhoneButton)
                }
              }
            }

            HorizontalDivider()
            Text(
              text = strings.guardianPermissionsTitle,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val permsSummary =
              buildList {
                if (guardian.canViewAppointments) add("Appointments")
                if (guardian.canViewMedications) add("Medications")
                if (guardian.canViewHealthAlerts) add("Health Alerts")
                if (guardian.canViewInsurance) add("Insurance")
                if (guardian.canViewEmergencyInfo) add("Emergency Info")
                if (guardian.canViewAuthorizedDocuments) add("Authorized Documents")
              }
            Text(
              text =
                if (permsSummary.isEmpty()) "No sections authorized"
                else permsSummary.joinToString(" · "),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }
  }

  if (showEditorDialog) {
    val existing = editingGuardian
    var name by remember(existing) { mutableStateOf(existing?.fullName ?: "") }
    var relationship by
      remember(existing) { mutableStateOf(existing?.relationship ?: "Parent") }
    var phone by remember(existing) { mutableStateOf(existing?.phoneNumber ?: "") }
    var email by remember(existing) { mutableStateOf(existing?.email ?: "") }
    var isPrimary by
      remember(existing) { mutableStateOf(existing?.isPrimaryEmergencyContact ?: false) }
    var canAppointments by
      remember(existing) { mutableStateOf(existing?.canViewAppointments ?: true) }
    var canMedications by
      remember(existing) { mutableStateOf(existing?.canViewMedications ?: true) }
    var canAlerts by
      remember(existing) { mutableStateOf(existing?.canViewHealthAlerts ?: true) }
    var canInsurance by
      remember(existing) { mutableStateOf(existing?.canViewInsurance ?: true) }
    var canEmergency by
      remember(existing) { mutableStateOf(existing?.canViewEmergencyInfo ?: true) }
    var canDocs by
      remember(existing) { mutableStateOf(existing?.canViewAuthorizedDocuments ?: false) }
    var dialogError by remember { mutableStateOf<String?>(null) }

    val relationshipOptions =
      listOf(
        "Parent" to strings.relParent,
        "Mother" to strings.relMother,
        "Father" to strings.relFather,
        "Legal guardian" to strings.relLegalGuardian,
        "Other authorized caregiver" to strings.relCaregiver,
      )

    AlertDialog(
      onDismissRequest = { showEditorDialog = false },
      title = {
        Text(if (existing == null) strings.addGuardianButton else strings.editGuardianButton)
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedTextField(
            value = name,
            onValueChange = {
              name = it
              dialogError = null
            },
            label = { Text("${strings.guardianNameLabel} *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          Text(
            text = strings.guardianRelationshipLabel,
            style = MaterialTheme.typography.labelMedium,
          )
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            relationshipOptions.forEach { (key, label) ->
              FilterChip(
                selected = relationship == key,
                onClick = { relationship = key },
                label = { Text(label) },
              )
            }
          }

          OutlinedTextField(
            value = phone,
            onValueChange = {
              phone = it.filter { ch -> ch.isDigit() }.take(10)
              dialogError = null
            },
            label = { Text("${strings.guardianPhoneLabel} *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          if (existing != null && AuthAndFileManager.normalizePhoneNumber(phone) != existing.phoneNumber) {
            Text(
              text = "Note: Changing the guardian's phone number will reset verification status until the new number is verified via OTP.",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFD97706),
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { isPrimary = !isPrimary },
          ) {
            Checkbox(checked = isPrimary, onCheckedChange = { isPrimary = it })
            Text(strings.setPrimaryEmergencyLabel, style = MaterialTheme.typography.bodySmall)
          }

          HorizontalDivider()
          Text(
            text = strings.guardianPermissionsTitle,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )

          PermissionToggleRow(strings.permAppointments, canAppointments) { canAppointments = it }
          PermissionToggleRow(strings.permMedications, canMedications) { canMedications = it }
          PermissionToggleRow(strings.permHealthAlerts, canAlerts) { canAlerts = it }
          PermissionToggleRow(strings.permInsurance, canInsurance) { canInsurance = it }
          PermissionToggleRow(strings.permEmergencyInfo, canEmergency) { canEmergency = it }
          PermissionToggleRow(strings.permDocuments, canDocs) { canDocs = it }

          if (dialogError != null) {
            Text(
              text = dialogError!!,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (name.isBlank() || !AuthAndFileManager.isValidPhoneNumber(phone)) {
              dialogError = "Please enter guardian name and a valid 10-digit phone number."
              return@Button
            }
            val normalizedPhone = AuthAndFileManager.normalizePhoneNumber(phone)
            // If phone changed, require verification for the new phone number
            val phoneStillVerified =
              existing != null && existing.isPhoneVerified && existing.phoneNumber == normalizedPhone

            val entity =
              GuardianEntity(
                id = existing?.id ?: 0L,
                userId = userId,
                fullName = name.trim(),
                relationship = relationship,
                phoneNumber = normalizedPhone,
                isPhoneVerified = phoneStillVerified,
                email = email.trim(),
                isPrimaryEmergencyContact = isPrimary,
                canViewAppointments = canAppointments,
                canViewMedications = canMedications,
                canViewHealthAlerts = canAlerts,
                canViewInsurance = canInsurance,
                canViewEmergencyInfo = canEmergency,
                canViewAuthorizedDocuments = canDocs,
                addedAt = existing?.addedAt ?: System.currentTimeMillis(),
              )
            onSaveGuardian(entity)
            showEditorDialog = false
          }
        ) {
          Text(strings.saveButton)
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditorDialog = false }) { Text(strings.cancelButton) }
      },
    )
  }

  val delTarget = guardianToDelete
  if (delTarget != null) {
    AlertDialog(
      onDismissRequest = { guardianToDelete = null },
      title = { Text(strings.removeGuardianButton) },
      text = { Text("Are you sure you want to remove ${delTarget.fullName} as a guardian?") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteGuardian(delTarget)
            guardianToDelete = null
          }
        ) {
          Text(strings.deleteButton)
        }
      },
      dismissButton = {
        TextButton(onClick = { guardianToDelete = null }) { Text(strings.cancelButton) }
      },
    )
  }

  val otpChall = guardianOtpChallenge
  val verGuard = verifyingGuardian
  if (otpChall != null && verGuard != null) {
    AlertDialog(
      onDismissRequest = {
        guardianOtpChallenge = null
        verifyingGuardian = null
      },
      title = { Text("${strings.verifyGuardianPhoneButton}: ${verGuard.fullName}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "${strings.otpAssociatedWith}: +91 ${otpChall.phoneNumber}",
            style = MaterialTheme.typography.bodySmall,
          )
          Surface(
            color = Color(0xFFFFFBEB),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = strings.demoOtpBannerTitle,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF92400E),
              )
              Text(
                text = "Test OTP for ${otpChall.phoneNumber}: ${otpChall.otpCode}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF92400E),
              )
              TextButton(onClick = { guardianOtpInput = otpChall.otpCode }) {
                Text("Use Test OTP")
              }
            }
          }
          OutlinedTextField(
            value = guardianOtpInput,
            onValueChange = {
              guardianOtpInput = it.filter { ch -> ch.isDigit() }.take(6)
              guardianOtpError = null
            },
            label = { Text(strings.enterSixDigitOtp) },
            singleLine = true,
          )
          if (guardianOtpError != null) {
            Text(
              text = guardianOtpError!!,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            when (AuthAndFileManager.verifyOtpForPhone(otpChall.phoneNumber, guardianOtpInput)) {
              is OtpVerificationResult.Success -> {
                onSaveGuardian(verGuard.copy(isPhoneVerified = true))
                guardianOtpChallenge = null
                verifyingGuardian = null
              }
              OtpVerificationResult.ExpiredOtp -> guardianOtpError = strings.expiredOtpError
              else -> guardianOtpError = strings.invalidOtpError
            }
          }
        ) {
          Text(strings.verifyOtpButton)
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            guardianOtpChallenge = null
            verifyingGuardian = null
          }
        ) {
          Text(strings.cancelButton)
        }
      },
    )
  }
}

@Composable
private fun PermissionToggleRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      modifier = Modifier.weight(1f),
    )
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuardianOverviewScreen(
  strings: AppStrings,
  profile: UserProfileEntity,
  guardians: List<GuardianEntity>,
  appointments: List<AppointmentEntity>,
  medicines: List<MedicineEntity>,
  snapshot: HealthSnapshotEntity?,
  insurancePolicies: List<InsurancePolicyEntity>,
  records: List<HealthRecordEntity>,
  activityLogs: List<ActivityLogEntity>,
  onNavigateToGuardianManagement: () -> Unit,
) {
  var selectedGuardianId by
    remember(guardians) { mutableStateOf(guardians.firstOrNull()?.id) }
  val activeGuardian =
    guardians.firstOrNull { it.id == selectedGuardianId } ?: guardians.firstOrNull()

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = strings.guardianOverviewTitle,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = strings.guardianRoleSubtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      OutlinedButton(onClick = onNavigateToGuardianManagement) {
        Text(strings.guardianManagementTitle)
      }
    }

    if (activeGuardian == null) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            Icons.Default.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
          )
          Text(text = strings.noGuardiansAdded, style = MaterialTheme.typography.bodyMedium)
          Button(onClick = onNavigateToGuardianManagement) {
            Text(strings.addGuardianButton)
          }
        }
      }
      return@Column
    }

    if (guardians.size > 1) {
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        guardians.forEach { g ->
          FilterChip(
            selected = g.id == activeGuardian.id,
            onClick = { selectedGuardianId = g.id },
            label = { Text("${g.fullName} (${g.relationship})") },
          )
        }
      }
    }

    // Patient summary header
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors =
        CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "Patient: ${profile.fullName.ifBlank { "Patient" }}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text =
            "Viewing as Guardian: ${activeGuardian.fullName} (${activeGuardian.relationship}) · " +
              if (activeGuardian.isPhoneVerified) strings.phoneVerifiedBadge
              else strings.phoneUnverifiedBadge,
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }

    // 1. Upcoming Appointments (Role-checked)
    GuardianPermissionSectionCard(
      title = strings.upcomingAppointmentCardTitle,
      isAuthorized = activeGuardian.canViewAppointments,
      restrictedText = strings.accessRestrictedMessage,
    ) {
      val upcoming = appointments.filter { it.status == "Upcoming" }
      if (upcoming.isEmpty()) {
        Text(strings.noUpcomingAppointments, style = MaterialTheme.typography.bodySmall)
      } else {
        upcoming.forEach { appt ->
          Text(
            text = "${appt.appointmentDate} ${appt.appointmentTime} · ${appt.doctorName} (${appt.hospitalOrClinic})",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
          )
        }
      }
    }

    // 2. Medication Reminders (Role-checked + medicine-level visibility check)
    GuardianPermissionSectionCard(
      title = strings.activeMedicinesCardTitle,
      isAuthorized = activeGuardian.canViewMedications,
      restrictedText = strings.accessRestrictedMessage,
    ) {
      val sharedMeds = medicines.filter { it.visibleToGuardian }
      if (sharedMeds.isEmpty()) {
        Text(
          text = "No guardian-authorized medication reminders active.",
          style = MaterialTheme.typography.bodySmall,
        )
      } else {
        sharedMeds.forEach { med ->
          Text(
            text = "${med.medicineName} (${med.dosage}) · ${med.frequency} · Schedule: ${med.reminderSchedule} [${med.status}]",
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    }

    // 3. Important Healthcare Alerts & Recent Activity (Role-checked)
    GuardianPermissionSectionCard(
      title = "Healthcare Alerts & Recent Activity",
      isAuthorized = activeGuardian.canViewHealthAlerts,
      restrictedText = strings.accessRestrictedMessage,
    ) {
      if (snapshot != null && (snapshot.allergies.isNotBlank() || snapshot.medicalConditions.isNotBlank())) {
        Text(
          text = "Allergies: ${snapshot.allergies.ifBlank { "None recorded" }}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = "Conditions: ${snapshot.medicalConditions.ifBlank { "None recorded" }}",
          style = MaterialTheme.typography.bodySmall,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
      }
      if (activityLogs.isEmpty()) {
        Text(strings.noRecentActivity, style = MaterialTheme.typography.bodySmall)
      } else {
        activityLogs.take(5).forEach { log ->
          Text(
            text = "${log.title} · ${Translations.formatLocalTimestamp(log.timestamp)}",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    }

    // 4. Insurance Status (Role-checked)
    GuardianPermissionSectionCard(
      title = strings.insuranceCardTitle,
      isAuthorized = activeGuardian.canViewInsurance,
      restrictedText = strings.accessRestrictedMessage,
    ) {
      if (insurancePolicies.isEmpty()) {
        Text(strings.insuranceNotAdded, style = MaterialTheme.typography.bodySmall)
      } else {
        insurancePolicies.forEach { pol ->
          Text(
            text =
              "${pol.providerName} · Policy ${pol.policyNumber} · " +
                if (pol.isActive) strings.activeCoverBadge else strings.inactiveCoverBadge,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
          )
        }
      }
    }

    // 5. Emergency Information (Role-checked)
    GuardianPermissionSectionCard(
      title = "Emergency Information",
      isAuthorized = activeGuardian.canViewEmergencyInfo,
      restrictedText = strings.accessRestrictedMessage,
    ) {
      Text(
        text = "Emergency Contact: ${profile.emergencyContactName.ifBlank { "Not set" }} (${profile.emergencyContactPhone.ifBlank { "N/A" }})",
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        text = "Blood Group: ${snapshot?.bloodGroup?.ifBlank { "Not recorded" } ?: "Not recorded"}",
        style = MaterialTheme.typography.bodySmall,
      )
    }

    // 6. Recent Documents where permission has been granted
    GuardianPermissionSectionCard(
      title = "Authorized Health Documents",
      isAuthorized = activeGuardian.canViewAuthorizedDocuments,
      restrictedText = strings.accessRestrictedMessage,
    ) {
      val sharedDocs = records.filter { it.sharedWithGuardian }
      if (sharedDocs.isEmpty()) {
        Text(
          text = "No documents have been marked as shared with guardians.",
          style = MaterialTheme.typography.bodySmall,
        )
      } else {
        sharedDocs.forEach { doc ->
          Text(
            text = "${doc.fileName} (${doc.recordType}) · ${Translations.formatLocalTimestamp(doc.uploadDate)}",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    }
  }
}

@Composable
private fun GuardianPermissionSectionCard(
  title: String,
  isAuthorized: Boolean,
  restrictedText: String,
  content: @Composable () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Icon(
          imageVector = if (isAuthorized) Icons.Default.CheckCircle else Icons.Default.Lock,
          contentDescription = null,
          tint = if (isAuthorized) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
          modifier = Modifier.size(18.dp),
        )
      }
      if (isAuthorized) {
        content()
      } else {
        Text(
          text = restrictedText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicationManagementScreen(
  strings: AppStrings,
  userId: String,
  medicines: List<MedicineEntity>,
  onSaveMedicine: (MedicineEntity) -> Unit,
  onDeleteMedicine: (MedicineEntity) -> Unit,
) {
  var statusFilter by remember { mutableStateOf("All") }
  var editingMedicine by remember { mutableStateOf<MedicineEntity?>(null) }
  var showDialog by remember { mutableStateOf(false) }

  val filteredMedicines =
    if (statusFilter == "All") medicines else medicines.filter { it.status == statusFilter }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = strings.medicinesTitle,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f),
      )
      Button(
        onClick = {
          editingMedicine = null
          showDialog = true
        }
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(strings.addMedicineButton)
      }
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf(
        "All" to "All",
        "Active" to strings.statusActive,
        "Upcoming" to strings.statusUpcoming,
        "Completed" to strings.statusCompleted,
        "Missed" to strings.statusMissed,
      ).forEach { (key, label) ->
        FilterChip(
          selected = statusFilter == key,
          onClick = { statusFilter = key },
          label = { Text(label) },
        )
      }
    }

    if (filteredMedicines.isEmpty()) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            Icons.Default.Medication,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
          )
          Text(
            text = strings.noMedicinesFound,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      filteredMedicines.forEach { med ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${med.medicineName} · ${med.dosage}",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "${med.frequency} · ${strings.statusLabel}: ${med.status}",
                  style = MaterialTheme.typography.bodySmall,
                  color =
                    when (med.status) {
                      "Active" -> Color(0xFF16A34A)
                      "Missed" -> Color(0xFFDC2626)
                      else -> MaterialTheme.colorScheme.primary
                    },
                  fontWeight = FontWeight.SemiBold,
                )
              }
              Row {
                IconButton(
                  onClick = {
                    editingMedicine = med
                    showDialog = true
                  }
                ) {
                  Icon(Icons.Default.Edit, contentDescription = strings.editButton)
                }
                IconButton(onClick = { onDeleteMedicine(med) }) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = strings.deleteButton,
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }

            Text(
              text =
                "${strings.startDateLabel}: ${med.startDate} · ${strings.endDateLabel}: ${med.endDate}",
              fontFamily = FontFamily.Monospace,
              style = MaterialTheme.typography.bodySmall,
            )
            Text(
              text = "${strings.prescribedByLabel}: ${med.prescribedBy.ifBlank { "Not specified" }}",
              style = MaterialTheme.typography.bodySmall,
            )
            Text(
              text = "${strings.reminderScheduleLabel}: ${med.reminderSchedule}",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
            )
            if (med.instructions.isNotBlank()) {
              Text(
                text = "${strings.instructionsLabel}: ${med.instructions}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            HorizontalDivider()
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text =
                  if (med.visibleToGuardian) "Guardian Visibility: Authorized"
                  else "Guardian Visibility: Private",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Active", "Completed", "Upcoming", "Missed").forEach { st ->
                  if (st != med.status) {
                    TextButton(onClick = { onSaveMedicine(med.copy(status = st)) }) {
                      Text(st, style = MaterialTheme.typography.labelSmall)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showDialog) {
    val existing = editingMedicine
    var medName by remember(existing) { mutableStateOf(existing?.medicineName ?: "") }
    var dosage by remember(existing) { mutableStateOf(existing?.dosage ?: "") }
    var frequency by remember(existing) { mutableStateOf(existing?.frequency ?: "Twice daily") }
    var startDate by
      remember(existing) {
        mutableStateOf(existing?.startDate ?: Translations.currentLocalDateString())
      }
    var endDate by
      remember(existing) {
        mutableStateOf(existing?.endDate ?: Translations.currentLocalDateString())
      }
    var prescribedBy by remember(existing) { mutableStateOf(existing?.prescribedBy ?: "") }
    var instructions by remember(existing) { mutableStateOf(existing?.instructions ?: "After meals") }
    var reminderSchedule by
      remember(existing) { mutableStateOf(existing?.reminderSchedule ?: "08:00 AM, 08:00 PM") }
    var status by remember(existing) { mutableStateOf(existing?.status ?: "Active") }
    var visibleToGuardian by
      remember(existing) { mutableStateOf(existing?.visibleToGuardian ?: true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showDialog = false },
      title = {
        Text(if (existing == null) strings.addMedicineButton else strings.editButton)
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedTextField(
            value = medName,
            onValueChange = {
              medName = it
              errorMsg = null
            },
            label = { Text("${strings.medicineNameLabel} *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = dosage,
            onValueChange = {
              dosage = it
              errorMsg = null
            },
            label = { Text("${strings.dosageLabel} * (e.g. 500 mg)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = frequency,
            onValueChange = { frequency = it },
            label = { Text(strings.frequencyLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = startDate,
            onValueChange = { startDate = it },
            label = { Text(strings.startDateLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = endDate,
            onValueChange = { endDate = it },
            label = { Text(strings.endDateLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = prescribedBy,
            onValueChange = { prescribedBy = it },
            label = { Text(strings.prescribedByLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = reminderSchedule,
            onValueChange = { reminderSchedule = it },
            label = { Text(strings.reminderScheduleLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = { Text(strings.instructionsLabel) },
            modifier = Modifier.fillMaxWidth(),
          )

          Text(text = strings.statusLabel, style = MaterialTheme.typography.labelMedium)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Active", "Upcoming", "Completed", "Missed").forEach { st ->
              FilterChip(
                selected = status == st,
                onClick = { status = st },
                label = { Text(st) },
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { visibleToGuardian = !visibleToGuardian },
          ) {
            Checkbox(checked = visibleToGuardian, onCheckedChange = { visibleToGuardian = it })
            Text(strings.visibleToGuardianLabel, style = MaterialTheme.typography.bodySmall)
          }

          if (errorMsg != null) {
            Text(
              text = errorMsg!!,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (medName.isBlank() || dosage.isBlank()) {
              errorMsg = "Medicine name and dosage are required."
              return@Button
            }
            onSaveMedicine(
              MedicineEntity(
                id = existing?.id ?: 0L,
                userId = userId,
                medicineName = medName.trim(),
                dosage = dosage.trim(),
                frequency = frequency.trim(),
                startDate = startDate.trim(),
                endDate = endDate.trim(),
                prescribedBy = prescribedBy.trim(),
                instructions = instructions.trim(),
                reminderSchedule = reminderSchedule.trim(),
                status = status,
                visibleToGuardian = visibleToGuardian,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
              )
            )
            showDialog = false
          }
        ) {
          Text(strings.saveButton)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDialog = false }) { Text(strings.cancelButton) }
      },
    )
  }
}
