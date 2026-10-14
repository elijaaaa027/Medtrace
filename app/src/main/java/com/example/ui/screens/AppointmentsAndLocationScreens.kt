package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.AppointmentEntity
import com.example.data.HealthRecordEntity
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.i18n.Translations
import com.example.network.FacilityFilterType
import com.example.network.MapsApiResult
import com.example.network.MapsPlacesService
import com.example.network.NearbyHealthcarePlace
import com.example.network.RouteDirectionsResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppointmentsScreen(
  strings: AppStrings,
  userId: String,
  patientName: String,
  appointments: List<AppointmentEntity>,
  records: List<HealthRecordEntity>,
  onSaveAppointment: (AppointmentEntity) -> Unit,
  onOpenRecordViewer: (HealthRecordEntity) -> Unit,
) {
  var selectedTab by remember { mutableStateOf("Upcoming") }
  var showAddOrEditDialog by remember { mutableStateOf(false) }
  var editingAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }
  var appointmentToCancel by remember { mutableStateOf<AppointmentEntity?>(null) }
  var summaryAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }

  val upcomingList = appointments.filter { it.status == "Upcoming" }
  val historyList = appointments.filter { it.status != "Upcoming" }

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
        text = strings.appointmentsTitle,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f),
      )
      Button(
        onClick = {
          editingAppointment = null
          showAddOrEditDialog = true
        }
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(strings.bookAppointmentButton)
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = selectedTab == "Upcoming",
        onClick = { selectedTab = "Upcoming" },
        label = { Text("${strings.upcomingTab} (${upcomingList.size})") },
      )
      FilterChip(
        selected = selectedTab == "History",
        onClick = { selectedTab = "History" },
        label = { Text("${strings.historyTab} (${historyList.size})") },
      )
    }

    val activeList = if (selectedTab == "Upcoming") upcomingList else historyList
    if (activeList.isEmpty()) {
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
            Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp),
          )
          Text(
            text =
              if (selectedTab == "Upcoming") strings.noUpcomingAppointments
              else "No past or cancelled appointments in history.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      activeList.forEach { appt ->
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
                  text = appt.doctorName,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "${appt.department} · ${appt.hospitalOrClinic}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text =
                  if (appt.status == "Cancelled") strings.statusCancelled else appt.status,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color =
                  when (appt.status) {
                    "Upcoming" -> MaterialTheme.colorScheme.primary
                    "Cancelled" -> MaterialTheme.colorScheme.error
                    else -> Color(0xFF16A34A)
                  },
              )
            }

            Text(
              text =
                "${strings.appointmentDateLabel}: ${appt.appointmentDate} · ${strings.appointmentTimeLabel}: ${appt.appointmentTime}",
              fontFamily = FontFamily.Monospace,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
            )

            Text(
              text = "${strings.reasonForVisitLabel}: ${appt.reasonForVisit}",
              style = MaterialTheme.typography.bodySmall,
            )

            if (appt.status == "Cancelled" && appt.cancelledAt.isNotBlank()) {
              Text(
                text = "Cancelled at: ${appt.cancelledAt}",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
              )
            }

            HorizontalDivider()
            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedButton(onClick = { summaryAppointment = appt }) {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.viewSummaryButton)
              }

              OutlinedButton(
                onClick = {
                  editingAppointment = appt
                  showAddOrEditDialog = true
                }
              ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.editButton)
              }

              if (appt.status == "Upcoming") {
                Button(
                  onClick = { appointmentToCancel = appt },
                  colors =
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                  Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(strings.cancelAppointmentButton)
                }
              }
            }
          }
        }
      }
    }
  }

  // Mandatory Confirmation Dialog for Appointment Cancellation
  val cancelTarget = appointmentToCancel
  if (cancelTarget != null) {
    AlertDialog(
      onDismissRequest = { appointmentToCancel = null },
      title = { Text(strings.cancelConfirmTitle) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(strings.cancelConfirmMessage, fontWeight = FontWeight.SemiBold)
          Text(
            text = "${cancelTarget.doctorName} (${cancelTarget.hospitalOrClinic}) on ${cancelTarget.appointmentDate} at ${cancelTarget.appointmentTime}",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val cancelledTimestamp =
              Translations.formatLocalTimestamp(System.currentTimeMillis())
            onSaveAppointment(
              cancelTarget.copy(
                status = "Cancelled",
                cancelledAt = cancelledTimestamp,
              )
            )
            appointmentToCancel = null
            selectedTab = "History"
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text(strings.confirmButton)
        }
      },
      dismissButton = {
        TextButton(onClick = { appointmentToCancel = null }) {
          Text(strings.cancelButton)
        }
      },
    )
  }

  // Detailed Appointment Summary Page / Modal (Requirement #14)
  val summaryTarget = summaryAppointment
  if (summaryTarget != null) {
    val associatedDocs = records.filter { it.appointmentId == summaryTarget.id }
    AlertDialog(
      onDismissRequest = { summaryAppointment = null },
      title = {
        Column {
          Text(strings.appointmentSummaryTitle, fontWeight = FontWeight.Bold)
          Text(
            text = "${summaryTarget.status} · ${summaryTarget.appointmentDate} ${summaryTarget.appointmentTime}",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          SummaryItemRow("Patient", summaryTarget.patientName.ifBlank { patientName.ifBlank { "Patient" } })
          SummaryItemRow(strings.doctorLabel, summaryTarget.doctorName)
          SummaryItemRow(strings.hospitalClinicLabel, summaryTarget.hospitalOrClinic)
          SummaryItemRow(strings.departmentLabel, summaryTarget.department)
          SummaryItemRow(strings.appointmentDateLabel, summaryTarget.appointmentDate)
          SummaryItemRow(strings.appointmentTimeLabel, summaryTarget.appointmentTime)
          SummaryItemRow(strings.reasonForVisitLabel, summaryTarget.reasonForVisit)
          if (summaryTarget.status == "Cancelled" && summaryTarget.cancelledAt.isNotBlank()) {
            SummaryItemRow("Cancelled On", summaryTarget.cancelledAt)
          }

          HorizontalDivider()
          // Only display clinical fields if recorded in database, otherwise show Not recorded
          if (summaryTarget.diagnosis.isNotBlank()) {
            SummaryItemRow(strings.diagnosisLabel, summaryTarget.diagnosis)
          }
          if (summaryTarget.prescriptions.isNotBlank()) {
            SummaryItemRow(strings.prescriptionsLabel, summaryTarget.prescriptions)
          }
          if (summaryTarget.testsOrdered.isNotBlank()) {
            SummaryItemRow(strings.testsOrderedLabel, summaryTarget.testsOrdered)
          }
          if (summaryTarget.followUpInstructions.isNotBlank()) {
            SummaryItemRow(strings.followUpInstructionsLabel, summaryTarget.followUpInstructions)
          }
          if (summaryTarget.billingInfo.isNotBlank()) {
            SummaryItemRow(strings.billingInfoLabel, summaryTarget.billingInfo)
          }

          if (
            summaryTarget.diagnosis.isBlank() &&
              summaryTarget.prescriptions.isBlank() &&
              summaryTarget.testsOrdered.isBlank() &&
              summaryTarget.followUpInstructions.isBlank() &&
              summaryTarget.billingInfo.isBlank()
          ) {
            Text(
              text = "Clinical notes (diagnosis, prescriptions, tests, billing): ${strings.notRecordedLabel}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          HorizontalDivider()
          Text(
            text = "${strings.uploadedDocumentsLabel} (${associatedDocs.size})",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
          if (associatedDocs.isEmpty()) {
            Text(
              text = "No documents linked to this appointment.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            associatedDocs.forEach { doc ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "${doc.fileName} (${doc.recordType})",
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.weight(1f),
                )
                TextButton(
                  onClick = {
                    summaryAppointment = null
                    onOpenRecordViewer(doc)
                  }
                ) {
                  Text(strings.openDocumentButton)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        OutlinedButton(
          onClick = {
            editingAppointment = summaryTarget
            summaryAppointment = null
            showAddOrEditDialog = true
          }
        ) {
          Text(strings.editButton)
        }
      },
      dismissButton = {
        TextButton(onClick = { summaryAppointment = null }) { Text(strings.closeButton) }
      },
    )
  }

  if (showAddOrEditDialog) {
    val existing = editingAppointment
    var doctor by remember(existing) { mutableStateOf(existing?.doctorName ?: "") }
    var hospital by remember(existing) { mutableStateOf(existing?.hospitalOrClinic ?: "") }
    var department by remember(existing) { mutableStateOf(existing?.department ?: "General Medicine") }
    var date by
      remember(existing) {
        mutableStateOf(existing?.appointmentDate ?: Translations.currentLocalDateString())
      }
    var time by remember(existing) { mutableStateOf(existing?.appointmentTime ?: "10:30 AM") }
    var reason by remember(existing) { mutableStateOf(existing?.reasonForVisit ?: "") }
    var status by remember(existing) { mutableStateOf(existing?.status ?: "Upcoming") }
    var diagnosis by remember(existing) { mutableStateOf(existing?.diagnosis ?: "") }
    var prescriptions by remember(existing) { mutableStateOf(existing?.prescriptions ?: "") }
    var testsOrdered by remember(existing) { mutableStateOf(existing?.testsOrdered ?: "") }
    var followUp by remember(existing) { mutableStateOf(existing?.followUpInstructions ?: "") }
    var billing by remember(existing) { mutableStateOf(existing?.billingInfo ?: "") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showAddOrEditDialog = false },
      title = {
        Text(
          if (existing == null) strings.bookAppointmentButton else strings.editButton
        )
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedTextField(
            value = doctor,
            onValueChange = {
              doctor = it
              err = null
            },
            label = { Text("${strings.doctorLabel} *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = hospital,
            onValueChange = {
              hospital = it
              err = null
            },
            label = { Text("${strings.hospitalClinicLabel} *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = department,
            onValueChange = { department = it },
            label = { Text(strings.departmentLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("${strings.appointmentDateLabel} (YYYY-MM-DD)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = time,
            onValueChange = { time = it },
            label = { Text(strings.appointmentTimeLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = reason,
            onValueChange = {
              reason = it
              err = null
            },
            label = { Text("${strings.reasonForVisitLabel} *") },
            modifier = Modifier.fillMaxWidth(),
          )

          HorizontalDivider()
          Text(
            text = "Clinical Summary Details (Optional)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
          OutlinedTextField(
            value = diagnosis,
            onValueChange = { diagnosis = it },
            label = { Text(strings.diagnosisLabel) },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = prescriptions,
            onValueChange = { prescriptions = it },
            label = { Text(strings.prescriptionsLabel) },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = testsOrdered,
            onValueChange = { testsOrdered = it },
            label = { Text(strings.testsOrderedLabel) },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = followUp,
            onValueChange = { followUp = it },
            label = { Text(strings.followUpInstructionsLabel) },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = billing,
            onValueChange = { billing = it },
            label = { Text(strings.billingInfoLabel) },
            modifier = Modifier.fillMaxWidth(),
          )

          if (err != null) {
            Text(
              text = err!!,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (doctor.isBlank() || hospital.isBlank() || reason.isBlank()) {
              err = "Doctor, Hospital/Clinic, and Reason for visit are required."
              return@Button
            }
            onSaveAppointment(
              AppointmentEntity(
                id = existing?.id ?: 0L,
                userId = userId,
                patientName = patientName.ifBlank { "Patient" },
                doctorName = doctor.trim(),
                hospitalOrClinic = hospital.trim(),
                department = department.trim(),
                appointmentDate = date.trim(),
                appointmentTime = time.trim(),
                reasonForVisit = reason.trim(),
                status = status,
                cancelledAt = existing?.cancelledAt ?: "",
                diagnosis = diagnosis.trim(),
                prescriptions = prescriptions.trim(),
                testsOrdered = testsOrdered.trim(),
                followUpInstructions = followUp.trim(),
                billingInfo = billing.trim(),
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
              )
            )
            showAddOrEditDialog = false
          }
        ) {
          Text(strings.saveButton)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddOrEditDialog = false }) { Text(strings.cancelButton) }
      },
    )
  }
}

@Composable
private fun SummaryItemRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(0.4f),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.weight(0.6f),
    )
  }
}

@SuppressLint("MissingPermission", "SetJavaScriptEnabled")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NearbyHospitalsMapScreen(
  strings: AppStrings,
  currentLanguage: AppLanguage,
  onQuotaExceeded: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  // Do NOT silently check/enable GPS on launch; wait for user to tap "Enable Location"
  var userRequestedLocation by remember { mutableStateOf(false) }
  var permissionDenied by remember { mutableStateOf(false) }
  var isLocating by remember { mutableStateOf(false) }
  var userCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }

  var selectedFilter by remember { mutableStateOf(FacilityFilterType.HOSPITALS) }
  var isLoadingPlaces by remember { mutableStateOf(false) }
  var placesError by remember { mutableStateOf<String?>(null) }
  var placesList by remember { mutableStateOf<List<NearbyHealthcarePlace>>(emptyList()) }

  // Active directions modal state
  var activeDirectionsPlace by remember { mutableStateOf<NearbyHealthcarePlace?>(null) }
  var isComputingRoute by remember { mutableStateOf(false) }
  var routeResult by remember { mutableStateOf<RouteDirectionsResult?>(null) }
  var routeError by remember { mutableStateOf<String?>(null) }

  fun fetchPlacesForLocation(lat: Double, lng: Double, filter: FacilityFilterType) {
    scope.launch {
      isLoadingPlaces = true
      placesError = null
      when (
        val res =
          MapsPlacesService.searchNearbyFacilities(
            userLat = lat,
            userLng = lng,
            filterType = filter,
            languageCode = currentLanguage.code,
          )
      ) {
        is MapsApiResult.Success -> {
          placesList = res.data
        }
        MapsApiResult.QuotaExceeded -> {
          onQuotaExceeded()
          placesError = "Google Maps Platform quota reached."
        }
        is MapsApiResult.Error -> {
          placesError = res.message
        }
      }
      isLoadingPlaces = false
    }
  }

  fun obtainCurrentDeviceLocation() {
    isLocating = true
    placesError = null
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    val cts = CancellationTokenSource()
    fusedClient
      .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
      .addOnSuccessListener { loc ->
        if (loc != null) {
          isLocating = false
          val coords = loc.latitude to loc.longitude
          userCoordinates = coords
          fetchPlacesForLocation(coords.first, coords.second, selectedFilter)
        } else {
          fusedClient.lastLocation
            .addOnSuccessListener { lastLoc ->
              isLocating = false
              if (lastLoc != null) {
                val coords = lastLoc.latitude to lastLoc.longitude
                userCoordinates = coords
                fetchPlacesForLocation(coords.first, coords.second, selectedFilter)
              } else {
                placesError =
                  "GPS permission granted, but the device/emulator reported no current GPS fix. Please set a GPS location in the device emulator controls and tap Refresh."
              }
            }
            .addOnFailureListener { e ->
              isLocating = false
              placesError = "Unable to read device GPS: ${e.localizedMessage}"
            }
        }
      }
      .addOnFailureListener { e ->
        isLocating = false
        placesError = "Unable to obtain GPS coordinates: ${e.localizedMessage}"
      }
  }

  val permissionLauncher =
    rememberLauncherForActivityResult(
      ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
      val granted =
        result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
      if (granted) {
        permissionDenied = false
        obtainCurrentDeviceLocation()
      } else {
        permissionDenied = true
      }
    }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = strings.nearbyFacilitiesTitle,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
    )

    val coords = userCoordinates
    if (!userRequestedLocation || coords == null) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
          )
          Text(
            text = strings.locationPermissionPrompt,
            style = MaterialTheme.typography.bodyMedium,
          )

          if (permissionDenied) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = strings.locationDeniedMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp),
              )
            }
          }

          if (placesError != null) {
            Text(
              text = placesError!!,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }

          Button(
            onClick = {
              userRequestedLocation = true
              val hasFine =
                ContextCompat.checkSelfPermission(
                  context,
                  Manifest.permission.ACCESS_FINE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
              val hasCoarse =
                ContextCompat.checkSelfPermission(
                  context,
                  Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED

              if (hasFine || hasCoarse) {
                permissionDenied = false
                obtainCurrentDeviceLocation()
              } else {
                permissionLauncher.launch(
                  arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                  )
                )
              }
            },
            enabled = !isLocating,
          ) {
            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              if (isLocating) "Acquiring GPS Location..." else strings.enableLocationButton,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    } else {
      // User granted location permission and we have real device coordinates
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = "Current Device GPS Location",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = String.format("Lat: %.5f, Lng: %.5f", coords.first, coords.second),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            OutlinedButton(onClick = { obtainCurrentDeviceLocation() }) {
              Text("Refresh GPS")
            }
          }

          // Interactive Google Map (Maps JavaScript API with AdvancedMarkerElement & attribution ID)
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
          ) {
            val mapHtml =
              remember(coords, placesList) {
                MapsPlacesService.buildInteractiveMapHtml(
                  coords.first,
                  coords.second,
                  placesList,
                )
              }
            AndroidView(
              factory = { ctx ->
                WebView(ctx).apply {
                  settings.javaScriptEnabled = true
                  settings.domStorageEnabled = true
                  webViewClient = WebViewClient()
                  addJavascriptInterface(
                    object {
                      @JavascriptInterface
                      fun onQuotaExceeded() {
                        onQuotaExceeded()
                      }
                    },
                    "AndroidBridge",
                  )
                }
              },
              update = { webView ->
                webView.loadDataWithBaseURL(
                  "https://localhost",
                  mapHtml,
                  "text/html",
                  "UTF-8",
                  null,
                )
              },
              modifier = Modifier.fillMaxSize(),
            )
          }

          // Filter Tabs: Hospitals, Clinics, Pharmacies
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
              FacilityFilterType.HOSPITALS to strings.filterHospitals,
              FacilityFilterType.CLINICS to strings.filterClinics,
              FacilityFilterType.PHARMACIES to strings.filterPharmacies,
            ).forEach { (type, label) ->
              FilterChip(
                selected = selectedFilter == type,
                onClick = {
                  selectedFilter = type
                  fetchPlacesForLocation(coords.first, coords.second, type)
                },
                label = { Text(label) },
              )
            }
          }
        }
      }

      if (isLoadingPlaces) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      } else if (placesError != null) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Text(
            text = placesError!!,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
          )
        }
      } else if (placesList.isEmpty()) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Text(
            text = "No nearby healthcare facilities found within 6 km of your current GPS coordinates.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
          )
        }
      } else {
        placesList.forEachIndexed { idx, place ->
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
                verticalAlignment = Alignment.Top,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${idx + 1}. ${place.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = place.categoryLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                Text(
                  text = MapsPlacesService.formatDistance(place.distanceMeters),
                  fontFamily = FontFamily.Monospace,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
              }

              if (place.address.isNotBlank()) {
                Text(
                  text = place.address,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              val metaParts = buildList {
                if (place.phoneNumber.isNotBlank()) add("Phone: ${place.phoneNumber}")
                if (place.rating != null) {
                  add("Rating: ${place.rating} (${place.userRatingCount ?: 0})")
                }
              }
              if (metaParts.isNotEmpty()) {
                Text(
                  text = metaParts.joinToString(" · "),
                  fontFamily = FontFamily.Monospace,
                  style = MaterialTheme.typography.bodySmall,
                )
              }

              HorizontalDivider()
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Button(
                  onClick = {
                    activeDirectionsPlace = place
                    routeResult = null
                    routeError = null
                    scope.launch {
                      isComputingRoute = true
                      when (
                        val r =
                          MapsPlacesService.computeRouteDirections(
                            originLat = coords.first,
                            originLng = coords.second,
                            destLat = place.latitude,
                            destLng = place.longitude,
                          )
                      ) {
                        is MapsApiResult.Success -> routeResult = r.data
                        MapsApiResult.QuotaExceeded -> {
                          onQuotaExceeded()
                          routeError = "Google Maps Platform quota reached."
                        }
                        is MapsApiResult.Error -> routeError = r.message
                      }
                      isComputingRoute = false
                    }
                  }
                ) {
                  Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(strings.getDirectionsButton)
                }

                if (place.phoneNumber.isNotBlank()) {
                  OutlinedButton(
                    onClick = {
                      val dialIntent =
                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${place.phoneNumber}"))
                      context.startActivity(dialIntent)
                    }
                  ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.callFacilityButton)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Directions Modal (Routes API v2 + Open in Google Maps Navigation)
  val targetPlace = activeDirectionsPlace
  val currentCoords = userCoordinates
  if (targetPlace != null && currentCoords != null) {
    AlertDialog(
      onDismissRequest = { activeDirectionsPlace = null },
      title = {
        Column {
          Text(text = "${strings.getDirectionsButton}: ${targetPlace.name}", fontWeight = FontWeight.Bold)
          Text(
            text = targetPlace.address,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (isComputingRoute) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp))
              Text("Calculating route via Google Maps Routes API...")
            }
          } else if (routeResult != null) {
            val r = routeResult!!
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "Travel Time: ${r.durationText}",
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                  text =
                    "Distance: ${MapsPlacesService.formatDistance(r.distanceMeters.toFloat())}",
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
            }
            if (r.steps.isNotEmpty()) {
              Text(
                text = "Turn-by-Turn Steps:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
              r.steps.forEachIndexed { i, step ->
                Text(
                  text = "${i + 1}. $step",
                  style = MaterialTheme.typography.bodySmall,
                )
              }
            }
          } else if (routeError != null) {
            Text(
              text = routeError!!,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val navUri =
              if (targetPlace.directionsUri.isNotBlank()) {
                Uri.parse(targetPlace.directionsUri)
              } else {
                Uri.parse(
                  "https://www.google.com/maps/dir/?api=1&origin=${currentCoords.first},${currentCoords.second}" +
                    "&destination=${targetPlace.latitude},${targetPlace.longitude}" +
                    "&destination_place_id=${targetPlace.id}"
                )
              }
            context.startActivity(Intent(Intent.ACTION_VIEW, navUri))
          }
        ) {
          Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Launch Google Maps Turn-by-Turn")
        }
      },
      dismissButton = {
        TextButton(onClick = { activeDirectionsPlace = null }) { Text(strings.closeButton) }
      },
    )
  }
}
