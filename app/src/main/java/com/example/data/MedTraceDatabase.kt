package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
  @PrimaryKey val userId: String, // Normalized phone number
  val fullName: String,
  val dateOfBirth: String,
  val phoneNumber: String,
  val isPhoneVerified: Boolean,
  val email: String,
  val address: String,
  val emergencyContactName: String,
  val emergencyContactPhone: String,
  val emergencyContactRelation: String,
  val preferredLanguage: String,
  val profilePictureUri: String,
  val idDocType: String, // "", "Demo Aadhaar", "Demo PAN"
  val idDocMaskedNumber: String, // e.g., "XXXX XXXX 1234"
  val idDocVerificationStatus: String, // "Not Submitted", "Demo Verification (DEMO/TEST DATA)"
  val idDocFileName: String,
  val isProfileCompleted: Boolean,
  val createdAt: Long,
  val lastLoginAt: Long,
)

@Entity(tableName = "guardians")
data class GuardianEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val userId: String,
  val fullName: String,
  val relationship: String, // Parent, Mother, Father, Legal guardian, Other authorized caregiver
  val phoneNumber: String,
  val isPhoneVerified: Boolean,
  val email: String,
  val isPrimaryEmergencyContact: Boolean,
  val canViewAppointments: Boolean,
  val canViewMedications: Boolean,
  val canViewHealthAlerts: Boolean,
  val canViewInsurance: Boolean,
  val canViewEmergencyInfo: Boolean,
  val canViewAuthorizedDocuments: Boolean,
  val addedAt: Long,
)

@Entity(tableName = "medicines")
data class MedicineEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val userId: String,
  val medicineName: String,
  val dosage: String,
  val frequency: String,
  val startDate: String,
  val endDate: String,
  val prescribedBy: String,
  val instructions: String,
  val reminderSchedule: String,
  val status: String, // Active, Completed, Upcoming, Missed
  val visibleToGuardian: Boolean,
  val createdAt: Long,
)

@Entity(tableName = "insurance_policies")
data class InsurancePolicyEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val userId: String,
  val providerName: String,
  val policyNumber: String,
  val policyType: String,
  val coverageAmount: String,
  val startDate: String,
  val expiryDate: String,
  val isActive: Boolean,
  val coveredServices: String,
  val claimInfo: String,
  val updatedAt: Long,
)

@Entity(tableName = "health_snapshots")
data class HealthSnapshotEntity(
  @PrimaryKey val userId: String,
  val bloodGroup: String,
  val allergies: String,
  val currentMedicationsSummary: String,
  val recentDiagnosis: String,
  val recentLabResults: String,
  val vaccinationStatus: String,
  val medicalConditions: String,
  val bloodPressure: String,
  val heartRate: String,
  val spo2: String,
  val temperature: String,
  val weightKg: String,
  val bloodGlucose: String,
  val updatedAt: Long,
)

@Entity(tableName = "health_records")
data class HealthRecordEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val userId: String,
  val fileName: String,
  val localFilePath: String,
  val mimeType: String,
  val fileSizeBytes: Long,
  val recordType: String, // Lab report, Prescription, Scan, Discharge summary, Medical certificate, Insurance document, Other
  val uploadedBy: String,
  val uploadDate: Long,
  val appointmentId: Long?,
  val appointmentLabel: String,
  val sharedWithGuardian: Boolean,
  val notes: String,
)

@Entity(tableName = "receipts")
data class ReceiptEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val userId: String,
  val receiptNumber: String,
  val hospitalOrClinic: String,
  val doctorName: String,
  val appointmentId: Long?,
  val receiptDate: String,
  val paymentMethod: String,
  val paymentStatus: String,
  val totalAmountInr: Double,
  val itemizedChargesRaw: String, // Format: "ItemName::Amount||ItemName2::Amount2"
  val notes: String,
  val createdAt: Long,
)

@Entity(tableName = "appointments")
data class AppointmentEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val userId: String,
  val patientName: String,
  val doctorName: String,
  val hospitalOrClinic: String,
  val department: String,
  val appointmentDate: String,
  val appointmentTime: String,
  val reasonForVisit: String,
  val status: String, // Upcoming, Completed, Cancelled
  val cancelledAt: String,
  val diagnosis: String,
  val prescriptions: String,
  val testsOrdered: String,
  val followUpInstructions: String,
  val billingInfo: String,
  val createdAt: Long,
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val userId: String,
  val title: String,
  val category: String,
  val timestamp: Long,
)

@Dao
interface MedTraceDao {
  @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
  fun observeUserProfile(userId: String): Flow<UserProfileEntity?>

  @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
  suspend fun getUserProfile(userId: String): UserProfileEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertUserProfile(profile: UserProfileEntity)

  // Guardians
  @Query("SELECT * FROM guardians WHERE userId = :userId ORDER BY isPrimaryEmergencyContact DESC, addedAt DESC")
  fun observeGuardians(userId: String): Flow<List<GuardianEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertGuardian(guardian: GuardianEntity): Long

  @Delete
  suspend fun deleteGuardian(guardian: GuardianEntity)

  @Query("UPDATE guardians SET isPrimaryEmergencyContact = 0 WHERE userId = :userId")
  suspend fun clearPrimaryEmergencyGuardians(userId: String)

  // Medicines
  @Query("SELECT * FROM medicines WHERE userId = :userId ORDER BY createdAt DESC")
  fun observeMedicines(userId: String): Flow<List<MedicineEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMedicine(medicine: MedicineEntity): Long

  @Delete
  suspend fun deleteMedicine(medicine: MedicineEntity)

  // Insurance
  @Query("SELECT * FROM insurance_policies WHERE userId = :userId ORDER BY isActive DESC, updatedAt DESC")
  fun observeInsurancePolicies(userId: String): Flow<List<InsurancePolicyEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertInsurancePolicy(policy: InsurancePolicyEntity): Long

  @Delete
  suspend fun deleteInsurancePolicy(policy: InsurancePolicyEntity)

  // Health Snapshot
  @Query("SELECT * FROM health_snapshots WHERE userId = :userId LIMIT 1")
  fun observeHealthSnapshot(userId: String): Flow<HealthSnapshotEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertHealthSnapshot(snapshot: HealthSnapshotEntity)

  // Health Records
  @Query("SELECT * FROM health_records WHERE userId = :userId ORDER BY uploadDate DESC")
  fun observeHealthRecords(userId: String): Flow<List<HealthRecordEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHealthRecord(record: HealthRecordEntity): Long

  @Delete
  suspend fun deleteHealthRecord(record: HealthRecordEntity)

  // Receipts
  @Query("SELECT * FROM receipts WHERE userId = :userId ORDER BY createdAt DESC")
  fun observeReceipts(userId: String): Flow<List<ReceiptEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertReceipt(receipt: ReceiptEntity): Long

  @Delete
  suspend fun deleteReceipt(receipt: ReceiptEntity)

  // Appointments
  @Query("SELECT * FROM appointments WHERE userId = :userId ORDER BY appointmentDate ASC, appointmentTime ASC")
  fun observeAppointments(userId: String): Flow<List<AppointmentEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAppointment(appointment: AppointmentEntity): Long

  @Update
  suspend fun updateAppointment(appointment: AppointmentEntity)

  // Activity Logs
  @Query("SELECT * FROM activity_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT 25")
  fun observeActivityLogs(userId: String): Flow<List<ActivityLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertActivityLog(log: ActivityLogEntity)
}

@Database(
  entities = [
    UserProfileEntity::class,
    GuardianEntity::class,
    MedicineEntity::class,
    InsurancePolicyEntity::class,
    HealthSnapshotEntity::class,
    HealthRecordEntity::class,
    ReceiptEntity::class,
    AppointmentEntity::class,
    ActivityLogEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class MedTraceDatabase : RoomDatabase() {
  abstract fun dao(): MedTraceDao

  companion object {
    @Volatile private var INSTANCE: MedTraceDatabase? = null

    fun getInstance(context: Context): MedTraceDatabase {
      return INSTANCE
        ?: synchronized(this) {
          val instance =
            Room.databaseBuilder(
                context.applicationContext,
                MedTraceDatabase::class.java,
                "medtrace_clinical.db",
              )
              .fallbackToDestructiveMigration(dropAllTables = true)
              .build()
          INSTANCE = instance
          instance
        }
    }
  }
}
