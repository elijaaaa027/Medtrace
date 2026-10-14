package com.example.security

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.i18n.AppLanguage
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom

data class OtpSessionChallenge(
  val phoneNumber: String,
  val otpCode: String,
  val issuedAtMillis: Long,
  val expiresAtMillis: Long,
  val resendAvailableAtMillis: Long,
)

sealed class OtpVerificationResult {
  data class Success(val verifiedPhoneNumber: String) : OtpVerificationResult()
  data object InvalidPhone : OtpVerificationResult()
  data object ExpiredOtp : OtpVerificationResult()
  data object InvalidCode : OtpVerificationResult()
}

data class PickedFileMetadata(
  val displayName: String,
  val mimeType: String,
  val sizeBytes: Long,
  val extension: String,
  val isSupported: Boolean,
)

object AuthAndFileManager {
  private const val PREFS_NAME = "medtrace_secure_prefs"
  private const val KEY_ACTIVE_USER_ID = "active_user_id"
  private const val KEY_REMEMBER_SESSION = "remember_session"
  private const val KEY_PREFERRED_LANGUAGE = "preferred_language"
  private const val KEY_DARK_MODE_ENABLED = "dark_mode_enabled"
  private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
  private const val KEY_BIOMETRIC_BOUND_PHONE = "biometric_bound_phone"
  private const val KEY_SECURITY_PIN = "security_pin_code"

  private const val OTP_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes
  private const val OTP_RESEND_COOLDOWN_MS = 30 * 1000L // 30 seconds

  private val secureRandom = SecureRandom()

  // Active OTP challenges keyed by normalized phone number so OTP is strictly bound to the phone number
  private val activeChallenges = mutableMapOf<String, OtpSessionChallenge>()

  fun normalizePhoneNumber(rawPhone: String): String {
    val digitsOnly = rawPhone.filter { it.isDigit() }
    return if (digitsOnly.length == 12 && digitsOnly.startsWith("91")) {
      digitsOnly.substring(2)
    } else {
      digitsOnly
    }
  }

  fun isValidPhoneNumber(rawPhone: String): Boolean {
    val normalized = normalizePhoneNumber(rawPhone)
    return normalized.length == 10 && normalized.first() in '6'..'9'
  }

  fun issueOtpForPhone(rawPhone: String): OtpSessionChallenge? {
    if (!isValidPhoneNumber(rawPhone)) return null
    val normalized = normalizePhoneNumber(rawPhone)
    val now = System.currentTimeMillis()
    val codeInt = 100000 + secureRandom.nextInt(900000)
    val challenge =
      OtpSessionChallenge(
        phoneNumber = normalized,
        otpCode = codeInt.toString(),
        issuedAtMillis = now,
        expiresAtMillis = now + OTP_VALIDITY_MS,
        resendAvailableAtMillis = now + OTP_RESEND_COOLDOWN_MS,
      )
    activeChallenges[normalized] = challenge
    return challenge
  }

  fun verifyOtpForPhone(rawPhone: String, enteredOtp: String): OtpVerificationResult {
    if (!isValidPhoneNumber(rawPhone)) return OtpVerificationResult.InvalidPhone
    val normalized = normalizePhoneNumber(rawPhone)
    val challenge = activeChallenges[normalized] ?: return OtpVerificationResult.InvalidCode
    val now = System.currentTimeMillis()
    if (now > challenge.expiresAtMillis) {
      return OtpVerificationResult.ExpiredOtp
    }
    if (challenge.otpCode != enteredOtp.trim()) {
      return OtpVerificationResult.InvalidCode
    }
    activeChallenges.remove(normalized)
    return OtpVerificationResult.Success(normalized)
  }

  fun saveAuthenticatedSession(context: Context, userId: String, rememberSession: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs
      .edit()
      .putString(KEY_ACTIVE_USER_ID, userId)
      .putBoolean(KEY_REMEMBER_SESSION, rememberSession)
      .apply()
  }

  fun getSavedAuthenticatedUserId(context: Context): String? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val remember = prefs.getBoolean(KEY_REMEMBER_SESSION, true)
    if (!remember) return null
    return prefs.getString(KEY_ACTIVE_USER_ID, null)?.takeIf { it.isNotBlank() }
  }

  fun clearAuthenticatedSession(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().remove(KEY_ACTIVE_USER_ID).putBoolean(KEY_REMEMBER_SESSION, false).apply()
  }

  fun saveLanguagePreference(context: Context, language: AppLanguage) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_PREFERRED_LANGUAGE, language.code).apply()
  }

  fun getSavedLanguagePreference(context: Context): AppLanguage {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return AppLanguage.fromCode(prefs.getString(KEY_PREFERRED_LANGUAGE, AppLanguage.EN.code))
  }

  fun saveDarkModePreference(context: Context, isDark: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_DARK_MODE_ENABLED, isDark).apply()
  }

  fun getSavedDarkModePreference(context: Context, defaultDark: Boolean = false): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return if (prefs.contains(KEY_DARK_MODE_ENABLED)) {
      prefs.getBoolean(KEY_DARK_MODE_ENABLED, defaultDark)
    } else {
      defaultDark
    }
  }

  fun isBiometricEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
  }

  fun setBiometricEnabled(context: Context, enabled: Boolean, boundPhone: String? = null) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val editor = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
    if (!boundPhone.isNullOrBlank()) {
      editor.putString(KEY_BIOMETRIC_BOUND_PHONE, normalizePhoneNumber(boundPhone))
    }
    editor.apply()
  }

  fun getBiometricBoundPhone(context: Context): String? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_BIOMETRIC_BOUND_PHONE, null)?.takeIf { it.isNotBlank() }
  }

  fun saveSecurityPin(context: Context, pin: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_SECURITY_PIN, pin.filter { it.isDigit() }.take(6)).apply()
  }

  fun getSecurityPin(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_SECURITY_PIN, "") ?: ""
  }

  fun getBiometricHardwareStatusLabel(context: Context): String {
    val pm = context.packageManager
    val hasFingerprint = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT)
    val hasFace =
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FACE)
      } else false
    return when {
      hasFingerprint && hasFace -> "Fingerprint & Face Unlock Hardware Detected"
      hasFingerprint -> "Fingerprint Sensor Detected (Face Unlock Supported)"
      hasFace -> "Face Unlock Sensor Detected"
      else -> "Biometric Sensor Ready (Fingerprint / Face Unlock)"
    }
  }

  fun launchSystemBiometricPromptIfEnrolled(
    context: Context,
    title: String,
    subtitle: String,
    negativeButtonText: String,
    onAuthenticated: () -> Unit,
    onRequireInteractiveSheet: (String?) -> Unit,
  ) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
      try {
        val biometricManager =
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
          } else null

        val canAuth =
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && biometricManager != null) {
            biometricManager.canAuthenticate() ==
              android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS
          } else false

        if (!canAuth) {
          onRequireInteractiveSheet(null)
          return
        }

        val cancelSignal = android.os.CancellationSignal()
        val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
        val prompt =
          android.hardware.biometrics.BiometricPrompt.Builder(context)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton(
              negativeButtonText,
              executor,
              { _, _ -> onRequireInteractiveSheet("Switched to interactive verification") },
            )
            .build()

        prompt.authenticate(
          cancelSignal,
          executor,
          object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
              result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?
            ) {
              onAuthenticated()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
              onRequireInteractiveSheet(errString?.toString())
            }

            override fun onAuthenticationFailed() {
              // Allow user to retry in system prompt or fallback
            }
          },
        )
      } catch (e: Exception) {
        onRequireInteractiveSheet(e.localizedMessage)
      }
    } else {
      onRequireInteractiveSheet(null)
    }
  }

  /**
   * Masks sensitive Aadhaar or PAN document numbers before storing or displaying in UI.
   * Example Aadhaar: XXXX XXXX 1234
   * Example PAN: XXXXXX123F
   */
  fun maskIdentityDocumentNumber(docType: String, rawInput: String): String {
    val cleaned = rawInput.filter { it.isLetterOrDigit() }.uppercase()
    return if (docType.contains("Aadhaar", ignoreCase = true)) {
      val last4 = cleaned.filter { it.isDigit() }.takeLast(4).padStart(4, '0')
      "XXXX XXXX $last4"
    } else {
      val last4 = cleaned.takeLast(4).padStart(4, 'X')
      "XXXXXX$last4"
    }
  }

  fun isValidDemoIdentityInput(docType: String, rawInput: String): Boolean {
    val cleaned = rawInput.filter { it.isLetterOrDigit() }
    return if (docType.contains("Aadhaar", ignoreCase = true)) {
      val digits = cleaned.filter { it.isDigit() }
      digits.length in 4..12
    } else {
      cleaned.length in 4..10
    }
  }

  fun inspectPickedFile(context: Context, uri: Uri): PickedFileMetadata {
    var displayName = "document_${System.currentTimeMillis()}"
    var sizeBytes = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
      if (cursor.moveToFirst()) {
        if (nameIndex >= 0) {
          displayName = cursor.getString(nameIndex) ?: displayName
        }
        if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
          sizeBytes = cursor.getLong(sizeIndex)
        }
      }
    }
    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
    val extension = displayName.substringAfterLast('.', "").lowercase()
    val allowedExtensions = setOf("pdf", "jpg", "jpeg", "png")
    val allowedMimes = setOf("application/pdf", "image/jpeg", "image/png", "image/jpg")
    val isSupported = extension in allowedExtensions || mimeType.lowercase() in allowedMimes
    return PickedFileMetadata(
      displayName = displayName,
      mimeType = mimeType,
      sizeBytes = sizeBytes,
      extension = extension.ifBlank { if (mimeType.contains("pdf")) "pdf" else "jpg" },
      isSupported = isSupported,
    )
  }

  fun copyUriToPrivateUserStorage(
    context: Context,
    userId: String,
    uri: Uri,
    fileName: String,
  ): File? {
    return try {
      val userDir = File(context.filesDir, "records_$userId")
      if (!userDir.exists()) userDir.mkdirs()
      val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
      val destFile = File(userDir, "${System.currentTimeMillis()}_$safeName")
      context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(destFile).use { output -> input.copyTo(output) }
      }
      if (destFile.exists() && destFile.length() > 0L) destFile else null
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Creates a real PDF file inside the user's private storage when the user generates a sample
   * test document on an emulator without pre-populated files in the Android Documents folder.
   */
  fun createRealPdfDocumentInPrivateStorage(
    context: Context,
    userId: String,
    fileName: String,
    title: String,
    lines: List<String>,
  ): File {
    val userDir = File(context.filesDir, "records_$userId")
    if (!userDir.exists()) userDir.mkdirs()
    val safeName =
      if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
    val destFile = File(userDir, "${System.currentTimeMillis()}_$safeName")

    val pdfDocument = android.graphics.pdf.PdfDocument()
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = android.graphics.Paint().apply {
      isAntiAlias = true
      color = android.graphics.Color.rgb(15, 118, 110)
      textSize = 18f
      isFakeBoldText = true
    }
    canvas.drawText("MedTrace Clinical Document Record", 40f, 50f, paint)
    paint.color = android.graphics.Color.rgb(15, 23, 42)
    paint.textSize = 15f
    canvas.drawText(title, 40f, 82f, paint)

    paint.isFakeBoldText = false
    paint.textSize = 12f
    var y = 120f
    lines.forEach { line ->
      canvas.drawText(line, 40f, y, paint)
      y += 24f
    }
    pdfDocument.finishPage(page)
    FileOutputStream(destFile).use { out -> pdfDocument.writeTo(out) }
    pdfDocument.close()
    return destFile
  }
}
