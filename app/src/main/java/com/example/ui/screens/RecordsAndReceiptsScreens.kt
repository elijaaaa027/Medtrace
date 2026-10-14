package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.AppointmentEntity
import com.example.data.HealthRecordEntity
import com.example.data.ReceiptEntity
import com.example.i18n.AppStrings
import com.example.i18n.Translations
import com.example.security.AuthAndFileManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

val HEALTH_RECORD_CATEGORIES =
  listOf(
    "Lab report",
    "Prescription",
    "Scan",
    "Discharge summary",
    "Medical certificate",
    "Insurance document",
    "Other",
  )

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthRecordsScreen(
  strings: AppStrings,
  userId: String,
  uploaderName: String,
  records: List<HealthRecordEntity>,
  appointments: List<AppointmentEntity>,
  onAddRecord: (HealthRecordEntity) -> Unit,
  onDeleteRecord: (HealthRecordEntity) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var selectedCategoryFilter by remember { mutableStateOf("All") }
  var showUploadDialog by remember { mutableStateOf(false) }
  var viewingRecord by remember { mutableStateOf<HealthRecordEntity?>(null) }
  var recordToDelete by remember { mutableStateOf<HealthRecordEntity?>(null) }
  var statusBanner by remember { mutableStateOf<String?>(null) }

  // Download launcher for exporting a health record file
  var recordPendingDownload by remember { mutableStateOf<HealthRecordEntity?>(null) }
  val downloadDocumentLauncher =
    rememberLauncherForActivityResult(
      ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destUri: Uri? ->
      val rec = recordPendingDownload
      if (destUri != null && rec != null) {
        try {
          val srcFile = File(rec.localFilePath)
          if (srcFile.exists()) {
            FileInputStream(srcFile).use { input ->
              context.contentResolver.openOutputStream(destUri)?.use { output ->
                input.copyTo(output)
              }
            }
            statusBanner = "Downloaded '${rec.fileName}' to device storage."
          }
        } catch (e: Exception) {
          statusBanner = "Download failed: ${e.localizedMessage}"
        }
      }
      recordPendingDownload = null
    }

  val filteredRecords =
    if (selectedCategoryFilter == "All") records
    else records.filter { it.recordType == selectedCategoryFilter }

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
          text = strings.healthRecordsTitle,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Upload PDF, JPG, JPEG, or PNG medical records with secure in-app preview.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Button(onClick = { showUploadDialog = true }) {
        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(strings.uploadRecordButton)
      }
    }

    if (statusBanner != null) {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = statusBanner!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
          )
          TextButton(onClick = { statusBanner = null }) {
            Text(strings.closeButton)
          }
        }
      }
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = selectedCategoryFilter == "All",
        onClick = { selectedCategoryFilter = "All" },
        label = { Text("All") },
      )
      HEALTH_RECORD_CATEGORIES.forEach { cat ->
        FilterChip(
          selected = selectedCategoryFilter == cat,
          onClick = { selectedCategoryFilter = cat },
          label = { Text(cat) },
        )
      }
    }

    if (filteredRecords.isEmpty()) {
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
            Icons.Default.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp),
          )
          Text(
            text = strings.noRecordsUploaded,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Button(onClick = { showUploadDialog = true }) {
            Text(strings.uploadRecordButton)
          }
        }
      }
    } else {
      filteredRecords.forEach { rec ->
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
                  text = rec.fileName,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text =
                    "${rec.recordType} · Uploaded by ${rec.uploadedBy} · " +
                      Translations.formatLocalTimestamp(rec.uploadDate),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            if (rec.appointmentLabel.isNotBlank()) {
              Text(
                text = "${strings.associatedAppointmentLabel}: ${rec.appointmentLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
              )
            }

            if (rec.notes.isNotBlank()) {
              Text(
                text = rec.notes,
                style = MaterialTheme.typography.bodySmall,
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
                  if (rec.sharedWithGuardian) "Shared with Guardian" else "Private Record",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewingRecord = rec }) {
                  Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(strings.openDocumentButton)
                }
                OutlinedButton(
                  onClick = {
                    recordPendingDownload = rec
                    downloadDocumentLauncher.launch(rec.fileName)
                  }
                ) {
                  Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(strings.downloadDocumentButton)
                }
                IconButton(onClick = { recordToDelete = rec }) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = strings.deleteDocumentButton,
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // Upload Health Record Dialog with explicit file picker, cancel option, and upload progress
  if (showUploadDialog) {
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var generatedSampleFile by remember { mutableStateOf<File?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedMimeType by remember { mutableStateOf("application/pdf") }
    var selectedFileSize by remember { mutableStateOf(0L) }
    var category by remember { mutableStateOf(HEALTH_RECORD_CATEGORIES.first()) }
    var selectedAppointmentId by remember { mutableStateOf<Long?>(null) }
    var selectedAppointmentLabel by remember { mutableStateOf("") }
    var shareWithGuardian by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    val documentPicker =
      rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
          val meta = AuthAndFileManager.inspectPickedFile(context, uri)
          if (meta.isSupported) {
            pickedUri = uri
            generatedSampleFile = null
            selectedFileName = meta.displayName
            selectedMimeType = meta.mimeType
            selectedFileSize = meta.sizeBytes
            uploadError = null
          } else {
            uploadError = "Unsupported file format (${meta.extension}). Allowed: PDF, JPG, JPEG, PNG."
          }
        }
      }

    AlertDialog(
      onDismissRequest = { if (!isUploading) showUploadDialog = false },
      title = { Text(strings.uploadRecordButton) },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedButton(
            onClick = {
              documentPicker.launch(
                arrayOf("application/pdf", "image/jpeg", "image/png")
              )
            },
            enabled = !isUploading,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.selectFileButton)
          }

          // Helper for emulator testing where device Downloads folder has no PDFs yet
          TextButton(
            onClick = {
              val realPdf =
                AuthAndFileManager.createRealPdfDocumentInPrivateStorage(
                  context = context,
                  userId = userId,
                  fileName = "${category.lowercase().replace(' ', '_')}_${System.currentTimeMillis() % 10000}.pdf",
                  title = "$category — Patient: ${uploaderName.ifBlank { "Patient" }}",
                  lines =
                    listOf(
                      "Date Generated: ${Translations.formatLocalTimestamp(System.currentTimeMillis())}",
                      "Record Category: $category",
                      "Uploaded By: ${uploaderName.ifBlank { "Patient" }}",
                      "Notes: ${notes.ifBlank { "Uploaded via MedTrace Document Manager" }}",
                    ),
                )
              generatedSampleFile = realPdf
              pickedUri = null
              selectedFileName = realPdf.name
              selectedMimeType = "application/pdf"
              selectedFileSize = realPdf.length()
              uploadError = null
            },
            enabled = !isUploading,
          ) {
            Text("Generate Real Test PDF File on Device [DEMO/TEST]")
          }

          if (selectedFileName.isNotBlank()) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Selected: $selectedFileName",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "Type: $selectedMimeType · ${(selectedFileSize / 1024).coerceAtLeast(1)} KB",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall,
                  )
                }
                if (!isUploading) {
                  TextButton(
                    onClick = {
                      pickedUri = null
                      generatedSampleFile = null
                      selectedFileName = ""
                    }
                  ) {
                    Text(strings.cancelButton)
                  }
                }
              }
            }
          }

          Text(text = strings.recordCategoryLabel, style = MaterialTheme.typography.labelMedium)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HEALTH_RECORD_CATEGORIES.forEach { cat ->
              FilterChip(
                selected = category == cat,
                onClick = { if (!isUploading) category = cat },
                label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
              )
            }
          }

          if (appointments.isNotEmpty()) {
            Text(
              text = strings.associatedAppointmentLabel,
              style = MaterialTheme.typography.labelMedium,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              FilterChip(
                selected = selectedAppointmentId == null,
                onClick = {
                  selectedAppointmentId = null
                  selectedAppointmentLabel = ""
                },
                label = { Text("None") },
              )
              appointments.forEach { appt ->
                val label = "${appt.appointmentDate} · ${appt.doctorName}"
                FilterChip(
                  selected = selectedAppointmentId == appt.id,
                  onClick = {
                    selectedAppointmentId = appt.id
                    selectedAppointmentLabel = label
                  },
                  label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
              }
            }
          }

          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Document Notes (Optional)") },
            enabled = !isUploading,
            modifier = Modifier.fillMaxWidth(),
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(enabled = !isUploading) { shareWithGuardian = !shareWithGuardian },
          ) {
            Checkbox(
              checked = shareWithGuardian,
              onCheckedChange = { if (!isUploading) shareWithGuardian = it },
            )
            Text(strings.shareWithGuardianLabel, style = MaterialTheme.typography.bodySmall)
          }

          if (isUploading) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Uploading '$selectedFileName'... ${(uploadProgress * 100).toInt()}%",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
              )
              LinearProgressIndicator(
                progress = { uploadProgress },
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }

          if (uploadError != null) {
            Text(
              text = uploadError!!,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (selectedFileName.isBlank()) {
              uploadError = "Please select a supported file (PDF, JPG, JPEG, PNG) before uploading."
              return@Button
            }
            scope.launch {
              isUploading = true
              uploadProgress = 0.25f
              delay(180)
              uploadProgress = 0.65f
              val savedFile =
                when {
                  pickedUri != null ->
                    AuthAndFileManager.copyUriToPrivateUserStorage(
                      context,
                      userId,
                      pickedUri!!,
                      selectedFileName,
                    )
                  generatedSampleFile != null -> generatedSampleFile
                  else -> null
                }
              delay(180)
              uploadProgress = 1.0f
              if (savedFile != null && savedFile.exists()) {
                onAddRecord(
                  HealthRecordEntity(
                    userId = userId,
                    fileName = selectedFileName,
                    localFilePath = savedFile.absolutePath,
                    mimeType = selectedMimeType,
                    fileSizeBytes = savedFile.length(),
                    recordType = category,
                    uploadedBy = uploaderName.ifBlank { "Patient" },
                    uploadDate = System.currentTimeMillis(),
                    appointmentId = selectedAppointmentId,
                    appointmentLabel = selectedAppointmentLabel,
                    sharedWithGuardian = shareWithGuardian,
                    notes = notes.trim(),
                  )
                )
                statusBanner = "Successfully uploaded '$selectedFileName'."
                isUploading = false
                showUploadDialog = false
              } else {
                isUploading = false
                uploadError = "Unable to copy selected file into secure storage."
              }
            }
          },
          enabled = !isUploading,
        ) {
          Text(strings.confirmUploadButton)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showUploadDialog = false },
          enabled = !isUploading,
        ) {
          Text(strings.cancelUploadButton)
        }
      },
    )
  }

  // Secure In-App Document Viewer Modal (Renders actual PDF via PdfRenderer or Image via Coil)
  val recordToView = viewingRecord
  if (recordToView != null) {
    SecureDocumentViewerDialog(
      strings = strings,
      record = recordToView,
      onDismiss = { viewingRecord = null },
      onDownload = {
        recordPendingDownload = recordToView
        downloadDocumentLauncher.launch(recordToView.fileName)
      },
    )
  }

  val delRec = recordToDelete
  if (delRec != null) {
    AlertDialog(
      onDismissRequest = { recordToDelete = null },
      title = { Text(strings.deleteDocumentButton) },
      text = { Text("Are you sure you want to permanently delete '${delRec.fileName}'?") },
      confirmButton = {
        Button(
          onClick = {
            try {
              File(delRec.localFilePath).delete()
            } catch (_: Exception) {}
            onDeleteRecord(delRec)
            recordToDelete = null
          }
        ) {
          Text(strings.deleteButton)
        }
      },
      dismissButton = {
        TextButton(onClick = { recordToDelete = null }) { Text(strings.cancelButton) }
      },
    )
  }
}

@Composable
fun SecureDocumentViewerDialog(
  strings: AppStrings,
  record: HealthRecordEntity,
  onDismiss: () -> Unit,
  onDownload: () -> Unit,
) {
  val file = remember(record.localFilePath) { File(record.localFilePath) }
  var pdfBitmap by remember(record.localFilePath) { mutableStateOf<Bitmap?>(null) }

  DisposableEffect(record.localFilePath) {
    var descriptor: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    try {
      if (file.exists() && (record.mimeType.contains("pdf", true) || file.name.endsWith(".pdf", true))) {
        descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(descriptor)
        if (renderer.pageCount > 0) {
          val page = renderer.openPage(0)
          val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
          bmp.eraseColor(android.graphics.Color.WHITE)
          page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
          page.close()
          pdfBitmap = bmp
        }
      }
    } catch (_: Exception) {}
    onDispose {
      try {
        renderer?.close()
        descriptor?.close()
      } catch (_: Exception) {}
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Text(text = record.fileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
          text = "${record.recordType} · ${Translations.formatLocalTimestamp(record.uploadDate)}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (record.appointmentLabel.isNotBlank()) {
          Text(
            text = "${strings.associatedAppointmentLabel}: ${record.appointmentLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Text(
          text = "Uploaded by: ${record.uploadedBy} · Size: ${(record.fileSizeBytes / 1024).coerceAtLeast(1)} KB",
          fontFamily = FontFamily.Monospace,
          style = MaterialTheme.typography.labelSmall,
        )

        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(320.dp)
              .background(Color.White, RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center,
        ) {
          val renderedPdf = pdfBitmap
          when {
            renderedPdf != null -> {
              Image(
                bitmap = renderedPdf.asImageBitmap(),
                contentDescription = record.fileName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(8.dp),
              )
            }
            file.exists() &&
              (record.mimeType.startsWith("image/") ||
                file.name.endsWith(".jpg", true) ||
                file.name.endsWith(".jpeg", true) ||
                file.name.endsWith(".png", true)) -> {
              AsyncImage(
                model = file,
                contentDescription = record.fileName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(8.dp),
              )
            }
            else -> {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp),
              ) {
                Icon(
                  Icons.Default.Description,
                  contentDescription = null,
                  tint = Color(0xFF0F766E),
                  modifier = Modifier.size(48.dp),
                )
                Text(
                  text = record.fileName,
                  color = Color(0xFF0F172A),
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "Document verified in private storage (${record.mimeType})",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFF475569),
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDownload) {
        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(strings.downloadDocumentButton)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.closeButton) }
    },
  )
}

@Composable
fun ReceiptsScreen(
  strings: AppStrings,
  userId: String,
  receipts: List<ReceiptEntity>,
  appointments: List<AppointmentEntity>,
  onSaveReceipt: (ReceiptEntity) -> Unit,
  onDeleteReceipt: (ReceiptEntity) -> Unit,
) {
  val context = LocalContext.current
  var showAddDialog by remember { mutableStateOf(false) }
  var viewingReceipt by remember { mutableStateOf<ReceiptEntity?>(null) }
  var downloadBanner by remember { mutableStateOf<String?>(null) }

  var receiptPendingDownload by remember { mutableStateOf<ReceiptEntity?>(null) }
  val receiptDownloadLauncher =
    rememberLauncherForActivityResult(
      ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
      val rec = receiptPendingDownload
      if (uri != null && rec != null) {
        try {
          val content = buildReceiptPlainText(rec)
          context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(content.toByteArray())
          }
          downloadBanner = "Receipt ${rec.receiptNumber} saved to device."
        } catch (e: Exception) {
          downloadBanner = "Unable to save receipt: ${e.localizedMessage}"
        }
      }
      receiptPendingDownload = null
    }

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
        text = strings.receiptsTitle,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f),
      )
      Button(onClick = { showAddDialog = true }) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(strings.addReceiptButton)
      }
    }

    if (downloadBanner != null) {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = downloadBanner!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          TextButton(onClick = { downloadBanner = null }) { Text(strings.closeButton) }
        }
      }
    }

    if (receipts.isEmpty()) {
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
            Icons.Default.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp),
          )
          Text(
            text = strings.noReceiptsFound,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Button(onClick = { showAddDialog = true }) { Text(strings.addReceiptButton) }
        }
      }
    } else {
      receipts.forEach { rec ->
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
                  text = rec.hospitalOrClinic,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text =
                    "${strings.receiptNumberLabel}: ${rec.receiptNumber} · ${rec.receiptDate}",
                  fontFamily = FontFamily.Monospace,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text = String.format("₹%.2f", rec.totalAmountInr),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }

            if (rec.doctorName.isNotBlank()) {
              Text(
                text = "${strings.doctorLabel}: ${rec.doctorName} · ${rec.paymentMethod} (${rec.paymentStatus})",
                style = MaterialTheme.typography.bodySmall,
              )
            }

            HorizontalDivider()
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
            ) {
              OutlinedButton(onClick = { viewingReceipt = rec }) {
                Text(strings.viewReceiptButton)
              }
              Spacer(modifier = Modifier.width(8.dp))
              OutlinedButton(
                onClick = {
                  receiptPendingDownload = rec
                  receiptDownloadLauncher.launch("Receipt_${rec.receiptNumber}.txt")
                }
              ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.downloadReceiptButton)
              }
              IconButton(onClick = { onDeleteReceipt(rec) }) {
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

  if (showAddDialog) {
    var receiptNumber by remember { mutableStateOf("RCPT-${System.currentTimeMillis() % 100000}") }
    var hospital by remember { mutableStateOf("") }
    var doctor by remember { mutableStateOf("") }
    var receiptDate by remember { mutableStateOf(Translations.currentLocalDateString()) }
    var paymentMethod by remember { mutableStateOf("UPI") }
    var item1Name by remember { mutableStateOf("Specialist Consultation Fee") }
    var item1Amount by remember { mutableStateOf("800") }
    var item2Name by remember { mutableStateOf("Diagnostic Lab Panel") }
    var item2Amount by remember { mutableStateOf("650") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text(strings.addReceiptButton) },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedTextField(
            value = receiptNumber,
            onValueChange = { receiptNumber = it },
            label = { Text(strings.receiptNumberLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = hospital,
            onValueChange = {
              hospital = it
              errorMsg = null
            },
            label = { Text("${strings.hospitalClinicLabel} *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = doctor,
            onValueChange = { doctor = it },
            label = { Text(strings.doctorLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = receiptDate,
            onValueChange = { receiptDate = it },
            label = { Text("Date") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = paymentMethod,
            onValueChange = { paymentMethod = it },
            label = { Text("Payment Method (UPI / Card / Cash / Insurance)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          HorizontalDivider()
          Text(
            text = strings.itemizedChargesLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = item1Name,
              onValueChange = { item1Name = it },
              label = { Text("Charge 1") },
              singleLine = true,
              modifier = Modifier.weight(0.65f),
            )
            OutlinedTextField(
              value = item1Amount,
              onValueChange = { item1Amount = it.filter { ch -> ch.isDigit() || ch == '.' } },
              label = { Text("₹ Amount") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(0.35f),
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = item2Name,
              onValueChange = { item2Name = it },
              label = { Text("Charge 2 (Optional)") },
              singleLine = true,
              modifier = Modifier.weight(0.65f),
            )
            OutlinedTextField(
              value = item2Amount,
              onValueChange = { item2Amount = it.filter { ch -> ch.isDigit() || ch == '.' } },
              label = { Text("₹ Amount") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(0.35f),
            )
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
            val a1 = item1Amount.toDoubleOrNull() ?: 0.0
            val a2 = item2Amount.toDoubleOrNull() ?: 0.0
            if (hospital.isBlank() || (a1 + a2) <= 0.0) {
              errorMsg = "Please enter hospital name and at least one valid charge amount."
              return@Button
            }
            val rawItems =
              buildList {
                if (item1Name.isNotBlank() && a1 > 0) add("${item1Name.trim()}::$a1")
                if (item2Name.isNotBlank() && a2 > 0) add("${item2Name.trim()}::$a2")
              }.joinToString("||")

            onSaveReceipt(
              ReceiptEntity(
                userId = userId,
                receiptNumber = receiptNumber.trim(),
                hospitalOrClinic = hospital.trim(),
                doctorName = doctor.trim(),
                appointmentId = appointments.firstOrNull()?.id,
                receiptDate = receiptDate.trim(),
                paymentMethod = paymentMethod.trim(),
                paymentStatus = "Paid",
                totalAmountInr = a1 + a2,
                itemizedChargesRaw = rawItems,
                notes = "",
                createdAt = System.currentTimeMillis(),
              )
            )
            showAddDialog = false
          }
        ) {
          Text(strings.saveButton)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) { Text(strings.cancelButton) }
      },
    )
  }

  val recDetail = viewingReceipt
  if (recDetail != null) {
    val parsedItems =
      recDetail.itemizedChargesRaw
        .split("||")
        .mapNotNull { token ->
          val parts = token.split("::")
          if (parts.size == 2) parts[0] to (parts[1].toDoubleOrNull() ?: 0.0) else null
        }

    AlertDialog(
      onDismissRequest = { viewingReceipt = null },
      title = {
        Column {
          Text(recDetail.hospitalOrClinic, fontWeight = FontWeight.Bold)
          Text(
            text = "${strings.receiptNumberLabel}: ${recDetail.receiptNumber} · ${recDetail.receiptDate}",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelSmall,
          )
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (recDetail.doctorName.isNotBlank()) {
            Text("${strings.doctorLabel}: ${recDetail.doctorName}", style = MaterialTheme.typography.bodySmall)
          }
          Text(
            "Payment Method: ${recDetail.paymentMethod} (${recDetail.paymentStatus})",
            style = MaterialTheme.typography.bodySmall,
          )
          HorizontalDivider()
          Text(
            strings.itemizedChargesLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
          parsedItems.forEach { (itemLabel, amt) ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(itemLabel, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
              Text(
                String.format("₹%.2f", amt),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
          HorizontalDivider()
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(strings.paymentAmountLabel, fontWeight = FontWeight.Bold)
            Text(
              String.format("₹%.2f", recDetail.totalAmountInr),
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            receiptPendingDownload = recDetail
            receiptDownloadLauncher.launch("Receipt_${recDetail.receiptNumber}.txt")
          }
        ) {
          Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.downloadReceiptButton)
        }
      },
      dismissButton = {
        TextButton(onClick = { viewingReceipt = null }) { Text(strings.closeButton) }
      },
    )
  }
}

private fun buildReceiptPlainText(rec: ReceiptEntity): String {
  val items =
    rec.itemizedChargesRaw.split("||").mapNotNull { token ->
      val parts = token.split("::")
      if (parts.size == 2) "- ${parts[0]}: INR ${parts[1]}" else null
    }
  return buildString {
    appendLine("==========================================")
    appendLine("MEDTRACE OFFICIAL MEDICAL PAYMENT RECEIPT")
    appendLine("==========================================")
    appendLine("Receipt Number : ${rec.receiptNumber}")
    appendLine("Hospital/Clinic: ${rec.hospitalOrClinic}")
    appendLine("Doctor         : ${rec.doctorName.ifBlank { "N/A" }}")
    appendLine("Date           : ${rec.receiptDate}")
    appendLine("Payment Method : ${rec.paymentMethod} (${rec.paymentStatus})")
    appendLine("------------------------------------------")
    appendLine("ITEMIZED CHARGES:")
    items.forEach { appendLine(it) }
    appendLine("------------------------------------------")
    appendLine(String.format("TOTAL PAID     : INR %.2f", rec.totalAmountInr))
    appendLine("==========================================")
  }
}
