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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MonitorHeart
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.example.data.HealthSnapshotEntity
import com.example.data.InsurancePolicyEntity
import com.example.data.MedicineEntity
import com.example.i18n.AppStrings
import com.example.i18n.Translations

val INDIAN_INSURANCE_PROVIDERS =
  listOf(
    "LIC",
    "Star Health and Allied Insurance",
    "HDFC ERGO",
    "ICICI Lombard",
    "Niva Bupa",
    "Care Health Insurance",
    "Aditya Birla Health Insurance",
    "Bajaj Allianz General Insurance",
    "Tata AIG",
    "SBI General Insurance",
    "New India Assurance",
  )

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsuranceScreen(
  strings: AppStrings,
  userId: String,
  policies: List<InsurancePolicyEntity>,
  onSavePolicy: (InsurancePolicyEntity) -> Unit,
  onDeletePolicy: (InsurancePolicyEntity) -> Unit,
) {
  var editingPolicy by remember { mutableStateOf<InsurancePolicyEntity?>(null) }
  var showDialog by remember { mutableStateOf(false) }

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
          text = strings.insuranceTitle,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = strings.demoProvidersNote,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Button(
        onClick = {
          editingPolicy = null
          showDialog = true
        }
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(strings.addInsuranceButton)
      }
    }

    if (policies.isEmpty()) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(28.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            Icons.Default.HealthAndSafety,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(44.dp),
          )
          Text(
            text = strings.insuranceNotAdded,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
          )
          Button(
            onClick = {
              editingPolicy = null
              showDialog = true
            }
          ) {
            Text(strings.addInsuranceButton)
          }
        }
      }
    } else {
      policies.forEach { policy ->
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
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = policy.providerName,
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "${strings.policyTypeLabel}: ${policy.policyType}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              // Active Cover Indicator
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = if (policy.isActive) Color(0xFF16A34A) else Color(0xFF64748B),
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text =
                    if (policy.isActive) strings.activeCoverBadge else strings.inactiveCoverBadge,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = if (policy.isActive) Color(0xFF16A34A) else Color(0xFF64748B),
                )
              }
            }

            HorizontalDivider()

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column {
                Text(
                  text = strings.policyNumberLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = policy.policyNumber,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.SemiBold,
                  style = MaterialTheme.typography.bodyMedium,
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = strings.coverageAmountLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = policy.coverageAmount,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }

            Text(
              text =
                "${strings.startDateLabel}: ${policy.startDate} · ${strings.expiryDateLabel}: ${policy.expiryDate}",
              fontFamily = FontFamily.Monospace,
              style = MaterialTheme.typography.bodySmall,
            )

            if (policy.coveredServices.isNotBlank()) {
              Text(
                text = "${strings.coveredServicesLabel}: ${policy.coveredServices}",
                style = MaterialTheme.typography.bodySmall,
              )
            }

            if (policy.claimInfo.isNotBlank()) {
              Text(
                text = "${strings.claimInfoLabel}: ${policy.claimInfo}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
            ) {
              IconButton(
                onClick = {
                  editingPolicy = policy
                  showDialog = true
                }
              ) {
                Icon(Icons.Default.Edit, contentDescription = strings.editInsuranceButton)
              }
              IconButton(onClick = { onDeletePolicy(policy) }) {
                Icon(
                  Icons.Default.Delete,
                  contentDescription = strings.deleteButton,
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            }
          }
        }
      }
    }
  }

  if (showDialog) {
    val existing = editingPolicy
    var provider by
      remember(existing) {
        mutableStateOf(existing?.providerName ?: INDIAN_INSURANCE_PROVIDERS.first())
      }
    var policyNum by remember(existing) { mutableStateOf(existing?.policyNumber ?: "") }
    var policyType by
      remember(existing) {
        mutableStateOf(existing?.policyType ?: "Family Floater Health Policy")
      }
    var coverageAmount by
      remember(existing) { mutableStateOf(existing?.coverageAmount ?: "₹5,00,000") }
    var startDate by
      remember(existing) {
        mutableStateOf(existing?.startDate ?: Translations.currentLocalDateString())
      }
    var expiryDate by
      remember(existing) { mutableStateOf(existing?.expiryDate ?: "2027-10-01") }
    var isActive by remember(existing) { mutableStateOf(existing?.isActive ?: true) }
    var coveredServices by
      remember(existing) {
        mutableStateOf(
          existing?.coveredServices
            ?: "In-patient Hospitalization, Pre & Post Hospitalization, Day Care Procedures, Annual Health Check-up"
        )
      }
    var claimInfo by
      remember(existing) {
        mutableStateOf(
          existing?.claimInfo
            ?: "Cashless Hospital Network Desk · No Active Claims Pending"
        )
      }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showDialog = false },
      title = {
        Text(
          if (existing == null) strings.addInsuranceButton else strings.editInsuranceButton
        )
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "${strings.providerNameLabel} (Demo Selectable Providers)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
          )
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            INDIAN_INSURANCE_PROVIDERS.forEach { p ->
              FilterChip(
                selected = provider == p,
                onClick = { provider = p },
                label = { Text(p, style = MaterialTheme.typography.labelSmall) },
              )
            }
          }

          OutlinedTextField(
            value = policyNum,
            onValueChange = {
              policyNum = it
              errorMsg = null
            },
            label = { Text("${strings.policyNumberLabel} *") },
            placeholder = { Text("e.g. SHAI/2026/8849201") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          OutlinedTextField(
            value = policyType,
            onValueChange = { policyType = it },
            label = { Text(strings.policyTypeLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          OutlinedTextField(
            value = coverageAmount,
            onValueChange = { coverageAmount = it },
            label = { Text("${strings.coverageAmountLabel} *") },
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
            value = expiryDate,
            onValueChange = { expiryDate = it },
            label = { Text(strings.expiryDateLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          OutlinedTextField(
            value = coveredServices,
            onValueChange = { coveredServices = it },
            label = { Text(strings.coveredServicesLabel) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
          )

          OutlinedTextField(
            value = claimInfo,
            onValueChange = { claimInfo = it },
            label = { Text(strings.claimInfoLabel) },
            modifier = Modifier.fillMaxWidth(),
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { isActive = !isActive },
          ) {
            Checkbox(checked = isActive, onCheckedChange = { isActive = it })
            Text(strings.activeCoverBadge, style = MaterialTheme.typography.bodySmall)
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
            if (policyNum.isBlank() || coverageAmount.isBlank()) {
              errorMsg = "Policy number and coverage amount are required."
              return@Button
            }
            onSavePolicy(
              InsurancePolicyEntity(
                id = existing?.id ?: 0L,
                userId = userId,
                providerName = provider,
                policyNumber = policyNum.trim(),
                policyType = policyType.trim(),
                coverageAmount = coverageAmount.trim(),
                startDate = startDate.trim(),
                expiryDate = expiryDate.trim(),
                isActive = isActive,
                coveredServices = coveredServices.trim(),
                claimInfo = claimInfo.trim(),
                updatedAt = System.currentTimeMillis(),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthSnapshotScreen(
  strings: AppStrings,
  userId: String,
  snapshot: HealthSnapshotEntity?,
  activeMedicines: List<MedicineEntity>,
  onSaveSnapshot: (HealthSnapshotEntity) -> Unit,
) {
  var showEditDialog by remember { mutableStateOf(false) }

  val isSnapshotPopulated =
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
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = strings.healthSnapshotCardTitle,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f),
      )
      Button(onClick = { showEditDialog = true }) {
        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(strings.updateHealthSnapshotButton)
      }
    }

    if (!isSnapshotPopulated || snapshot == null) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(28.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            Icons.Default.MonitorHeart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp),
          )
          Text(
            text = strings.healthSnapshotUnavailable,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
          )
          Button(onClick = { showEditDialog = true }) {
            Text(strings.updateHealthSnapshotButton)
          }
        }
      }
    } else {
      // Clinical Summary Card
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
            Text(
              text = "Clinical Summary",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Updated: ${Translations.formatLocalTimestamp(snapshot.updatedAt)}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          SnapshotFieldRow(strings.bloodGroupLabel, snapshot.bloodGroup.ifBlank { strings.notRecordedLabel })
          SnapshotFieldRow(strings.allergiesLabel, snapshot.allergies.ifBlank { strings.notRecordedLabel })
          SnapshotFieldRow(
            strings.currentMedicationsLabel,
            snapshot.currentMedicationsSummary.ifBlank {
              if (activeMedicines.isNotEmpty()) {
                activeMedicines.joinToString(", ") { "${it.medicineName} (${it.dosage})" }
              } else {
                strings.notRecordedLabel
              }
            },
          )
          SnapshotFieldRow(strings.recentDiagnosisLabel, snapshot.recentDiagnosis.ifBlank { strings.notRecordedLabel })
          SnapshotFieldRow(strings.recentLabResultsLabel, snapshot.recentLabResults.ifBlank { strings.notRecordedLabel })
          SnapshotFieldRow(strings.vaccinationStatusLabel, snapshot.vaccinationStatus.ifBlank { strings.notRecordedLabel })
          SnapshotFieldRow(strings.medicalConditionsLabel, snapshot.medicalConditions.ifBlank { strings.notRecordedLabel })
        }
      }

      // Recent Vital Measurements Card
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
              Icons.Default.Favorite,
              contentDescription = null,
              tint = Color(0xFFDC2626),
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = strings.vitalMeasurementsTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
          HorizontalDivider()
          SnapshotFieldRow(strings.bloodPressureLabel, snapshot.bloodPressure.ifBlank { strings.notRecordedLabel }, true)
          SnapshotFieldRow(strings.heartRateLabel, snapshot.heartRate.ifBlank { strings.notRecordedLabel }, true)
          SnapshotFieldRow(strings.spo2Label, snapshot.spo2.ifBlank { strings.notRecordedLabel }, true)
          SnapshotFieldRow(strings.temperatureLabel, snapshot.temperature.ifBlank { strings.notRecordedLabel }, true)
          SnapshotFieldRow(strings.weightLabel, snapshot.weightKg.ifBlank { strings.notRecordedLabel }, true)
          SnapshotFieldRow(strings.bloodGlucoseLabel, snapshot.bloodGlucose.ifBlank { strings.notRecordedLabel }, true)
        }
      }
    }
  }

  if (showEditDialog) {
    var bloodGroup by remember(snapshot) { mutableStateOf(snapshot?.bloodGroup ?: "") }
    var allergies by remember(snapshot) { mutableStateOf(snapshot?.allergies ?: "") }
    var medsSummary by
      remember(snapshot) { mutableStateOf(snapshot?.currentMedicationsSummary ?: "") }
    var diagnosis by remember(snapshot) { mutableStateOf(snapshot?.recentDiagnosis ?: "") }
    var labResults by remember(snapshot) { mutableStateOf(snapshot?.recentLabResults ?: "") }
    var vaccination by remember(snapshot) { mutableStateOf(snapshot?.vaccinationStatus ?: "") }
    var conditions by remember(snapshot) { mutableStateOf(snapshot?.medicalConditions ?: "") }
    var bp by remember(snapshot) { mutableStateOf(snapshot?.bloodPressure ?: "") }
    var hr by remember(snapshot) { mutableStateOf(snapshot?.heartRate ?: "") }
    var spo2 by remember(snapshot) { mutableStateOf(snapshot?.spo2 ?: "") }
    var temp by remember(snapshot) { mutableStateOf(snapshot?.temperature ?: "") }
    var weight by remember(snapshot) { mutableStateOf(snapshot?.weightKg ?: "") }
    var glucose by remember(snapshot) { mutableStateOf(snapshot?.bloodGlucose ?: "") }

    AlertDialog(
      onDismissRequest = { showEditDialog = false },
      title = { Text(strings.updateHealthSnapshotButton) },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(text = strings.bloodGroupLabel, style = MaterialTheme.typography.labelMedium)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-").forEach { bg ->
              FilterChip(
                selected = bloodGroup == bg,
                onClick = { bloodGroup = bg },
                label = { Text(bg) },
              )
            }
          }

          OutlinedTextField(
            value = allergies,
            onValueChange = { allergies = it },
            label = { Text(strings.allergiesLabel) },
            placeholder = { Text("e.g. Penicillin, Dust mites, None") },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = medsSummary,
            onValueChange = { medsSummary = it },
            label = { Text(strings.currentMedicationsLabel) },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = diagnosis,
            onValueChange = { diagnosis = it },
            label = { Text(strings.recentDiagnosisLabel) },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = labResults,
            onValueChange = { labResults = it },
            label = { Text(strings.recentLabResultsLabel) },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = vaccination,
            onValueChange = { vaccination = it },
            label = { Text(strings.vaccinationStatusLabel) },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = conditions,
            onValueChange = { conditions = it },
            label = { Text(strings.medicalConditionsLabel) },
            modifier = Modifier.fillMaxWidth(),
          )

          HorizontalDivider()
          Text(
            text = strings.vitalMeasurementsTitle,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
          OutlinedTextField(
            value = bp,
            onValueChange = { bp = it },
            label = { Text(strings.bloodPressureLabel) },
            placeholder = { Text("120/80 mmHg") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = hr,
            onValueChange = { hr = it },
            label = { Text(strings.heartRateLabel) },
            placeholder = { Text("72 bpm") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = spo2,
            onValueChange = { spo2 = it },
            label = { Text(strings.spo2Label) },
            placeholder = { Text("98%") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = temp,
            onValueChange = { temp = it },
            label = { Text(strings.temperatureLabel) },
            placeholder = { Text("98.4 °F") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text(strings.weightLabel) },
            placeholder = { Text("68 kg") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = glucose,
            onValueChange = { glucose = it },
            label = { Text(strings.bloodGlucoseLabel) },
            placeholder = { Text("94 mg/dL (Fasting)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSaveSnapshot(
              HealthSnapshotEntity(
                userId = userId,
                bloodGroup = bloodGroup.trim(),
                allergies = allergies.trim(),
                currentMedicationsSummary = medsSummary.trim(),
                recentDiagnosis = diagnosis.trim(),
                recentLabResults = labResults.trim(),
                vaccinationStatus = vaccination.trim(),
                medicalConditions = conditions.trim(),
                bloodPressure = bp.trim(),
                heartRate = hr.trim(),
                spo2 = spo2.trim(),
                temperature = temp.trim(),
                weightKg = weight.trim(),
                bloodGlucose = glucose.trim(),
                updatedAt = System.currentTimeMillis(),
              )
            )
            showEditDialog = false
          }
        ) {
          Text(strings.saveButton)
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditDialog = false }) { Text(strings.cancelButton) }
      },
    )
  }
}

@Composable
private fun SnapshotFieldRow(
  label: String,
  value: String,
  isMonospaceValue: Boolean = false,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(0.45f),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
      fontFamily = if (isMonospaceValue) FontFamily.Monospace else FontFamily.Default,
      modifier = Modifier.weight(0.55f),
    )
  }
}
