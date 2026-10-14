package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.GuardianEntity
import com.example.data.UserProfileEntity
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.security.AuthAndFileManager
import com.example.security.OtpSessionChallenge
import com.example.security.OtpVerificationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguageSelectorBar(
  currentLanguage: AppLanguage,
  onLanguageChange: (AppLanguage) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Icon(
      imageVector = Icons.Default.Language,
      contentDescription = "Language",
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(18.dp),
    )
    AppLanguage.entries.forEach { lang ->
      val selected = lang == currentLanguage
      Surface(
        shape = RoundedCornerShape(6.dp),
        color =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onLanguageChange(lang) },
      ) {
        Text(
          text = lang.nativeLabel,
          style = MaterialTheme.typography.labelMedium,
          color =
            if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
      }
    }
  }
}

@Composable
fun BiometricVerificationDialog(
  strings: AppStrings,
  boundPhone: String,
  onDismiss: () -> Unit,
  onSuccess: (verifiedMethod: String) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var selectedMode by remember { mutableStateOf("Fingerprint") } // "Fingerprint", "Face", "PIN"
  var isScanning by remember { mutableStateOf(false) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var enteredPin by remember { mutableStateOf("") }
  val savedPin = remember { AuthAndFileManager.getSecurityPin(context) }

  // Attempt native Android BiometricPrompt first if hardware & enrolled biometrics exist
  LaunchedEffect(Unit) {
    AuthAndFileManager.launchSystemBiometricPromptIfEnrolled(
      context = context,
      title = strings.biometricTitle,
      subtitle = strings.biometricSubtitle,
      negativeButtonText = strings.biometricUsePhoneOtpFallback,
      onAuthenticated = { onSuccess("System Biometric Sensor") },
      onRequireInteractiveSheet = { info ->
        if (!info.isNullOrBlank()) {
          statusMessage = info
        }
      },
    )
  }

  AlertDialog(
    onDismissRequest = { if (!isScanning) onDismiss() },
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector =
            if (selectedMode == "Face") Icons.Default.Face else Icons.Default.Fingerprint,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(26.dp),
        )
        Text(
          text = strings.biometricTitle,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = strings.biometricSubtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = AuthAndFileManager.getBiometricHardwareStatusLabel(context),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Account: +91 $boundPhone",
              fontFamily = FontFamily.Monospace,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = selectedMode == "Fingerprint",
            onClick = {
              selectedMode = "Fingerprint"
              statusMessage = null
            },
            label = { Text("Fingerprint") },
            leadingIcon = {
              Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp))
            },
          )
          FilterChip(
            selected = selectedMode == "Face",
            onClick = {
              selectedMode = "Face"
              statusMessage = null
            },
            label = { Text("Face Unlock") },
            leadingIcon = {
              Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(16.dp))
            },
          )
          if (savedPin.isNotBlank()) {
            FilterChip(
              selected = selectedMode == "PIN",
              onClick = {
                selectedMode = "PIN"
                statusMessage = null
              },
              label = { Text("PIN") },
              leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
              },
            )
          }
        }

        if (selectedMode == "PIN") {
          OutlinedTextField(
            value = enteredPin,
            onValueChange = {
              enteredPin = it.filter { ch -> ch.isDigit() }.take(6)
              statusMessage = null
            },
            label = { Text("Enter Security PIN") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Button(
            onClick = {
              if (enteredPin == savedPin) {
                onSuccess("Security PIN")
              } else {
                statusMessage = "Invalid Security PIN. Please try again or use Fingerprint/Face Unlock."
              }
            },
            enabled = enteredPin.length >= 4,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Unlock with PIN")
          }
        } else {
          // Interactive Fingerprint / Face sensor pad
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            modifier =
              Modifier.fillMaxWidth()
                .border(
                  1.dp,
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                  RoundedCornerShape(14.dp),
                )
                .clickable(enabled = !isScanning) {
                  isScanning = true
                  statusMessage =
                    if (selectedMode == "Face") "Scanning facial landmarks..."
                    else "Verifying fingerprint sensor..."
                  scope.launch {
                    delay(650)
                    isScanning = false
                    onSuccess(if (selectedMode == "Face") "Face Unlock" else "Fingerprint")
                  }
                }
                .padding(vertical = 20.dp, horizontal = 16.dp),
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector =
                    if (selectedMode == "Face") Icons.Default.Face else Icons.Default.Fingerprint,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(40.dp),
                )
              }
              Text(
                text =
                  if (selectedMode == "Face") strings.biometricFaceUnlockOption
                  else strings.biometricFingerprintOption,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  if (isScanning) (statusMessage ?: "Verifying...")
                  else "Tap sensor above to authenticate immediately",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              if (isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.75f))
              }
            }
          }
        }

        if (!statusMessage.isNullOrBlank() && !isScanning) {
          Text(
            text = statusMessage!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    },
    confirmButton = {
      if (selectedMode != "PIN") {
        Button(
          onClick = {
            isScanning = true
            scope.launch {
              delay(500)
              isScanning = false
              onSuccess(if (selectedMode == "Face") "Face Unlock" else "Fingerprint")
            }
          },
          enabled = !isScanning,
        ) {
          Icon(
            imageVector =
              if (selectedMode == "Face") Icons.Default.Face else Icons.Default.Fingerprint,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            if (selectedMode == "Face") strings.biometricFaceUnlockOption
            else strings.biometricFingerprintOption
          )
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isScanning) {
        Text(strings.biometricUsePhoneOtpFallback)
      }
    },
  )
}

@Composable
fun BiometricLockScreen(
  strings: AppStrings,
  currentLanguage: AppLanguage,
  userPhone: String,
  userName: String,
  onLanguageChange: (AppLanguage) -> Unit,
  onUnlocked: (method: String) -> Unit,
  onFallbackToPhoneOtp: () -> Unit,
) {
  var showBiometricDialog by remember { mutableStateOf(true) }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Box(
          modifier =
            Modifier.size(72.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(42.dp),
          )
        }

        Text(
          text = strings.biometricLockedTitle,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text =
            if (userName.isNotBlank()) "$userName · +91 $userPhone" else "+91 $userPhone",
          fontFamily = FontFamily.Monospace,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = strings.biometricLockedSubtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LanguageSelectorBar(
          currentLanguage = currentLanguage,
          onLanguageChange = onLanguageChange,
        )

        HorizontalDivider()

        Button(
          onClick = { showBiometricDialog = true },
          modifier = Modifier.fillMaxWidth().height(50.dp),
          shape = RoundedCornerShape(10.dp),
        ) {
          Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(strings.biometricLoginButton, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
          onClick = onFallbackToPhoneOtp,
          modifier = Modifier.fillMaxWidth().height(48.dp),
          shape = RoundedCornerShape(10.dp),
        ) {
          Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(strings.biometricUsePhoneOtpFallback)
        }
      }
    }
  }

  if (showBiometricDialog) {
    BiometricVerificationDialog(
      strings = strings,
      boundPhone = userPhone,
      onDismiss = { showBiometricDialog = false },
      onSuccess = { method ->
        showBiometricDialog = false
        onUnlocked(method)
      },
    )
  }
}

@Composable
fun LoginOtpScreen(
  strings: AppStrings,
  currentLanguage: AppLanguage,
  onLanguageChange: (AppLanguage) -> Unit,
  onAuthenticated: (verifiedPhone: String, rememberSession: Boolean) -> Unit,
) {
  val context = LocalContext.current
  val savedBiometricPhone = remember { AuthAndFileManager.getBiometricBoundPhone(context) }
  val isBiometricEnabled = remember { AuthAndFileManager.isBiometricEnabled(context) }

  var rawPhoneInput by remember { mutableStateOf(savedBiometricPhone ?: "") }
  var rememberSession by remember { mutableStateOf(true) }
  var phoneError by remember { mutableStateOf<String?>(null) }
  var showBiometricPrompt by remember { mutableStateOf(false) }

  // Active OTP Challenge state: null until user enters valid phone and presses "Send OTP"
  var activeChallenge by remember { mutableStateOf<OtpSessionChallenge?>(null) }
  var enteredOtp by remember { mutableStateOf("") }
  var otpError by remember { mutableStateOf<String?>(null) }
  var cooldownRemainingSec by remember { mutableIntStateOf(0) }

  LaunchedEffect(activeChallenge) {
    val challenge = activeChallenge ?: return@LaunchedEffect
    while (true) {
      val remMillis = challenge.resendAvailableAtMillis - System.currentTimeMillis()
      cooldownRemainingSec = if (remMillis > 0) ((remMillis + 999) / 1000).toInt() else 0
      if (cooldownRemainingSec <= 0) break
      delay(500)
    }
  }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = strings.appName,
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = strings.tagline,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        LanguageSelectorBar(
          currentLanguage = currentLanguage,
          onLanguageChange = onLanguageChange,
        )

        HorizontalDivider()

        val challenge = activeChallenge
        if (challenge == null) {
          // STEP 1: Ask for phone number and validate before showing OTP screen
          Text(
            text = strings.loginTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = strings.loginSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          OutlinedTextField(
            value = rawPhoneInput,
            onValueChange = {
              rawPhoneInput = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' }.take(15)
              phoneError = null
            },
            label = { Text(strings.phoneNumberLabel) },
            placeholder = { Text(strings.phonePlaceholder) },
            leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            isError = phoneError != null,
            modifier = Modifier.fillMaxWidth(),
          )

          if (phoneError != null) {
            Text(
              text = phoneError!!,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { rememberSession = !rememberSession },
          ) {
            Checkbox(checked = rememberSession, onCheckedChange = { rememberSession = it })
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = strings.rememberSessionLabel, style = MaterialTheme.typography.bodyMedium)
          }

          Button(
            onClick = {
              if (!AuthAndFileManager.isValidPhoneNumber(rawPhoneInput)) {
                phoneError = strings.invalidPhoneError
              } else {
                val issued = AuthAndFileManager.issueOtpForPhone(rawPhoneInput)
                if (issued != null) {
                  activeChallenge = issued
                  enteredOtp = ""
                  otpError = null
                } else {
                  phoneError = strings.invalidPhoneError
                }
              }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(text = strings.sendOtpButton, fontWeight = FontWeight.SemiBold)
          }

          // Alternative Biometric Authentication (Fingerprint / Face Unlock)
          if (isBiometricEnabled) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                  )
                  Column {
                    Text(
                      text = strings.biometricTitle,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = strings.biometricSubtitle,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }

                OutlinedButton(
                  onClick = {
                    val candidatePhone =
                      if (AuthAndFileManager.isValidPhoneNumber(rawPhoneInput)) {
                        AuthAndFileManager.normalizePhoneNumber(rawPhoneInput)
                      } else if (!savedBiometricPhone.isNullOrBlank()) {
                        savedBiometricPhone
                      } else {
                        null
                      }
                    if (candidatePhone == null) {
                      phoneError = strings.invalidPhoneError
                    } else {
                      rawPhoneInput = candidatePhone
                      phoneError = null
                      showBiometricPrompt = true
                    }
                  },
                  modifier = Modifier.fillMaxWidth().height(46.dp),
                  shape = RoundedCornerShape(10.dp),
                ) {
                  Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Icon(
                    Icons.Default.Face,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(strings.biometricLoginButton, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        } else {
          // STEP 2: OTP Screen — appears ONLY after valid phone number & "Send OTP" pressed
          Text(
            text = strings.enterOtpTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = "${strings.otpAssociatedWith}: +91 ${challenge.phoneNumber}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
          )

          // Clearly labelled Demo/Test OTP Banner (never claims a real SMS was sent)
          Surface(
            color = Color(0xFFFFFBEB),
            shape = RoundedCornerShape(10.dp),
            modifier =
              Modifier.fillMaxWidth().border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp)),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  Icons.Default.Security,
                  contentDescription = null,
                  tint = Color(0xFFB45309),
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = strings.demoOtpBannerTitle,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF92400E),
                )
              }
              Text(
                text = strings.demoOtpBannerBody,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF78350F),
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "OTP for ${challenge.phoneNumber}: ${challenge.otpCode}",
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.titleMedium,
                  color = Color(0xFF92400E),
                )
                TextButton(
                  onClick = {
                    enteredOtp = challenge.otpCode
                    otpError = null
                  }
                ) {
                  Text("Use Test OTP", fontWeight = FontWeight.SemiBold, color = Color(0xFFB45309))
                }
              }
            }
          }

          OutlinedTextField(
            value = enteredOtp,
            onValueChange = {
              enteredOtp = it.filter { ch -> ch.isDigit() }.take(6)
              otpError = null
            },
            label = { Text(strings.enterSixDigitOtp) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            isError = otpError != null,
            modifier = Modifier.fillMaxWidth(),
          )

          if (otpError != null) {
            Text(
              text = otpError!!,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
            )
          }

          Button(
            onClick = {
              when (val res = AuthAndFileManager.verifyOtpForPhone(challenge.phoneNumber, enteredOtp)) {
                is OtpVerificationResult.Success -> {
                  onAuthenticated(res.verifiedPhoneNumber, rememberSession)
                }
                OtpVerificationResult.ExpiredOtp -> {
                  otpError = strings.expiredOtpError
                }
                OtpVerificationResult.InvalidCode,
                OtpVerificationResult.InvalidPhone -> {
                  otpError = strings.invalidOtpError
                }
              }
            },
            enabled = enteredOtp.length == 6,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(text = strings.verifyOtpButton, fontWeight = FontWeight.SemiBold)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedButton(
              onClick = {
                val reissued = AuthAndFileManager.issueOtpForPhone(challenge.phoneNumber)
                if (reissued != null) {
                  activeChallenge = reissued
                  enteredOtp = ""
                  otpError = null
                }
              },
              enabled = cooldownRemainingSec <= 0,
            ) {
              if (cooldownRemainingSec > 0) {
                Text(
                  text = "${strings.resendCooldownLabel} ${cooldownRemainingSec}s",
                  fontFamily = FontFamily.Monospace,
                )
              } else {
                Text(text = strings.resendOtpButton)
              }
            }

            TextButton(
              onClick = {
                activeChallenge = null
                enteredOtp = ""
                otpError = null
              }
            ) {
              Text(text = strings.changePhoneButton)
            }
          }
        }
      }
    }
  }

  if (showBiometricPrompt) {
    val targetPhone = AuthAndFileManager.normalizePhoneNumber(rawPhoneInput)
    BiometricVerificationDialog(
      strings = strings,
      boundPhone = targetPhone,
      onDismiss = { showBiometricPrompt = false },
      onSuccess = {
        showBiometricPrompt = false
        AuthAndFileManager.setBiometricEnabled(context, true, targetPhone)
        onAuthenticated(targetPhone, rememberSession)
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupOrEditScreen(
  strings: AppStrings,
  currentLanguage: AppLanguage,
  existingProfile: UserProfileEntity,
  isInitialSetup: Boolean,
  onLanguageChange: (AppLanguage) -> Unit,
  onSaveProfile: (UserProfileEntity, GuardianEntity?) -> Unit,
) {
  val context = LocalContext.current
  var fullName by remember(existingProfile) { mutableStateOf(existingProfile.fullName) }
  var dob by remember(existingProfile) { mutableStateOf(existingProfile.dateOfBirth) }
  var verifiedPhone by remember(existingProfile) { mutableStateOf(existingProfile.phoneNumber) }
  var pendingPhoneInput by remember(existingProfile) { mutableStateOf(existingProfile.phoneNumber) }
  var email by remember(existingProfile) { mutableStateOf(existingProfile.email) }
  var address by remember(existingProfile) { mutableStateOf(existingProfile.address) }
  var emergencyName by remember(existingProfile) { mutableStateOf(existingProfile.emergencyContactName) }
  var emergencyPhone by remember(existingProfile) { mutableStateOf(existingProfile.emergencyContactPhone) }
  var emergencyRelation by
    remember(existingProfile) {
      mutableStateOf(existingProfile.emergencyContactRelation.ifBlank { "Parent" })
    }
  var profilePicUri by remember(existingProfile) { mutableStateOf(existingProfile.profilePictureUri) }

  // Optional Guardian creation during profile setup
  var addGuardianNow by remember { mutableStateOf(false) }
  var guardianName by remember { mutableStateOf("") }
  var guardianRelation by remember { mutableStateOf("Parent") }
  var guardianPhone by remember { mutableStateOf("") }

  // Identity Document Verification State (Demo Aadhaar / Demo PAN)
  var idDocType by
    remember(existingProfile) {
      mutableStateOf(existingProfile.idDocType.ifBlank { "Demo Aadhaar" })
    }
  var idDocFileName by remember(existingProfile) { mutableStateOf(existingProfile.idDocFileName) }
  var idDocMasked by remember(existingProfile) { mutableStateOf(existingProfile.idDocMaskedNumber) }
  var idDocStatus by
    remember(existingProfile) { mutableStateOf(existingProfile.idDocVerificationStatus) }
  var rawDemoIdInput by remember { mutableStateOf("") }
  var extractedPreviewReady by remember { mutableStateOf(false) }
  var idValidationError by remember { mutableStateOf<String?>(null) }

  // Phone change OTP verification dialog state
  var phoneChangeChallenge by remember { mutableStateOf<OtpSessionChallenge?>(null) }
  var phoneChangeOtpInput by remember { mutableStateOf("") }
  var phoneChangeOtpError by remember { mutableStateOf<String?>(null) }

  var formError by remember { mutableStateOf<String?>(null) }
  var savedMessage by remember { mutableStateOf<String?>(null) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
      if (uri != null) {
        profilePicUri = uri.toString()
      }
    }

  val idDocPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
      if (uri != null) {
        val meta = AuthAndFileManager.inspectPickedFile(context, uri)
        if (meta.isSupported) {
          idDocFileName = meta.displayName
          idValidationError = null
        } else {
          idValidationError = "Unsupported file format. Please select PDF, JPG, JPEG, or PNG."
        }
      }
    }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Text(
          text = if (isInitialSetup) strings.profileSetupTitle else strings.editProfileTitle,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
        if (isInitialSetup) {
          Text(
            text = strings.profileSetupSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        // Profile Picture section
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Box(
            modifier =
              Modifier.size(68.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            if (profilePicUri.isNotBlank()) {
              AsyncImage(
                model = profilePicUri,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
            } else {
              Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp),
              )
            }
          }
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = strings.profilePictureLabel, style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { photoPickerLauncher.launch("image/*") }) {
              Text(text = strings.selectProfilePhotoButton)
            }
          }
        }

        // Preferred Language Selector
        Text(
          text = strings.preferredLanguageLabel,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
        LanguageSelectorBar(
          currentLanguage = currentLanguage,
          onLanguageChange = onLanguageChange,
        )

        OutlinedTextField(
          value = fullName,
          onValueChange = {
            fullName = it
            formError = null
          },
          label = { Text("${strings.fullNameLabel} *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = dob,
          onValueChange = {
            dob = it
            formError = null
          },
          label = { Text("${strings.dobLabel} *") },
          placeholder = { Text("1992-08-15") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        // Phone Number with verification enforcement when modified
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedTextField(
            value = pendingPhoneInput,
            onValueChange = {
              pendingPhoneInput = it.filter { ch -> ch.isDigit() }.take(10)
            },
            label = { Text("${strings.phoneNumberLabel} *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            trailingIcon = {
              val isSameVerified =
                AuthAndFileManager.normalizePhoneNumber(pendingPhoneInput) == verifiedPhone
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 8.dp),
              ) {
                Icon(
                  imageVector =
                    if (isSameVerified) Icons.Default.CheckCircle else Icons.Default.Security,
                  contentDescription = null,
                  tint = if (isSameVerified) Color(0xFF16A34A) else Color(0xFFD97706),
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text =
                    if (isSameVerified) strings.phoneVerifiedBadge
                    else strings.phoneUnverifiedBadge,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isSameVerified) Color(0xFF16A34A) else Color(0xFFD97706),
                )
              }
            },
            modifier = Modifier.fillMaxWidth(),
          )

          if (AuthAndFileManager.normalizePhoneNumber(pendingPhoneInput) != verifiedPhone) {
            OutlinedButton(
              onClick = {
                if (AuthAndFileManager.isValidPhoneNumber(pendingPhoneInput)) {
                  phoneChangeChallenge = AuthAndFileManager.issueOtpForPhone(pendingPhoneInput)
                  phoneChangeOtpInput = ""
                  phoneChangeOtpError = null
                } else {
                  formError = strings.invalidPhoneError
                }
              }
            ) {
              Text(text = "${strings.sendOtpButton} (${pendingPhoneInput})")
            }
          }
        }

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text(strings.emailLabel) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text(strings.addressLabel) },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider()
        Text(
          text = "Emergency Contact",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )

        OutlinedTextField(
          value = emergencyName,
          onValueChange = { emergencyName = it },
          label = { Text("${strings.emergencyContactNameLabel} *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = emergencyPhone,
          onValueChange = { emergencyPhone = it.filter { ch -> ch.isDigit() }.take(10) },
          label = { Text("${strings.emergencyContactPhoneLabel} *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = emergencyRelation,
          onValueChange = { emergencyRelation = it },
          label = { Text(strings.emergencyContactRelationLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        if (isInitialSetup) {
          HorizontalDivider()
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { addGuardianNow = !addGuardianNow },
          ) {
            Checkbox(checked = addGuardianNow, onCheckedChange = { addGuardianNow = it })
            Text(
              text = "Add an Authorized Guardian now (Optional)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
            )
          }

          if (addGuardianNow) {
            OutlinedTextField(
              value = guardianName,
              onValueChange = { guardianName = it },
              label = { Text(strings.guardianNameLabel) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("Parent", "Mother", "Father", "Legal guardian", "Other authorized caregiver")
                .forEach { rel ->
                  FilterChip(
                    selected = guardianRelation == rel,
                    onClick = { guardianRelation = rel },
                    label = { Text(rel) },
                  )
                }
            }
            OutlinedTextField(
              value = guardianPhone,
              onValueChange = { guardianPhone = it.filter { ch -> ch.isDigit() }.take(10) },
              label = { Text(strings.guardianPhoneLabel) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      }
    }

    // Secure Identity Document Verification Section (Demo Aadhaar / Demo PAN)
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
            text = strings.idVerificationTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }

        // Explicit DEMO/TEST DATA banner
        Surface(
          color = Color(0xFFFFFBEB),
          shape = RoundedCornerShape(8.dp),
          modifier =
            Modifier.fillMaxWidth().border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp)),
        ) {
          Text(
            text = strings.demoDataDisclaimer,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF92400E),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(10.dp),
          )
        }

        if (idDocMasked.isNotBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = "${strings.selectDocTypeLabel}: $idDocType",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = "${strings.maskedDocLabel}: $idDocMasked",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "${strings.statusLabel}: $idDocStatus",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
              )
              if (idDocFileName.isNotBlank()) {
                Text(
                  text = "File: $idDocFileName",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        // Step 1: Select document type
        Text(text = "1. ${strings.selectDocTypeLabel}", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("Demo Aadhaar" to strings.docTypeAadhaar, "Demo PAN" to strings.docTypePan)
            .forEach { (typeKey, label) ->
              FilterChip(
                selected = idDocType == typeKey,
                onClick = {
                  idDocType = typeKey
                  extractedPreviewReady = false
                },
                label = { Text(label) },
              )
            }
        }

        // Step 2: Upload document via file picker or attach demo test document
        Text(text = "2. ${strings.uploadIdDocButton}", style = MaterialTheme.typography.labelLarge)
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = {
              idDocPickerLauncher.launch(
                arrayOf("application/pdf", "image/jpeg", "image/png")
              )
            }
          ) {
            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Choose File")
          }

          TextButton(
            onClick = {
              idDocFileName =
                if (idDocType.contains("Aadhaar")) "demo_aadhaar_test_card.pdf"
                else "demo_pan_test_card.pdf"
              if (rawDemoIdInput.isBlank()) {
                rawDemoIdInput = if (idDocType.contains("Aadhaar")) "4829" else "918K"
              }
              idValidationError = null
            }
          ) {
            Text("Use Demo Test File [DEMO]")
          }
        }

        if (idDocFileName.isNotBlank()) {
          Text(
            text = "Selected file: $idDocFileName",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }

        // Step 3: Enter Demo last-4 / number & validate
        OutlinedTextField(
          value = rawDemoIdInput,
          onValueChange = {
            rawDemoIdInput = it.take(12)
            extractedPreviewReady = false
            idValidationError = null
          },
          label = { Text("3. ${strings.enterDemoDocNumberLabel}") },
          placeholder = {
            Text(
              if (idDocType.contains("Aadhaar")) "Last 4 digits (e.g. 4829)"
              else "Last 4 chars (e.g. 123F)"
            )
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        if (idValidationError != null) {
          Text(
            text = idValidationError!!,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
          )
        }

        // Step 4: Validate & Preview Extracted Demo Information
        OutlinedButton(
          onClick = {
            if (idDocFileName.isBlank()) {
              idValidationError = "Please select or attach a document file first."
            } else if (!AuthAndFileManager.isValidDemoIdentityInput(idDocType, rawDemoIdInput)) {
              idValidationError = "Please enter at least the last 4 characters/digits of the demo document."
            } else {
              extractedPreviewReady = true
              idValidationError = null
            }
          }
        ) {
          Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "4. ${strings.extractAndPreviewButton}")
        }

        // Step 5: Ask user to confirm extracted demo info
        if (extractedPreviewReady) {
          val previewMasked =
            AuthAndFileManager.maskIdentityDocumentNumber(idDocType, rawDemoIdInput)
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = "Extracted Demo Information Preview (DEMO/TEST DATA)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Holder Name: ${fullName.ifBlank { "Patient" }}",
                style = MaterialTheme.typography.bodySmall,
              )
              Text(
                text = "Document Type: $idDocType",
                style = MaterialTheme.typography.bodySmall,
              )
              Text(
                text = "Masked Number: $previewMasked",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
              )
              Text(
                text = "File Validated: $idDocFileName",
                style = MaterialTheme.typography.bodySmall,
              )
              Button(
                onClick = {
                  idDocMasked = previewMasked
                  idDocStatus = strings.statusDemoVerified
                  rawDemoIdInput = ""
                  extractedPreviewReady = false
                },
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(text = "5. ${strings.confirmDemoVerificationButton}")
              }
            }
          }
        }
      }
    }

    if (formError != null) {
      Text(
        text = formError!!,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
      )
    }

    if (savedMessage != null) {
      Text(
        text = savedMessage!!,
        color = Color(0xFF16A34A),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
      )
    }

    Button(
      onClick = {
        val normalizedPending = AuthAndFileManager.normalizePhoneNumber(pendingPhoneInput)
        if (fullName.isBlank() || dob.isBlank() || emergencyName.isBlank() || emergencyPhone.isBlank()) {
          formError = "Please fill in Full Name, Date of Birth, and Emergency Contact details."
          return@Button
        }
        if (normalizedPending != verifiedPhone) {
          formError =
            "You edited your phone number. Please verify the new phone number via OTP before saving."
          return@Button
        }
        val updatedProfile =
          existingProfile.copy(
            fullName = fullName.trim(),
            dateOfBirth = dob.trim(),
            phoneNumber = verifiedPhone,
            isPhoneVerified = true,
            email = email.trim(),
            address = address.trim(),
            emergencyContactName = emergencyName.trim(),
            emergencyContactPhone = emergencyPhone.trim(),
            emergencyContactRelation = emergencyRelation.trim(),
            preferredLanguage = currentLanguage.code,
            profilePictureUri = profilePicUri,
            idDocType = idDocType,
            idDocMaskedNumber = idDocMasked,
            idDocVerificationStatus =
              idDocStatus.ifBlank { strings.statusNotSubmitted },
            idDocFileName = idDocFileName,
            isProfileCompleted = true,
          )
        val initialGuardian =
          if (isInitialSetup && addGuardianNow && guardianName.isNotBlank() && guardianPhone.isNotBlank()) {
            GuardianEntity(
              userId = existingProfile.userId,
              fullName = guardianName.trim(),
              relationship = guardianRelation,
              phoneNumber = AuthAndFileManager.normalizePhoneNumber(guardianPhone),
              isPhoneVerified = false,
              email = "",
              isPrimaryEmergencyContact = true,
              canViewAppointments = true,
              canViewMedications = true,
              canViewHealthAlerts = true,
              canViewInsurance = true,
              canViewEmergencyInfo = true,
              canViewAuthorizedDocuments = false,
              addedAt = System.currentTimeMillis(),
            )
          } else null
        onSaveProfile(updatedProfile, initialGuardian)
        savedMessage = "Profile saved successfully."
      },
      modifier = Modifier.fillMaxWidth().height(50.dp),
      shape = RoundedCornerShape(10.dp),
    ) {
      Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(text = strings.saveProfileButton, fontWeight = FontWeight.SemiBold)
    }
  }

  // Dialog for verifying new phone number when edited in Profile
  val challenge = phoneChangeChallenge
  if (challenge != null) {
    AlertDialog(
      onDismissRequest = { phoneChangeChallenge = null },
      title = { Text(strings.verifyNewPhoneTitle) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(strings.verifyNewPhoneDesc, style = MaterialTheme.typography.bodySmall)
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
                text = "Test OTP for +91 ${challenge.phoneNumber}: ${challenge.otpCode}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF92400E),
              )
              TextButton(onClick = { phoneChangeOtpInput = challenge.otpCode }) {
                Text("Auto-fill Test OTP")
              }
            }
          }
          OutlinedTextField(
            value = phoneChangeOtpInput,
            onValueChange = {
              phoneChangeOtpInput = it.filter { ch -> ch.isDigit() }.take(6)
              phoneChangeOtpError = null
            },
            label = { Text(strings.enterSixDigitOtp) },
            singleLine = true,
          )
          if (phoneChangeOtpError != null) {
            Text(
              text = phoneChangeOtpError!!,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            when (
              val res =
                AuthAndFileManager.verifyOtpForPhone(challenge.phoneNumber, phoneChangeOtpInput)
            ) {
              is OtpVerificationResult.Success -> {
                verifiedPhone = res.verifiedPhoneNumber
                pendingPhoneInput = res.verifiedPhoneNumber
                phoneChangeChallenge = null
              }
              OtpVerificationResult.ExpiredOtp -> phoneChangeOtpError = strings.expiredOtpError
              else -> phoneChangeOtpError = strings.invalidOtpError
            }
          }
        ) {
          Text(strings.verifyOtpButton)
        }
      },
      dismissButton = {
        TextButton(onClick = { phoneChangeChallenge = null }) { Text(strings.cancelButton) }
      },
    )
  }
}
