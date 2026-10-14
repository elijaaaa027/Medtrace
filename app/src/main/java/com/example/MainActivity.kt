package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ActivityLogEntity
import com.example.data.AppointmentEntity
import com.example.data.GuardianEntity
import com.example.data.HealthRecordEntity
import com.example.data.HealthSnapshotEntity
import com.example.data.InsurancePolicyEntity
import com.example.data.MedTraceDatabase
import com.example.data.MedicineEntity
import com.example.data.ReceiptEntity
import com.example.data.UserProfileEntity
import com.example.i18n.AppLanguage
import com.example.i18n.Translations
import com.example.security.AuthAndFileManager
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.AppointmentsScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GuardianManagementScreen
import com.example.ui.screens.GuardianOverviewScreen
import com.example.ui.screens.HealthRecordsScreen
import com.example.ui.screens.HealthSnapshotScreen
import com.example.ui.screens.InsuranceScreen
import com.example.ui.screens.LanguageSelectorBar
import com.example.ui.screens.LoginOtpScreen
import com.example.ui.screens.MedTraceRoute
import com.example.ui.screens.MedicationManagementScreen
import com.example.ui.screens.NearbyHospitalsMapScreen
import com.example.ui.screens.ProfileSetupOrEditScreen
import com.example.ui.screens.ReceiptsScreen
import com.example.ui.screens.SecureDocumentViewerDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val context = LocalContext.current
      var isDarkMode by remember {
        mutableStateOf(AuthAndFileManager.getSavedDarkModePreference(context, false))
      }

      MyApplicationTheme(darkTheme = isDarkMode) {
        MedTraceRootApp(
          isDarkMode = isDarkMode,
          onDarkModeChange = { enabled ->
            isDarkMode = enabled
            AuthAndFileManager.saveDarkModePreference(context, enabled)
          },
        )
      }
    }
  }
}

private data class NavDestinationItem(
  val route: MedTraceRoute,
  val label: String,
  val icon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedTraceRootApp(
  isDarkMode: Boolean,
  onDarkModeChange: (Boolean) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val db = remember { MedTraceDatabase.getInstance(context) }
  val dao = remember { db.dao() }

  var currentLanguage by remember {
    mutableStateOf(AuthAndFileManager.getSavedLanguagePreference(context))
  }
  val strings = remember(currentLanguage) { Translations.forLanguage(currentLanguage) }

  var authenticatedUserId by remember {
    mutableStateOf(AuthAndFileManager.getSavedAuthenticatedUserId(context))
  }
  var isBiometricEnabled by remember {
    mutableStateOf(AuthAndFileManager.isBiometricEnabled(context))
  }
  var isSessionLocked by remember { mutableStateOf(false) }
  var currentRoute by remember { mutableStateOf(MedTraceRoute.DASHBOARD) }

  // Top-level state for Google Maps Platform Demo Key Quota Banner
  var isMapsQuotaExceeded by remember { mutableStateOf(false) }

  // Record preview dialog launched from Appointment Summary
  var quickPreviewRecord by remember { mutableStateOf<HealthRecordEntity?>(null) }

  val activeUserId = authenticatedUserId

  val profileFlow =
    remember(activeUserId) {
      if (activeUserId != null) dao.observeUserProfile(activeUserId) else flowOf(null)
    }
  val userProfile by profileFlow.collectAsState(initial = null)

  // Sync user's saved language preference when profile loads
  LaunchedEffect(userProfile?.preferredLanguage) {
    val profLang = userProfile?.preferredLanguage
    if (!profLang.isNullOrBlank()) {
      val parsed = AppLanguage.fromCode(profLang)
      if (parsed != currentLanguage) {
        currentLanguage = parsed
        AuthAndFileManager.saveLanguagePreference(context, parsed)
      }
    }
  }

  val guardians by
    remember(activeUserId) {
      if (activeUserId != null) dao.observeGuardians(activeUserId) else flowOf(emptyList())
    }.collectAsState(initial = emptyList())

  val medicines by
    remember(activeUserId) {
      if (activeUserId != null) dao.observeMedicines(activeUserId) else flowOf(emptyList())
    }.collectAsState(initial = emptyList())

  val insurancePolicies by
    remember(activeUserId) {
      if (activeUserId != null) dao.observeInsurancePolicies(activeUserId)
      else flowOf(emptyList())
    }.collectAsState(initial = emptyList())

  val healthSnapshot by
    remember(activeUserId) {
      if (activeUserId != null) dao.observeHealthSnapshot(activeUserId) else flowOf(null)
    }.collectAsState(initial = null)

  val healthRecords by
    remember(activeUserId) {
      if (activeUserId != null) dao.observeHealthRecords(activeUserId) else flowOf(emptyList())
    }.collectAsState(initial = emptyList())

  val receipts by
    remember(activeUserId) {
      if (activeUserId != null) dao.observeReceipts(activeUserId) else flowOf(emptyList())
    }.collectAsState(initial = emptyList())

  val appointments by
    remember(activeUserId) {
      if (activeUserId != null) dao.observeAppointments(activeUserId) else flowOf(emptyList())
    }.collectAsState(initial = emptyList())

  val activityLogs by
    remember(activeUserId) {
      if (activeUserId != null) dao.observeActivityLogs(activeUserId) else flowOf(emptyList())
    }.collectAsState(initial = emptyList())

  fun updateLanguage(newLang: AppLanguage) {
    currentLanguage = newLang
    AuthAndFileManager.saveLanguagePreference(context, newLang)
    val prof = userProfile
    if (prof != null) {
      scope.launch {
        dao.upsertUserProfile(prof.copy(preferredLanguage = newLang.code))
      }
    }
  }

  fun logActivity(uid: String, title: String, category: String) {
    scope.launch {
      dao.insertActivityLog(
        ActivityLogEntity(
          userId = uid,
          title = title,
          category = category,
          timestamp = System.currentTimeMillis(),
        )
      )
    }
  }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // Mandatory Google Maps Platform Quota Defense Banner (Section 8 Case A)
    if (isMapsQuotaExceeded) {
      Surface(
        color = Color(0xFFFFFBEB),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Google Maps Platform quota reached. If you are the app owner, visit ",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF78350F),
          )
          Text(
            text = "maps developer site",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.Underline,
            color = Color(0xFF451A03),
            modifier =
              Modifier.clickable {
                context.startActivity(
                  Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                      "https://developers.google.com/maps/ai/ai-studio?utm_campaign=gmp_mcp_codeassist_v1_aistudio#quota_exceeded_errors"
                    ),
                  )
                )
              },
          )
          Text(
            text = " for instructions to update your account.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF78350F),
          )
        }
      }
    }

    // 1. Unauthenticated -> Real Phone + OTP Login Flow
    if (activeUserId == null) {
      LoginOtpScreen(
        strings = strings,
        currentLanguage = currentLanguage,
        onLanguageChange = ::updateLanguage,
        onAuthenticated = { verifiedPhone, rememberSession ->
          scope.launch {
            val existing = dao.getUserProfile(verifiedPhone)
            val now = System.currentTimeMillis()
            if (existing == null) {
              dao.upsertUserProfile(
                UserProfileEntity(
                  userId = verifiedPhone,
                  fullName = "",
                  dateOfBirth = "",
                  phoneNumber = verifiedPhone,
                  isPhoneVerified = true,
                  email = "",
                  address = "",
                  emergencyContactName = "",
                  emergencyContactPhone = "",
                  emergencyContactRelation = "Parent",
                  preferredLanguage = currentLanguage.code,
                  profilePictureUri = "",
                  idDocType = "",
                  idDocMaskedNumber = "",
                  idDocVerificationStatus = strings.statusNotSubmitted,
                  idDocFileName = "",
                  isProfileCompleted = false,
                  createdAt = now,
                  lastLoginAt = now,
                )
              )
            } else {
              dao.upsertUserProfile(existing.copy(lastLoginAt = now))
            }
            AuthAndFileManager.saveAuthenticatedSession(context, verifiedPhone, rememberSession)
            AuthAndFileManager.setBiometricEnabled(context, isBiometricEnabled, verifiedPhone)
            authenticatedUserId = verifiedPhone
            isSessionLocked = false
            currentRoute = MedTraceRoute.DASHBOARD
            logActivity(verifiedPhone, "Authenticated session (+91 $verifiedPhone)", "Auth")
          }
        },
      )
      return@Column
    }

    val currentProfile =
      userProfile
        ?: UserProfileEntity(
          userId = activeUserId,
          fullName = "",
          dateOfBirth = "",
          phoneNumber = activeUserId,
          isPhoneVerified = true,
          email = "",
          address = "",
          emergencyContactName = "",
          emergencyContactPhone = "",
          emergencyContactRelation = "Parent",
          preferredLanguage = currentLanguage.code,
          profilePictureUri = "",
          idDocType = "",
          idDocMaskedNumber = "",
          idDocVerificationStatus = strings.statusNotSubmitted,
          idDocFileName = "",
          isProfileCompleted = false,
          createdAt = System.currentTimeMillis(),
          lastLoginAt = System.currentTimeMillis(),
        )

    // 1b. Biometric Lock Screen when session is locked
    if (isSessionLocked) {
      BiometricLockScreen(
        strings = strings,
        currentLanguage = currentLanguage,
        userPhone = activeUserId,
        userName = currentProfile.fullName,
        onLanguageChange = ::updateLanguage,
        onUnlocked = { method ->
          isSessionLocked = false
          logActivity(activeUserId, "Unlocked MedTrace session via $method", "Security")
        },
        onFallbackToPhoneOtp = {
          isSessionLocked = false
          AuthAndFileManager.clearAuthenticatedSession(context)
          authenticatedUserId = null
        },
      )
      return@Column
    }

    // 2. New User Profile Setup Flow
    if (!currentProfile.isProfileCompleted) {
      ProfileSetupOrEditScreen(
        strings = strings,
        currentLanguage = currentLanguage,
        existingProfile = currentProfile,
        isInitialSetup = true,
        onLanguageChange = ::updateLanguage,
        onSaveProfile = { savedProfile, optionalGuardian ->
          scope.launch {
            dao.upsertUserProfile(savedProfile)
            if (optionalGuardian != null) {
              dao.upsertGuardian(optionalGuardian)
            }
            logActivity(activeUserId, "Completed patient profile setup (${savedProfile.fullName})", "Profile")
          }
        },
      )
      return@Column
    }

    // 3. Authenticated Main Healthcare Platform with Responsive Navigation
    val navDestinations =
      listOf(
        NavDestinationItem(MedTraceRoute.DASHBOARD, strings.navDashboard, Icons.Default.Dashboard),
        NavDestinationItem(MedTraceRoute.AI_CHAT, strings.navAiChat, Icons.Default.AutoAwesome),
        NavDestinationItem(MedTraceRoute.HEALTH_RECORDS, strings.navHealthRecords, Icons.Default.Description),
        NavDestinationItem(MedTraceRoute.APPOINTMENTS, strings.navAppointments, Icons.Default.CalendarMonth),
        NavDestinationItem(MedTraceRoute.MEDICINES, strings.navMedicines, Icons.Default.Medication),
        NavDestinationItem(MedTraceRoute.INSURANCE, strings.navInsurance, Icons.Default.HealthAndSafety),
        NavDestinationItem(MedTraceRoute.GUARDIAN, strings.navGuardian, Icons.Default.People),
        NavDestinationItem(MedTraceRoute.GUARDIAN_OVERVIEW, strings.navGuardianOverview, Icons.Default.Shield),
        NavDestinationItem(MedTraceRoute.HEALTH_SNAPSHOT, strings.navHealthSnapshot, Icons.Default.MonitorHeart),
        NavDestinationItem(MedTraceRoute.NEARBY_HOSPITALS, strings.navNearbyHospitals, Icons.Default.LocationOn),
        NavDestinationItem(MedTraceRoute.RECEIPTS, strings.navReceipts, Icons.Default.ReceiptLong),
        NavDestinationItem(MedTraceRoute.PROFILE, strings.navProfile, Icons.Default.Person),
        NavDestinationItem(MedTraceRoute.SETTINGS, strings.navSettings, Icons.Default.Settings),
      )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val isWideScreen = maxWidth >= 720.dp

      @Composable
      fun RenderActiveScreen() {
        when (currentRoute) {
          MedTraceRoute.DASHBOARD ->
            DashboardScreen(
              strings = strings,
              currentLanguage = currentLanguage,
              profile = currentProfile,
              snapshot = healthSnapshot,
              appointments = appointments,
              medicines = medicines,
              records = healthRecords,
              insurancePolicies = insurancePolicies,
              guardians = guardians,
              receipts = receipts,
              activityLogs = activityLogs,
              onNavigate = { currentRoute = it },
            )
          MedTraceRoute.HEALTH_SNAPSHOT ->
            HealthSnapshotScreen(
              strings = strings,
              userId = activeUserId,
              snapshot = healthSnapshot,
              activeMedicines = medicines.filter { it.status == "Active" },
              onSaveSnapshot = { snap ->
                scope.launch {
                  dao.upsertHealthSnapshot(snap)
                  logActivity(activeUserId, "Updated Health Snapshot & Vitals", "Health Snapshot")
                }
              },
            )
          MedTraceRoute.HEALTH_RECORDS ->
            HealthRecordsScreen(
              strings = strings,
              userId = activeUserId,
              uploaderName = currentProfile.fullName,
              records = healthRecords,
              appointments = appointments,
              onAddRecord = { rec ->
                scope.launch {
                  dao.insertHealthRecord(rec)
                  logActivity(activeUserId, "Uploaded health record: ${rec.fileName} (${rec.recordType})", "Records")
                }
              },
              onDeleteRecord = { rec ->
                scope.launch {
                  dao.deleteHealthRecord(rec)
                  logActivity(activeUserId, "Deleted health record: ${rec.fileName}", "Records")
                }
              },
            )
          MedTraceRoute.APPOINTMENTS ->
            AppointmentsScreen(
              strings = strings,
              userId = activeUserId,
              patientName = currentProfile.fullName,
              appointments = appointments,
              records = healthRecords,
              onSaveAppointment = { appt ->
                scope.launch {
                  dao.upsertAppointment(appt)
                  val action =
                    if (appt.status == "Cancelled") "Cancelled appointment with ${appt.doctorName}"
                    else "Saved appointment with ${appt.doctorName} (${appt.appointmentDate})"
                  logActivity(activeUserId, action, "Appointments")
                }
              },
              onOpenRecordViewer = { rec -> quickPreviewRecord = rec },
            )
          MedTraceRoute.MEDICINES ->
            MedicationManagementScreen(
              strings = strings,
              userId = activeUserId,
              medicines = medicines,
              onSaveMedicine = { med ->
                scope.launch {
                  dao.upsertMedicine(med)
                  logActivity(activeUserId, "Updated medication: ${med.medicineName} (${med.status})", "Medicines")
                }
              },
              onDeleteMedicine = { med ->
                scope.launch {
                  dao.deleteMedicine(med)
                  logActivity(activeUserId, "Removed medication: ${med.medicineName}", "Medicines")
                }
              },
            )
          MedTraceRoute.INSURANCE ->
            InsuranceScreen(
              strings = strings,
              userId = activeUserId,
              policies = insurancePolicies,
              onSavePolicy = { pol ->
                scope.launch {
                  dao.upsertInsurancePolicy(pol)
                  logActivity(activeUserId, "Saved insurance policy: ${pol.providerName}", "Insurance")
                }
              },
              onDeletePolicy = { pol ->
                scope.launch {
                  dao.deleteInsurancePolicy(pol)
                  logActivity(activeUserId, "Removed insurance policy: ${pol.providerName}", "Insurance")
                }
              },
            )
          MedTraceRoute.GUARDIAN ->
            GuardianManagementScreen(
              strings = strings,
              userId = activeUserId,
              guardians = guardians,
              onSaveGuardian = { g ->
                scope.launch {
                  if (g.isPrimaryEmergencyContact) {
                    dao.clearPrimaryEmergencyGuardians(activeUserId)
                  }
                  dao.upsertGuardian(g)
                  logActivity(activeUserId, "Saved guardian: ${g.fullName} (${g.relationship})", "Guardian")
                }
              },
              onDeleteGuardian = { g ->
                scope.launch {
                  dao.deleteGuardian(g)
                  logActivity(activeUserId, "Removed guardian: ${g.fullName}", "Guardian")
                }
              },
              onNavigateToOverview = { currentRoute = MedTraceRoute.GUARDIAN_OVERVIEW },
            )
          MedTraceRoute.GUARDIAN_OVERVIEW ->
            GuardianOverviewScreen(
              strings = strings,
              profile = currentProfile,
              guardians = guardians,
              appointments = appointments,
              medicines = medicines,
              snapshot = healthSnapshot,
              insurancePolicies = insurancePolicies,
              records = healthRecords,
              activityLogs = activityLogs,
              onNavigateToGuardianManagement = { currentRoute = MedTraceRoute.GUARDIAN },
            )
          MedTraceRoute.NEARBY_HOSPITALS ->
            NearbyHospitalsMapScreen(
              strings = strings,
              currentLanguage = currentLanguage,
              onQuotaExceeded = { isMapsQuotaExceeded = true },
            )
          MedTraceRoute.AI_CHAT ->
            AiChatScreen(
              strings = strings,
              currentLanguage = currentLanguage,
              patientName = currentProfile.fullName.ifBlank { "Patient" },
              activeMedicationsSummary = medicines.filter { it.status == "Active" }.joinToString { it.medicineName },
              onNavigateToHospitals = { currentRoute = MedTraceRoute.NEARBY_HOSPITALS },
            )
          MedTraceRoute.RECEIPTS ->
            ReceiptsScreen(
              strings = strings,
              userId = activeUserId,
              receipts = receipts,
              appointments = appointments,
              onSaveReceipt = { rec ->
                scope.launch {
                  dao.upsertReceipt(rec)
                  logActivity(activeUserId, "Added medical receipt: ${rec.receiptNumber}", "Receipts")
                }
              },
              onDeleteReceipt = { rec ->
                scope.launch {
                  dao.deleteReceipt(rec)
                  logActivity(activeUserId, "Deleted medical receipt: ${rec.receiptNumber}", "Receipts")
                }
              },
            )
          MedTraceRoute.PROFILE ->
            ProfileSetupOrEditScreen(
              strings = strings,
              currentLanguage = currentLanguage,
              existingProfile = currentProfile,
              isInitialSetup = false,
              onLanguageChange = ::updateLanguage,
              onSaveProfile = { updated, _ ->
                scope.launch {
                  dao.upsertUserProfile(updated)
                  logActivity(activeUserId, "Updated patient profile details", "Profile")
                }
              },
            )
          MedTraceRoute.SETTINGS ->
            SettingsScreen(
              strings = strings,
              currentLanguage = currentLanguage,
              isDarkMode = isDarkMode,
              isBiometricEnabled = isBiometricEnabled,
              profile = currentProfile,
              onLanguageChange = ::updateLanguage,
              onDarkModeToggle = onDarkModeChange,
              onBiometricToggle = { enabled ->
                isBiometricEnabled = enabled
                AuthAndFileManager.setBiometricEnabled(context, enabled, activeUserId)
                logActivity(
                  activeUserId,
                  if (enabled) "Enabled Biometric Authentication (Fingerprint / Face Unlock)"
                  else "Disabled Biometric Authentication",
                  "Security",
                )
              },
              onLockAppNow = { isSessionLocked = true },
              onNavigateToProfile = { currentRoute = MedTraceRoute.PROFILE },
              onLoadDemoSampleData = {
                scope.launch {
                  seedDemoClinicalDataForUser(context, dao, activeUserId, currentProfile.fullName)
                  logActivity(activeUserId, "Loaded demo sample clinical data [DEMO/TEST DATA]", "Demo")
                  currentRoute = MedTraceRoute.DASHBOARD
                }
              },
              onLogout = {
                AuthAndFileManager.clearAuthenticatedSession(context)
                authenticatedUserId = null
              },
            )
        }
      }

      if (isWideScreen) {
        // Desktop / Tablet Permanent Sidebar Layout
        Row(modifier = Modifier.fillMaxSize()) {
          Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.width(260.dp).fillMaxHeight(),
          ) {
            Column(
              modifier =
                Modifier.fillMaxSize()
                  .verticalScroll(rememberScrollState())
                  .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = strings.appName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp),
              )
              LanguageSelectorBar(
                currentLanguage = currentLanguage,
                onLanguageChange = ::updateLanguage,
              )
              HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

              navDestinations.forEach { item ->
                NavigationDrawerItem(
                  label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                  selected = currentRoute == item.route,
                  onClick = { currentRoute = item.route },
                  icon = { Icon(item.icon, contentDescription = item.label) },
                )
              }

              Spacer(modifier = Modifier.height(12.dp))
              HorizontalDivider()
              NavigationDrawerItem(
                label = { Text(strings.logoutButton) },
                selected = false,
                onClick = {
                  AuthAndFileManager.clearAuthenticatedSession(context)
                  authenticatedUserId = null
                },
                icon = { Icon(Icons.Default.Logout, contentDescription = null) },
              )
            }
          }

          Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            TopAppBar(
              title = {
                Text(
                  text = navDestinations.firstOrNull { it.route == currentRoute }?.label ?: strings.appName,
                  fontWeight = FontWeight.Bold,
                )
              },
              actions = {
                if (isBiometricEnabled) {
                  IconButton(onClick = { isSessionLocked = true }) {
                    Icon(
                      imageVector = Icons.Default.Lock,
                      contentDescription = strings.biometricLockAppButton,
                    )
                  }
                }
                IconButton(onClick = { onDarkModeChange(!isDarkMode) }) {
                  Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Dark Mode",
                  )
                }
              },
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
              RenderActiveScreen()
            }
          }
        }
      } else {
        // Mobile Responsive Modal Drawer + Scrollable Route Bar + Bottom Bar
        ModalNavigationDrawer(
          drawerState = drawerState,
          drawerContent = {
            ModalDrawerSheet {
              Column(
                modifier =
                  Modifier.fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = strings.appName,
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = currentProfile.fullName.ifBlank { "Patient" },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LanguageSelectorBar(
                  currentLanguage = currentLanguage,
                  onLanguageChange = ::updateLanguage,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                navDestinations.forEach { item ->
                  NavigationDrawerItem(
                    label = { Text(item.label) },
                    selected = currentRoute == item.route,
                    onClick = {
                      currentRoute = item.route
                      scope.launch { drawerState.close() }
                    },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                  )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                  label = { Text(strings.logoutButton) },
                  selected = false,
                  onClick = {
                    scope.launch { drawerState.close() }
                    AuthAndFileManager.clearAuthenticatedSession(context)
                    authenticatedUserId = null
                  },
                  icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                )
              }
            }
          },
        ) {
          Scaffold(
            topBar = {
              Column {
                TopAppBar(
                  navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                      Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                  },
                  title = {
                    Text(
                      text = strings.appName,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  },
                  actions = {
                    LanguageSelectorBar(
                      currentLanguage = currentLanguage,
                      onLanguageChange = ::updateLanguage,
                    )
                    if (isBiometricEnabled) {
                      IconButton(onClick = { isSessionLocked = true }) {
                        Icon(
                          imageVector = Icons.Default.Lock,
                          contentDescription = strings.biometricLockAppButton,
                        )
                      }
                    }
                    IconButton(onClick = { onDarkModeChange(!isDarkMode) }) {
                      Icon(
                        imageVector =
                          if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Dark Mode",
                      )
                    }
                  },
                  colors =
                    TopAppBarDefaults.topAppBarColors(
                      containerColor = MaterialTheme.colorScheme.surface
                    ),
                )
                // Horizontal quick-access route tabs so all 12 routes are 1-tap reachable on mobile
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .background(MaterialTheme.colorScheme.surface)
                      .horizontalScroll(rememberScrollState())
                      .padding(horizontal = 12.dp, vertical = 6.dp),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  navDestinations.forEach { item ->
                    val selected = currentRoute == item.route
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color =
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                      modifier = Modifier.clickable { currentRoute = item.route },
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                      ) {
                        Icon(
                          imageVector = item.icon,
                          contentDescription = null,
                          tint =
                            if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                          modifier = Modifier.size(15.dp),
                        )
                        Text(
                          text = item.label,
                          style = MaterialTheme.typography.labelMedium,
                          color =
                            if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                          fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                      }
                    }
                  }
                }
                HorizontalDivider()
              }
            },
            bottomBar = {
              NavigationBar {
                listOf(
                  NavDestinationItem(MedTraceRoute.DASHBOARD, strings.navDashboard, Icons.Default.Dashboard),
                  NavDestinationItem(MedTraceRoute.APPOINTMENTS, strings.navAppointments, Icons.Default.CalendarMonth),
                  NavDestinationItem(MedTraceRoute.HEALTH_RECORDS, strings.navHealthRecords, Icons.Default.Description),
                  NavDestinationItem(MedTraceRoute.NEARBY_HOSPITALS, strings.navNearbyHospitals, Icons.Default.LocationOn),
                  NavDestinationItem(MedTraceRoute.SETTINGS, strings.navSettings, Icons.Default.Settings),
                ).forEach { item ->
                  NavigationBarItem(
                    selected = currentRoute == item.route,
                    onClick = { currentRoute = item.route },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                  )
                }
              }
            },
          ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
              RenderActiveScreen()
            }
          }
        }
      }
    }
  }

  val previewRec = quickPreviewRecord
  if (previewRec != null) {
    SecureDocumentViewerDialog(
      strings = strings,
      record = previewRec,
      onDismiss = { quickPreviewRecord = null },
      onDownload = { quickPreviewRecord = null },
    )
  }
}

private suspend fun seedDemoClinicalDataForUser(
  context: android.content.Context,
  dao: com.example.data.MedTraceDao,
  userId: String,
  patientName: String,
) {
  val now = System.currentTimeMillis()
  val name = patientName.ifBlank { "Patient" }

  dao.upsertHealthSnapshot(
    HealthSnapshotEntity(
      userId = userId,
      bloodGroup = "O+",
      allergies = "Penicillin (Mild rash)",
      currentMedicationsSummary = "Metformin 500 mg, Atorvastatin 10 mg",
      recentDiagnosis = "Routine Preventive Metabolic Check — Controlled",
      recentLabResults = "HbA1c: 5.8%, Fasting Glucose: 94 mg/dL, Lipid Profile: Normal",
      vaccinationStatus = "Tetanus Booster & Influenza Updated (2026)",
      medicalConditions = "Mild Seasonal Allergic Rhinitis",
      bloodPressure = "118/76 mmHg",
      heartRate = "72 bpm",
      spo2 = "98%",
      temperature = "98.4 °F",
      weightKg = "68.5 kg",
      bloodGlucose = "94 mg/dL",
      updatedAt = now,
    )
  )

  val apptId =
    dao.upsertAppointment(
      AppointmentEntity(
        userId = userId,
        patientName = name,
        doctorName = "Dr. Aravind Ramanathan (DEMO)",
        hospitalOrClinic = "Apollo Speciality Clinic (DEMO)",
        department = "Internal Medicine & Cardiology",
        appointmentDate = "2026-10-18",
        appointmentTime = "10:30 AM",
        reasonForVisit = "Annual Preventive Cardiac & Metabolic Review",
        status = "Upcoming",
        cancelledAt = "",
        diagnosis = "Preventive evaluation scheduled",
        prescriptions = "Continue Metformin 500 mg post-dinner",
        testsOrdered = "Complete Blood Count (CBC), HbA1c, ECG",
        followUpInstructions = "Bring fasting lab report 15 minutes before appointment.",
        billingInfo = "Consultation Fee: ₹850 (Covered under Star Health Outpatient)",
        createdAt = now,
      )
    )

  dao.upsertMedicine(
    MedicineEntity(
      userId = userId,
      medicineName = "Metformin Hydrochloride (DEMO)",
      dosage = "500 mg",
      frequency = "Twice daily after meals",
      startDate = "2026-09-01",
      endDate = "2026-12-01",
      prescribedBy = "Dr. Aravind Ramanathan",
      instructions = "Take after breakfast and dinner with water",
      reminderSchedule = "08:30 AM, 08:30 PM",
      status = "Active",
      visibleToGuardian = true,
      createdAt = now,
    )
  )

  dao.upsertInsurancePolicy(
    InsurancePolicyEntity(
      userId = userId,
      providerName = "Star Health and Allied Insurance",
      policyNumber = "DEMO-SHAI-2026-99412",
      policyType = "Family Health Optima Plan (DEMO)",
      coverageAmount = "₹10,00,000",
      startDate = "2026-01-01",
      expiryDate = "2026-12-31",
      isActive = true,
      coveredServices = "In-patient Hospitalization, Day Care Treatments, Pre/Post Hospitalization, Annual Health Check",
      claimInfo = "Demo Policy — Cashless TPA Desk Active",
      updatedAt = now,
    )
  )

  dao.upsertGuardian(
    GuardianEntity(
      userId = userId,
      fullName = "Lakshmi Narayanan (DEMO)",
      relationship = "Mother",
      phoneNumber = "9876501234",
      isPhoneVerified = true,
      email = "lakshmi.demo@example.com",
      isPrimaryEmergencyContact = true,
      canViewAppointments = true,
      canViewMedications = true,
      canViewHealthAlerts = true,
      canViewInsurance = true,
      canViewEmergencyInfo = true,
      canViewAuthorizedDocuments = true,
      addedAt = now,
    )
  )

  dao.upsertReceipt(
    ReceiptEntity(
      userId = userId,
      receiptNumber = "DEMO-RCPT-2026-104",
      hospitalOrClinic = "Apollo Speciality Clinic (DEMO)",
      doctorName = "Dr. Aravind Ramanathan",
      appointmentId = apptId,
      receiptDate = Translations.currentLocalDateString(),
      paymentMethod = "UPI",
      paymentStatus = "Paid",
      totalAmountInr = 1450.0,
      itemizedChargesRaw = "Specialist Consultation Fee::850.0||Comprehensive Metabolic Panel::600.0",
      notes = "Demo receipt record",
      createdAt = now,
    )
  )

  val pdfFile =
    AuthAndFileManager.createRealPdfDocumentInPrivateStorage(
      context = context,
      userId = userId,
      fileName = "demo_lab_report_metabolic_panel.pdf",
      title = "Lab Report (DEMO/TEST DATA) — Patient: $name",
      lines =
        listOf(
          "Report Type: Comprehensive Metabolic & Lipid Panel [DEMO DATA]",
          "Ordering Physician: Dr. Aravind Ramanathan",
          "Fasting Blood Glucose: 94 mg/dL (Normal: 70 - 99 mg/dL)",
          "HbA1c: 5.8% (Normal: < 6.0%)",
          "Total Cholesterol: 172 mg/dL",
        ),
    )

  dao.insertHealthRecord(
    HealthRecordEntity(
      userId = userId,
      fileName = pdfFile.name,
      localFilePath = pdfFile.absolutePath,
      mimeType = "application/pdf",
      fileSizeBytes = pdfFile.length(),
      recordType = "Lab report",
      uploadedBy = name,
      uploadDate = now,
      appointmentId = apptId,
      appointmentLabel = "2026-10-18 · Dr. Aravind Ramanathan (DEMO)",
      sharedWithGuardian = true,
      notes = "Demo lab report associated with appointment",
    )
  )
}
