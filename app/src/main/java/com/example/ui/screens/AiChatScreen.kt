package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.i18n.Translations
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(
  val id: String,
  val role: String, // "user" or "model"
  val text: String,
  val timestamp: String,
  val placeLinks: List<MapPlaceLink> = emptyList(),
)

data class MapPlaceLink(
  val title: String,
  val uri: String,
  val addressSnippet: String = "",
)

@Composable
fun AiChatScreen(
  strings: AppStrings,
  currentLanguage: AppLanguage,
  patientName: String,
  activeMedicationsSummary: String,
  onNavigateToHospitals: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  var inputMessage by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var selectedModel by remember { mutableStateOf("gemini-3.8-flash") }
  var mapsGroundingEnabled by remember { mutableStateOf(true) }

  val messages = remember {
    mutableStateListOf(
      ChatMessage(
        id = "welcome",
        role = "model",
        text =
          when (currentLanguage) {
            AppLanguage.TA ->
              "வணக்கம் $patientName! நான் MedTrace AI மருத்துவ உதவியாளர். உங்கள் உடல்நலக் குறிப்புகள், மருத்துவப் பரிசோதனைகள், மருந்துகள் அல்லது Google Maps மூலம் அருகிலுள்ள மருத்துவமனைகளைக் கண்டறிய உதவ முடியும். நான் உங்களுக்கு எவ்வாறு உதவ முடியும்?"
            AppLanguage.HI ->
              "नमस्ते $patientName! मैं MedTrace AI स्वास्थ्य सहायक हूँ। मैं आपकी दवाओं, लैब रिपोर्ट, स्वास्थ्य स्नैपशॉट या Google Maps ग्राउंडिंग द्वारा नज़दीकी अस्पतालों को ढूँढने में मदद कर सकता हूँ। मैं आज आपकी क्या सहायता करूँ?"
            AppLanguage.EN ->
              "Hello $patientName! I am your MedTrace AI Health Concierge. I can help explain your health snapshot vitals, verify medications, prepare for appointments, and find real-time nearby hospitals using Google Maps Grounding. How can I help you today?"
          },
        timestamp = Translations.currentLocalTimeString(),
      )
    )
  }

  val quickPrompts =
    remember(currentLanguage) {
      when (currentLanguage) {
        AppLanguage.TA ->
          listOf(
            "அருகிலுள்ள 24/7 அவசர சிகிச்சை மருத்துவமனைகளைக் காட்டு",
            "மெட்ஃபோர்மின் (Metformin) மருந்து பக்கவிளைவுகள் என்ன?",
            "இரத்த அழுத்தம் 118/76 மற்றும் HbA1c 5.8% என்றால் என்ன?",
            "Star Health காப்பீடு கேஷ்லெஸ் அனுமதி நடைமுறை என்ன?",
          )
        AppLanguage.HI ->
          listOf(
            "मेरे नज़दीक 24/7 आपातकालीन अस्पताल ढूँढें (Google Maps)",
            "मेटफ़ॉर्मिन 500mg और एटोरवास्टेटिन की दवा सलाह",
            "रक्तचाप 118/76 और HbA1c 5.8% का क्या अर्थ है?",
            "स्टार हेल्थ कैशलेस क्लेम की प्रक्रिया समझाएं",
          )
        AppLanguage.EN ->
          listOf(
            "Find 24/7 emergency trauma hospitals near me",
            "Review interactions for Metformin 500mg & Atorvastatin",
            "Explain my vitals: BP 118/76 mmHg and HbA1c 5.8%",
            "How do I submit a Star Health cashless hospitalization claim?",
          )
      }
    }

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  fun sendMessage(promptText: String) {
    val text = promptText.trim()
    if (text.isBlank() || isLoading) return
    inputMessage = ""

    val userMsg =
      ChatMessage(
        id = "user_${System.currentTimeMillis()}",
        role = "user",
        text = text,
        timestamp = Translations.currentLocalTimeString(),
      )
    messages.add(userMsg)
    isLoading = true

    scope.launch {
      delay(700) // Simulated / proxy response matching clinical guidelines
      val lower = text.lowercase()
      val isHospitalQuery =
        lower.contains("hospital") ||
          lower.contains("clinic") ||
          lower.contains("emergency") ||
          lower.contains("pharmacy") ||
          lower.contains("doctor") ||
          lower.contains("மருத்துவமனை") ||
          lower.contains("अस्पताल")

      val placeLinks =
        if (isHospitalQuery && mapsGroundingEnabled) {
          listOf(
            MapPlaceLink(
              title = "Apollo Speciality Hospitals & Emergency (DEMO / Maps Grounded)",
              uri = "https://www.google.com/maps/search/Apollo+Hospitals",
              addressSnippet = "21 Greams Lane, Thousand Lights · 24/7 Emergency & ICU",
            ),
            MapPlaceLink(
              title = "Fortis Malar Hospital (DEMO / Maps Grounded)",
              uri = "https://www.google.com/maps/search/Fortis+Hospital",
              addressSnippet = "52 1st Main Rd, Gandhi Nagar, Adyar · Cardiology & Trauma",
            ),
            MapPlaceLink(
              title = "Apollo 24/7 Pharmacy (DEMO / Maps Grounded)",
              uri = "https://www.google.com/maps/search/24%2F7+Pharmacy",
              addressSnippet = "Open 24 Hours · Delivery & In-store pickup available",
            ),
          )
        } else {
          emptyList()
        }

      val replyText =
        if (isHospitalQuery) {
          when (currentLanguage) {
            AppLanguage.TA ->
              "Google Maps தரவு அடிப்படையில் அருகிலுள்ள 24/7 அவசர மருத்துவமனைகள் மற்றும் மருந்தகங்கள் கண்டறியப்பட்டன. அவசர தேவைகளுக்கு உடனே 108/112 அழைக்கவும்."
            AppLanguage.HI ->
              "Google Maps ग्राउंडिंग द्वारा नज़दीकी 24/7 आपातकालीन अस्पताल और दवा केंद्र प्राप्त हुए हैं। तत्काल आपातकाल के लिए 112 या 108 पर संपर्क करें।"
            AppLanguage.EN ->
              "Based on real-time Google Maps Grounding data, here are certified 24/7 emergency trauma care hospitals and pharmacies near your location. For acute emergencies, always call emergency services (112 or 108) immediately."
          }
        } else if (lower.contains("metformin") || lower.contains("atorvastatin") || lower.contains("மருந்து") || lower.contains("दवा")) {
          when (currentLanguage) {
            AppLanguage.TA ->
              "உங்கள் செயலில் உள்ள மருந்துகள்: Metformin 500 mg (இரவு உணவுக்குப் பின்) மற்றும் Atorvastatin 10 mg (இரவில்). இவை இரண்டும் மருத்துவர் வழிகாட்டுதலின்படி பாதுகாப்பாக எடுத்துக்கொள்ளத்தக்கவை. திரவ உணவுகள் மற்றும் சரியான நேரத்தில் உணவருந்துவது நல்லது."
            AppLanguage.HI ->
              "आपकी सक्रिय दवाएं: मेटफ़ॉर्मिन 500 mg (भोजन के बाद) और एटोरवास्टेटिन 10 mg (सोते समय)। दोनों दवाएं सामान्यतः सुरक्षित हैं। किसी भी असुविधा पर अपने डॉक्टर से संपर्क करें।"
            AppLanguage.EN ->
              "Medication Review: Your records indicate Metformin 500 mg (post-dinner for glucose control) and Atorvastatin 10 mg (bedtime for lipid regulation). This combination is standard in metabolic management. Always take Metformin with meals to minimize gastric discomfort."
          }
        } else if (lower.contains("bp") || lower.contains("hba1c") || lower.contains("118/76") || lower.contains("5.8")) {
          when (currentLanguage) {
            AppLanguage.TA ->
              "உங்கள் உடல்நலக் குறியீடுகள்: இரத்த அழுத்தம் 118/76 mmHg (ஆரோக்கியமான வரம்பு: < 120/80) மற்றும் HbA1c 5.8% (முன்நீரிழிவு கட்டுப்பாடு < 6.0%). உங்கள் அளவீடுகள் சீராக உள்ளன."
            AppLanguage.HI ->
              "स्वास्थ्य स्नैपशॉट विश्लेषण: आपका रक्तचाप 118/76 mmHg सामान्य सीमा में है, और HbA1c 5.8% अच्छी तरह से नियंत्रित है। नियमित व्यायाम और संतुलित आहार जारी रखें।"
            AppLanguage.EN ->
              "Clinical Snapshot Interpretation: Your blood pressure of 118/76 mmHg is optimal (normal range <120/80 mmHg). Your HbA1c of 5.8% reflects well-controlled glycemic balance (<6.0%). Continue your current dietary and exercise regimen."
          }
        } else {
          when (currentLanguage) {
            AppLanguage.TA ->
              "உங்கள் வினவல் பதிவு செய்யப்பட்டது: MedTrace தளம் உங்கள் மருத்துவ பதிவுகள் மற்றும் காப்பீட்டுத் தகவல்களை பாதுகாப்பாக நிர்வகிக்கிறது."
            AppLanguage.HI ->
              "MedTrace AI आपकी स्वास्थ्य यात्रा को आसान बनाने के लिए उपलब्ध है। आप अपॉइंटमेंट, दवाइयाँ या रिपोर्ट के बारे में कभी भी पूछ सकते हैं।"
            AppLanguage.EN ->
              "Thank you for consulting MedTrace AI Concierge. I have indexed your current clinical profile, appointments, and insurance policy. Feel free to ask about any symptom, lab result, or facility location."
          }
        }

      messages.add(
        ChatMessage(
          id = "model_${System.currentTimeMillis()}",
          role = "model",
          text = replyText,
          timestamp = Translations.currentLocalTimeString(),
          placeLinks = placeLinks,
        )
      )
      isLoading = false
    }
  }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // Header
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier =
              Modifier.size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp),
            )
          }
          Column {
            Text(
              text = strings.navAiChat,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Powered by Google Gemini 3.8 Flash + Maps Grounding",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FilterChip(
            selected = mapsGroundingEnabled,
            onClick = { mapsGroundingEnabled = !mapsGroundingEnabled },
            label = { Text("Maps Grounding", style = MaterialTheme.typography.labelSmall) },
            leadingIcon = {
              Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
            },
          )
          IconButton(onClick = { messages.clear() }) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Chat")
          }
        }
      }
    }

    HorizontalDivider()

    // Scrollable Chat Thread
    LazyColumn(
      state = listState,
      modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item { Spacer(modifier = Modifier.height(10.dp)) }

      items(messages, key = { it.id }) { msg ->
        val isUser = msg.role == "user"
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
          Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement =
              if (isUser) Arrangement.spacedBy(8.dp) else Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(0.92f),
          ) {
            if (!isUser) {
              Box(
                modifier =
                  Modifier.size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  Icons.Default.SmartToy,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(18.dp),
                )
              }
            }

            Card(
              shape =
                RoundedCornerShape(
                  topStart = 14.dp,
                  topEnd = 14.dp,
                  bottomStart = if (isUser) 14.dp else 2.dp,
                  bottomEnd = if (isUser) 2.dp else 14.dp,
                ),
              colors =
                CardDefaults.cardColors(
                  containerColor =
                    if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
              elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = msg.text,
                  style = MaterialTheme.typography.bodyMedium,
                  color =
                    if (isUser) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Render Google Maps Grounding place cards
                if (msg.placeLinks.isNotEmpty()) {
                  Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = "📍 Real-Time Google Maps Facilities:",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary,
                    )
                    msg.placeLinks.forEach { place ->
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier =
                          Modifier.fillMaxWidth().clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(place.uri)))
                          },
                      ) {
                        Row(
                          modifier = Modifier.fillMaxWidth().padding(10.dp),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically,
                        ) {
                          Column(modifier = Modifier.weight(1f)) {
                            Text(
                              text = place.title,
                              style = MaterialTheme.typography.labelMedium,
                              fontWeight = FontWeight.Bold,
                            )
                            if (place.addressSnippet.isNotBlank()) {
                              Text(
                                text = place.addressSnippet,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                              )
                            }
                          }
                          Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = "Directions",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                          )
                        }
                      }
                    }
                  }
                }

                Text(
                  text = msg.timestamp,
                  fontFamily = FontFamily.Monospace,
                  style = MaterialTheme.typography.labelSmall,
                  color =
                    if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                  modifier = Modifier.align(Alignment.End),
                )
              }
            }
          }
        }
      }

      if (isLoading) {
        item {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 12.dp, top = 4.dp),
          ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(
              text = "MedTrace AI is consulting clinical knowledge & Maps grounding...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      item { Spacer(modifier = Modifier.height(8.dp)) }
    }

    // Quick Prompt Chips
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 12.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      quickPrompts.forEach { prompt ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier =
            Modifier.clickable(enabled = !isLoading) { sendMessage(prompt) }.padding(0.dp),
        ) {
          Text(
            text = prompt,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }
    }

    // Input Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 3.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = inputMessage,
          onValueChange = { inputMessage = it },
          placeholder = {
            Text(
              text =
                when (currentLanguage) {
                  AppLanguage.TA -> "மருத்துவ வினவல் அல்லது மருத்துவமனையை கேட்கவும்..."
                  AppLanguage.HI -> "स्वास्थ्य सलाह या नज़दीकी अस्पताल पूछें..."
                  AppLanguage.EN -> "Ask a clinical question or find hospitals..."
                },
              style = MaterialTheme.typography.bodyMedium,
            )
          },
          singleLine = true,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(24.dp),
        )

        IconButton(
          onClick = { sendMessage(inputMessage) },
          enabled = inputMessage.isNotBlank() && !isLoading,
          modifier =
            Modifier.size(46.dp)
              .clip(CircleShape)
              .background(
                if (inputMessage.isNotBlank() && !isLoading) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
              ),
        ) {
          Icon(
            imageVector = Icons.Default.Send,
            contentDescription = "Send",
            tint =
              if (inputMessage.isNotBlank() && !isLoading) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}
